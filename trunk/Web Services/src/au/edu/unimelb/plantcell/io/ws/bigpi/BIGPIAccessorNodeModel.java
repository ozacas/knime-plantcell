package au.edu.unimelb.plantcell.io.ws.bigpi;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.namespace.QName;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.osgi.framework.Bundle;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcell.io.ws.tmhmm.AbstractWebServiceNodeModel;
import au.edu.unimelb.plantcell.servers.bigpi.BIGPIService;
import au.edu.unimelb.plantcell.servers.bigpi.BIGPIServicePortType;


/**
 * Accessor for the BIG-PI web server installed on the Nectar cloud (nectar.org.au) or any server with the 
 * cloud-bioservices tomcat webapp installed. The node supports both primary omega and, where applicable, secondary site predictions
 * in short format (for now). BIG-PI predicts GPI anchors amongst C-Terminal protein sequence
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class BIGPIAccessorNodeModel extends AbstractWebServiceNodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("BIG-PI Accessor");
    
    // dialog configuration & model settings
	public static final String CFGKEY_SEQUENCE      = "sequence-column";
	public static final String CFGKEY_URL           = "endpoint-url";
	public static final String CFGKEY_ORGANISM_TYPE = "organism-type";

	private static final int MAX_RETRIES = 5;

	public static final String[] ORGANISM_TYPES   = new String[] { "viridiplantae", "metazoa", "protozoa", "fungi"  };
	
	private final SettingsModelString m_seq_col   = new SettingsModelString(CFGKEY_SEQUENCE, "Sequence");
	private final SettingsModelString m_url       = new SettingsModelString(CFGKEY_URL, getDefaultEndpoint());
	private final SettingsModelString m_model     = new SettingsModelString(CFGKEY_ORGANISM_TYPE, ORGANISM_TYPES[0]);
	private int m_seq_idx = -1;
	
	public BIGPIAccessorNodeModel() {
		super(1, 1);
	}
	
	public BIGPIServicePortType getClientProxy() {
   		 // NB: need to use the local WSDL copy rather than go online for it... so...
   		 try {
   			 Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.io.ws");
   			 URL u = FileLocator.find(bundle, new Path("/wsdl/bigpi@nectar.wsdl"), null);
   			 
   			 // must not call default constructor for local WSDL... so...
   			BIGPIService srv = new BIGPIService(u,
   					new QName("http://nectar.plantcell.unimelb.edu.au", "BIGPIService"));
   			 return srv.getBIGPIServiceHttpSoap11Endpoint();
   		 } catch (Exception e) {
   			 e.printStackTrace();
   			 Logger.getAnonymousLogger().warning("Unable to get BIGPI proxy: "+e.getMessage());
   			 return null;
   		 }
   	}
	
	@Override
	public String getStatus(String jobId) {
		return getClientProxy().getStatus(jobId);
	}
	 
	public DataTableSpec[] make_output_spec(DataTableSpec inSpec) throws InvalidSettingsException {
		DataColumnSpec[] cols = new DataColumnSpec[9];
		cols[0] = new DataColumnSpecCreator("Has GPI anchor prediction?", BooleanCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Primary site: Total Score", DoubleCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Primary site: P-value", DoubleCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("Primary site: Position of best scoring AA (starts at 1)", IntCell.TYPE).createSpec();
		cols[4] = new DataColumnSpecCreator("Primary site: identification status", StringCell.TYPE).createSpec();
		cols[5] = new DataColumnSpecCreator("Secondary site: Total Score", DoubleCell.TYPE).createSpec();
		cols[6] = new DataColumnSpecCreator("Secondary site: P-Value", DoubleCell.TYPE).createSpec();
		cols[7] = new DataColumnSpecCreator("Secondary site: Position of best scoring AA (starts at 1)", IntCell.TYPE).createSpec();
		cols[8] = new DataColumnSpecCreator("Secondary site: identification status", StringCell.TYPE).createSpec();
		
		return new DataTableSpec[] { new DataTableSpec(inSpec, new DataTableSpec(cols)) };
	}
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
	
    	logger.info("Predicting GPI anchors from sequences in "+m_seq_col.getStringValue());
    	
		int n_rows     = inData[0].getRowCount();
		if (m_seq_idx < 0)
			throw new InvalidSettingsException("Cannot find column: "+m_seq_col.getStringValue()+" - reconfigure the node?");
		if (!inData[0].getSpec().getColumnSpec(m_seq_idx).getType().isCompatible(SequenceValue.class))
			throw new InvalidSettingsException("No suitable Sequence column (see Columns to Sequence node)!");
		
		DataTableSpec[] outputSpecs = make_output_spec(inData[0].getSpec());
		
    	final MyDataContainer c1 = new MyDataContainer(exec.createDataContainer(outputSpecs[0]), "Pred");
    	
		BIGPIServicePortType proxy = getClientProxy();
		
		int done = 0;

		BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(inData[0].iterator(), 
				m_seq_idx, 100, 200 * 1024, new SequenceProcessor() {

					@Override
					public SequenceValue process(SequenceValue sv) {
						if (!sv.getSequenceType().isProtein()) {
							logger.warn("Skipping sequence "+sv.getID()+" - must be protein sequence!");
							return null;
						}
						if (sv.getLength() < 1) {
							logger.warn("Skipping empty sequence: "+sv.getID());
							return null;
						}
						return sv;
					}
			
		});
		bsi.setSaveRows(true);
		
		while (bsi.hasNext()) {
			Map<UniqueID,SequenceValue> batch_map = bsi.nextAsMap();
			List<DataRow> batch_rows = bsi.lastBatchRows();
			Map<UniqueID,DataRow> uid2row = make_uid2row_map(batch_map, batch_rows);
			
			int n_out_cols = outputSpecs[0].getNumColumns() - inData[0].getSpec().getNumColumns();
			for (int i=0; i<MAX_RETRIES; i++) {
				exec.checkCanceled();
				try {
					logger.info("Predicting batch of "+batch_map.size()+" sequences.");
					String fasta = toFasta(batch_map);
					logger.info("Made "+fasta.length()+" bytes of FASTA file for submission to BigPI");
					String jobid = proxy.submit(fasta, m_model.getStringValue());
					logger.info("Got job id "+jobid+ ", now waiting for batch to complete.");
					
					wait_for_completion(logger, exec, jobid);
					String result = proxy.getResult(jobid);
					//logger.info(result);
				
					HashSet<UniqueID> got_hits = new HashSet<UniqueID>();
					for (String line : result.split("\\n")) {
						if (line.startsWith("WARNING")) {
							logger.warn(line);
							continue;
						} else if (line.trim().length() > 0) {
							String[] fields = line.trim().split("\\s+");
							DataCell[] out = missing_cells(n_out_cols);
							
							UniqueID uid = new UniqueID(fields[1]);
							String pos   = fields[7];
							String has_no_gpi  = fields[12];
							got_hits.add(uid);
							
							// primary site cells
							out[0] = has_no_gpi.equals("NO_GPI_SITE") ? BooleanCell.FALSE : BooleanCell.TRUE;
							try {
								out[3] = new IntCell(Integer.valueOf(pos));
							} catch (NumberFormatException nfe) {
								// out[3] is already a missing cell, so just warn
								logger.warn(nfe.getMessage());
							}
							out[4] = report_identification_status(fields[6]);
							try {
								out[1] = new DoubleCell(Double.valueOf(fields[9]));
								out[2] = new DoubleCell(Double.valueOf(fields[11]));
							} catch (NumberFormatException nfe) {
								logger.warn(nfe.getMessage());
							}
							
							// handle secondary site cells
							if (has_no_gpi.startsWith("2:") && fields.length > 16) {
								try {
									out[7] = new IntCell(Integer.valueOf(fields[13]));
									out[5] = new DoubleCell(Double.valueOf(fields[15]));
									out[6] = new DoubleCell(Double.valueOf(fields[17]));
								} catch (NumberFormatException nfe) {
									logger.warn(nfe.getMessage());
								}
								out[8] = report_identification_status(fields[12]);
							}
							
							DataRow input_row = uid2row.get(uid);
							assert(input_row != null);
							c1.addRow(new JoinedRow(input_row, new DefaultRow(input_row.getKey(), out)));
						}
					}
					
					if (got_hits.size() < batch_map.size()) {
						logger.warn("Expected hits for "+batch_map.size()+" sequences, but got hits for only "+got_hits.size()+" - missing values added. Too short for GPI prediction?");
						for (UniqueID uid : uid2row.keySet()) {
							if (!got_hits.contains(uid)) {
								DataRow input_row = uid2row.get(uid);
							
								c1.addRow(new JoinedRow(input_row, new DefaultRow(input_row.getKey(), missing_cells(n_out_cols))));
							}
						}
					}
					done += batch_map.size();
					break;
				} catch (Exception e) {
					if (e instanceof CanceledExecutionException || e instanceof InterruptedException)
						throw e;
					int delay = 200 + (200*i);
					if (i<MAX_RETRIES-1) {
						logger.warn("Unable to contact server (retrying in "+delay+" seconds): "+e.getMessage());
						Thread.sleep(delay*1000);
					}
				}
			}
			exec.checkCanceled();
			exec.setProgress(((double)done)/n_rows);
		}
		
		return new BufferedDataTable[] { c1.close() };
	}
	

	/**
	 * Maps a string like "1:P" to a user-meaningful description (taken from the BIG-PI README file)
	 */
	private DataCell report_identification_status(String str) {
		String temp = str.toLowerCase().trim();
		if (temp.length() < 1)
			return DataType.getMissingCell();
		char status = temp.charAt(temp.length()-1);
		switch (status) {
		case 'p':
			return new StringCell("good site");
		case 's':
			return new StringCell("twilight zone");
		case 'n':
			return new StringCell("rejection due to total score");
		case 'i':
			return new StringCell("rejection due to physical properties");
		default:
			return DataType.getMissingCell();
		}
	}

	private Map<UniqueID, DataRow> make_uid2row_map(
			Map<UniqueID, SequenceValue> batch_map, List<DataRow> batch_rows) {
		HashMap<UniqueID, DataRow> ret = new HashMap<UniqueID, DataRow>();
		for (DataRow dr : batch_rows) {
			DataCell dc = dr.getCell(m_seq_idx);
			if (dc == null || dc.isMissing())
				continue;
			SequenceValue sv = (SequenceValue) dc;
			for (UniqueID uid : batch_map.keySet()) {
				SequenceValue s2 = batch_map.get(uid);
				if (s2.equals(sv)) {
					ret.put(uid, dr);
				}
			}
		}
		return ret;
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
	            throws InvalidSettingsException {
		m_seq_idx = inSpecs[0].findColumnIndex(m_seq_col.getStringValue());
		if (m_seq_idx  < 0 && hasSequenceColumn(inSpecs[0])) {
			m_seq_idx = useSequenceColumnIndex(inSpecs[0], logger);
		}
		return make_output_spec(inSpecs[0]);
	}
	
	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_seq_col.saveSettingsTo(settings);
		m_url.saveSettingsTo(settings);
		m_model.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_seq_col.validateSettings(settings);
		m_url.validateSettings(settings);
		m_model.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_seq_col.loadSettingsFrom(settings);
		m_url.loadSettingsFrom(settings);
		m_model.loadSettingsFrom(settings);
	}
}
