package au.edu.unimelb.plantcell.io.ws.predgpi;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.cells.CoordinateSystem;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;
import au.edu.unimelb.plantcell.core.cells.TrackCreator;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcore.core.regions.PredGPIRegionsAnnotation;
import au.edu.unimelb.plantcore.core.regions.ScoredRegion;

public class PredGPINodeModel extends NodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger("PredGPI");
    
	public final static String CFGKEY_URL         = "predgpi-url";
	public static final String CFGKEY_SEQUENCE    = "sequence-column";
	public static final String CFGKEY_MODEL       = "model";
	
	public static final String DEFAULT_URL        = "http://gpcr.biocomp.unibo.it/cgi-bin/predictors/gpi/gpipe_1.4.cgi";
		
	private final SettingsModelString m_url       = new SettingsModelString(CFGKEY_URL, DEFAULT_URL);
	
	private final SettingsModelString m_seq_col   = new SettingsModelString(CFGKEY_SEQUENCE, "Sequence");
	private final SettingsModelString m_model     = new SettingsModelString(CFGKEY_MODEL,    "general" );
	
	// internal state
	private static StringCell m_highly_probable = new StringCell("highly probable");
	private static StringCell m_probable        = new StringCell("probable");
	private static StringCell m_unlikely        = new StringCell("unlikely");
	private static StringCell m_no              = new StringCell("no");
	
	public PredGPINodeModel() {
		this(1, 1);
	}
	
	protected PredGPINodeModel(int nrInDataPorts, int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
	}
	
	public DataTableSpec[] make_output_spec(DataTableSpec inSpec) {
		DataColumnSpec[] cols = new DataColumnSpec[4];

		DataColumnSpecCreator dcsc = new DataColumnSpecCreator("Sequence (incl. PredGPI annotation)", SequenceCell.TYPE);
		int index = inSpec.findColumnIndex(m_seq_col.getStringValue());
		if (index >= 0) {
	        DataColumnProperties isp = inSpec.getColumnSpec(index).getProperties();
			TrackColumnPropertiesCreator tcpc;
			try {
				tcpc = new TrackColumnPropertiesCreator(isp,
												new Track(Track.PREDGPI_TRACK, getTrackCreator())
											);
				dcsc.setProperties(tcpc.getProperties());
			} catch (InvalidSettingsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		cols[0] = dcsc.createSpec();
		cols[1] = new DataColumnSpecCreator("Omega Site", IntCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Specificity (FP_r)", DoubleCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("Prediction summary", StringCell.TYPE).createSpec();
		
		return new DataTableSpec[] { new DataTableSpec(cols) };
	}
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
		
		DataTableSpec[]        outputSpecs = make_output_spec(inData[0].getSpec());
    	final MyDataContainer c     = new MyDataContainer(exec.createDataContainer(outputSpecs[0]), "Prot");
        	
		final int n_rows     = inData[0].getRowCount();
		int seq_idx    = inData[0].getDataTableSpec().findColumnIndex(m_seq_col.getStringValue());
		if (seq_idx < 0)
			throw new InvalidSettingsException("Cannot find column: "+m_seq_col.getStringValue()+" - reconfigure the node?");
		
		// establish and configure instance for CGI invocations...
		Form f = new Form();
		f.setModel(m_model.getStringValue());
		
		Callback cb = new Callback() {
			
			@Override
			public void updateStatus() throws CanceledExecutionException {
				exec.checkCanceled();
			}
			
			@Override
			public void warn(String warning_msg) {
				logger.warn(warning_msg);
			}
			
			@Override
			public void addPrediction(SequenceValue sv, String omega, String fp, String key) {
				DataCell[] cells = new DataCell[4];
				int omega_site = Integer.valueOf(omega);
				cells[1] = (omega == null) ? DataType.getMissingCell() : new IntCell(omega_site);
				Double specificity = Double.valueOf(fp);
				cells[2] = (fp == null) ? DataType.getMissingCell() : new DoubleCell(specificity);
				cells[3] = prediction_summary(key);
				try {
            		SequenceCell sc = new SequenceCell(sv);
            		// NB: only add an annotation if there is an omega site prediction
            		if (!cells[3].equals(m_no)) {
            			Track t = sc.addTrack(Track.PREDGPI_TRACK, getTrackCreator());
            			PredGPIRegionsAnnotation ra = new PredGPIRegionsAnnotation();
            			ra.addRegion(new ScoredRegion(omega_site, "omega site", specificity.doubleValue()));
            			t.addAnnotation(ra);
            		}
					cells[0] = sc;
				} catch (InvalidSettingsException e) {
					e.printStackTrace();
					return;
				}
				c.addRow(cells);
			}

			// from the PredGPI publication as stated by the authors in the methods
			private DataCell prediction_summary(String prediction_class) {
				if (prediction_class.startsWith("g")) {
					return m_highly_probable;
				} else if (prediction_class.startsWith("y")) {
					return m_probable;
				} else if (prediction_class.startsWith("o")) {
					return m_unlikely;
				} else {
					return m_no;
				}
			}
		};
		
		// here we stay well within the website's limits to be friendly to other users
		BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(inData[0].iterator(), 
				seq_idx, 50, 50 * 1024, new SequenceProcessor() {

					@Override
					public SequenceValue process(SequenceValue sv) {
						if (!sv.getSequenceType().isProtein()) {
							logger.warn("PredGPI can only predict protein sequences! Skipping "+sv.getID());
							return null;
						}
						if (sv.getLength() < 40) {
							logger.warn("Skipping too short sequence (must be at least 40AA) " + sv.getID());
							return null;
						}
						return sv;
					}
			
		});
		
		int done = 0;
		while (bsi.hasNext()) {
			List<SequenceValue> batch = bsi.next();
			if (batch == null)
				break;
			
			f.process(batch, new URL(m_url.getStringValue()), cb);
			
			exec.setProgress(((double)done)/n_rows);
			exec.checkCanceled();
			if (bsi.hasNext()) {
				logger.info("Waiting 30s to be nice to PredGPI server");
				Thread.sleep(30 * 1000);
			}
			done += batch.size();
		}
		
		return new BufferedDataTable[] { c.close() };
	}
	
	private TrackCreator getTrackCreator() {
		return new TrackCreator() {

			@Override
			public Track createTrack(String name)
					throws InvalidSettingsException {
				return new Track(name, new PredGPIRegionsAnnotation(), CoordinateSystem.OFFSET_FROM_START);
			}
			
		};
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
	            throws InvalidSettingsException {
		return make_output_spec(inSpecs[0]);
	}
	
	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// NO-OP
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// NO-OP
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_url.saveSettingsTo(settings);
		m_model.saveSettingsTo(settings);
		m_seq_col.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_url.validateSettings(settings);
		m_model.validateSettings(settings);
		m_seq_col.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_url.loadSettingsFrom(settings);
		m_model.loadSettingsFrom(settings);
		m_seq_col.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
	}

}
