package au.edu.unimelb.plantcell.io.ws.predgpi;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
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
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;

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
	
	public PredGPINodeModel() {
		this(1, 1);
	}
	
	protected PredGPINodeModel(int nrInDataPorts, int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
	}
	
	public DataTableSpec[] make_output_spec(DataTableSpec inSpec) {
		DataColumnSpec[] cols = new DataColumnSpec[3];
		cols[0] = new DataColumnSpecCreator("Sequence", SequenceCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Omega Site", IntCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Specificity (FP_r)", DoubleCell.TYPE).createSpec();
		
		return new DataTableSpec[] { new DataTableSpec(cols) };
	}
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
		
		DataTableSpec[]        outputSpecs = make_output_spec(inData[0].getSpec());
    	final MyDataContainer c     = new MyDataContainer(exec.createDataContainer(outputSpecs[0]), "Prot");
        	
		int n_rows     = inData[0].getRowCount();
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
			public void addPrediction(SequenceValue sv, String omega, String fp_r) {
				DataCell[] cells = new DataCell[3];
				try {
					cells[0] = new SequenceCell(sv);
				} catch (InvalidSettingsException e) {
					e.printStackTrace();
					return;
				}
				cells[1] = new IntCell(Integer.valueOf(omega));
				cells[2] = new DoubleCell(Double.valueOf(fp_r));
				c.addRow(cells);
			}
		};
		
		int done = 0;
		BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(inData[0].iterator(), 
				seq_idx, 50, 100 * 1024, new SequenceProcessor() {

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
		}
		
		return new BufferedDataTable[] { c.close() };
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
