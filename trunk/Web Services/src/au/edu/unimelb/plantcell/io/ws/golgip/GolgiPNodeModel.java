package au.edu.unimelb.plantcell.io.ws.golgip;
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
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;

public class GolgiPNodeModel extends NodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger("GolgiP");
    
	public final static String CFGKEY_URL         = "golgip-url";
	public static final String CFGKEY_SEQUENCE    = "sequence-column";
	public static final String CFGKEY_ORGANISM    = "organism";
	public static final String CFGKEY_MODEL       = "golgip-model";
	
	private final SettingsModelString m_url       = new SettingsModelString(CFGKEY_URL, 
			"http://csbl1.bmb.uga.edu/GolgiP/GolgiP_run.php");
	private final SettingsModelString m_seq_col   = new SettingsModelString(CFGKEY_SEQUENCE, "Sequence");
	private final SettingsModelString m_organism  = new SettingsModelString(CFGKEY_ORGANISM, "plant");
	private final SettingsModelString m_model     = new SettingsModelString(CFGKEY_MODEL,    "comprehensive" );
	
	public GolgiPNodeModel() {
		this(1, 2);
	}
	
	protected GolgiPNodeModel(int nrInDataPorts, int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
	}
	
	public DataTableSpec[] make_output_spec(DataTableSpec inSpec) {
		DataColumnSpec[] cols = new DataColumnSpec[4];
	    DataColumnSpecCreator my_annot_spec = new DataColumnSpecCreator("Annotated Sequence", SequenceCell.TYPE);
    	
		// we must keep the list of tracks as a property associated with the column. We must do this
		// so that annotation dialogs can correctly display the tracks for the user
    	DataColumnProperties isp = new DataColumnProperties();
    	int seq_idx = inSpec.findColumnIndex(m_seq_col.getStringValue());
    	if (seq_idx >= 0) {
    		isp = inSpec.getColumnSpec(seq_idx).getProperties();
    	}
    	// GolgiP node does not add a track, so just copy the input column's properties into the output column
		my_annot_spec.setProperties(isp);
		cols[0] = my_annot_spec.createSpec();
		cols[1] = new DataColumnSpecCreator("Predicted localization", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Score",                  DoubleCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("Accuracy (%)",           DoubleCell.TYPE).createSpec();
		
		DataColumnSpec[] cols2 = new DataColumnSpec[5];
		cols2[0] = new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
		cols2[1] = new DataColumnSpecCreator("Bit Score",DoubleCell.TYPE).createSpec();
		cols2[2] = new DataColumnSpecCreator("CDD ID",   IntCell.TYPE).createSpec();
		cols2[3] = new DataColumnSpecCreator("CDD Name", StringCell.TYPE).createSpec();
		cols2[4] = new DataColumnSpecCreator("CDD Link", StringCell.TYPE).createSpec();
		
		return new DataTableSpec[] { new DataTableSpec(cols2), new DataTableSpec(cols) };
	}
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
		
		DataTableSpec[]        outputSpecs = make_output_spec(inData[0].getSpec());
    	final MyDataContainer c2     = new MyDataContainer(exec.createDataContainer(outputSpecs[1]), "Prot");
    	final MyDataContainer c1     = new MyDataContainer(exec.createDataContainer(outputSpecs[0]), "CDD");
        	
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
			public void process_predictions(SequenceValue sv, String[] prediction_columns) {
				DataCell[] cells = new DataCell[4];
				/*logger.info("Processing results for "+sv.getID());
				logger.info("Prediction columns:");
				for (int i=0; i<prediction_columns.length; i++) {
					logger.info(prediction_columns[i]);
				}*/
				for (int i=0; i<cells.length; i++) {
					cells[i] = DataType.getMissingCell();
				}
				
				try {
					cells[0] = new SequenceCell(sv);
					cells[1] = new StringCell(prediction_columns[0]);
					try {
						cells[2] = new DoubleCell(new Double(prediction_columns[1]).doubleValue());
					} catch (NumberFormatException nfe) {
						cells[2] = DataType.getMissingCell();
						logger.warn("No score for "+sv.getID());
					}
					String tmp = prediction_columns[2];
					if (tmp.endsWith("%")) {
						tmp = tmp.substring(0, tmp.length()-1);
					}
					try {
						cells[3] = new DoubleCell(new Double(tmp).doubleValue());
					} catch (NumberFormatException nfe) {
						logger.warn("No accuracy score for "+sv.getID());
						cells[3] = DataType.getMissingCell();
					}
					c2.addRow(cells);
				} catch (InvalidSettingsException ise) {
					// should not happen since sv comes from valid input...
					ise.printStackTrace();
				}
			}

			@Override
			public void process_conserved_domains(String[] cdd_columns) {
				DataCell[] cells = new DataCell[5];
				for (int i=0; i<cells.length; i++) {
					cells[i] = DataType.getMissingCell();
				}
				cells[0] = new StringCell(cdd_columns[0]);
				cells[1] = new DoubleCell(new Double(cdd_columns[1]).doubleValue());
				cells[2] = new IntCell(new Integer(cdd_columns[2]).intValue());
				cells[3] = new StringCell(new String(cdd_columns[3]));
				cells[4] = new StringCell(new String(cdd_columns[4]));
				c1.addRow(cells);
			}
			
			public void warn(String warning_msg) {
				logger.warn(warning_msg);
			}
		};
		
		int done = 0;
		BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(inData[0].iterator(), 
				seq_idx, 10, 100 * 1024, new SequenceProcessor() {

					@Override
					public SequenceValue process(SequenceValue sv) {
						if (!sv.getSequenceType().isProtein()) {
							logger.warn("GolgiP can only predict protein sequences! Skipping "+sv.getID());
							return null;
						}
						if (sv.getLength() < 1) {
							logger.warn("Skipping empty sequence " + sv.getID());
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
				logger.info("Waiting 30s to be nice to GolgiP server");
				Thread.sleep(30 * 1000);
			}
		}
		
		return new BufferedDataTable[] { c1.close(), c2.close() };
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
		m_organism.saveSettingsTo(settings);
		m_model.saveSettingsTo(settings);
		m_seq_col.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_url.validateSettings(settings);
		m_organism.validateSettings(settings);
		m_model.validateSettings(settings);
		m_seq_col.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_url.loadSettingsFrom(settings);
		m_organism.loadSettingsFrom(settings);
		m_model.loadSettingsFrom(settings);
		m_seq_col.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
	}

}
