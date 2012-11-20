package au.edu.unimelb.plantcell.io.ws.estscan_nectar;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URL;
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
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.osgi.framework.Bundle;

import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcell.io.read.fasta.FastaIterator;
import au.edu.unimelb.plantcell.io.ws.tmhmm.AbstractWebServiceNodeModel;
import au.edu.unimelb.plantcell.servers.nectar.estscan.ESTScanService;
import au.edu.unimelb.plantcell.servers.nectar.estscan.ESTScanServicePortType;



public class ESTScanNodeModel extends AbstractWebServiceNodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("ESTScan@Nectar");
    
   
    // dialog configuration & model settings
	public static final String CFGKEY_SEQUENCE    = "sequence-column";
	public static final String CFGKEY_URL         = "endpoint-url";
	public static final String CFGKEY_PROGRAM     = "estscan-model";

	private static final int MAX_RETRIES = 5;
    
	private final SettingsModelString m_seq_col   = new SettingsModelString(CFGKEY_SEQUENCE, "Sequence");
	private final SettingsModelString m_url       = new SettingsModelString(CFGKEY_URL, getDefaultEndpoint());
	private final SettingsModelString m_model     = new SettingsModelString(CFGKEY_PROGRAM, "Oryza_Sativa");
	
	private int m_seq_idx = -1;
	
	public ESTScanNodeModel() {
		super(1, 2);
	}

	public static ESTScanServicePortType getClientProxy() {
   		 // NB: need to use the local WSDL copy rather than go online for it... so...
   		 try {
   			 Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.io.ws");
   			 URL u = FileLocator.find(bundle, new Path("/wsdl/estscan@nectar.wsdl"), null);
   			 
   			 // must not call default constructor for local WSDL... so...
   			ESTScanService srv = new ESTScanService(u,
   					new QName("http://nectar.plantcell.unimelb.edu.au", "ESTScanService"));
   			 return srv.getESTScanServiceHttpSoap11Endpoint();
   		 } catch (Exception e) {
   			 e.printStackTrace();
   			 Logger.getAnonymousLogger().warning("Unable to get ESTScan proxy: "+e.getMessage());
   			 return null;
   		 }
   	}
	
	@Override
	public String getStatus(String jobId) {
		return getClientProxy().getStatus(jobId);
	}
	
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
	
    	logger.info("Running ESTscan over sequences in "+m_seq_col.getStringValue());
    	
		int n_rows     = inData[0].getRowCount();
		RowIterator it = inData[0].iterator();
		if (m_seq_idx < 0)
			throw new InvalidSettingsException("Cannot find column: "+m_seq_col.getStringValue()+" - reconfigure the node?");
		if (!inData[0].getSpec().getColumnSpec(m_seq_idx).getType().isCompatible(SequenceValue.class))
			throw new InvalidSettingsException("Sequence column must a SequenceCell (see Sequence 2 String node)!");
		
		DataTableSpec[] outputSpecs = make_output_spec();
		
    	final MyDataContainer c1 = new MyDataContainer(exec.createDataContainer(outputSpecs[0]), "Nuc");
    	final MyDataContainer c2 = new MyDataContainer(exec.createDataContainer(outputSpecs[1]), "Prot");
    	
		ESTScanServicePortType proxy = getClientProxy();
		
		int done = 0;
		BatchSequenceRowIterator bsri = new BatchSequenceRowIterator(it, m_seq_idx, 50, 2 * 1024 * 1024, new SequenceProcessor() {

			@Override
			public SequenceValue process(SequenceValue sv) {
				if (!sv.getSequenceType().isNucleotides()) {
					logger.info("Skipping "+sv.getID()+" as it is not nucleotide sequence");
					return null;
				}
				//if (sv.getID().indexOf(" ") >= 0) {
				//	logger.warn("Sequence IDs not allowed to contain whitespace in this node! Skipping: "+sv.getID());
				//}
				return sv;
			}
			
		});
		
		while (bsri.hasNext()) {
			Map<UniqueID,SequenceValue> batch_map = bsri.nextAsMap();
			if (batch_map == null)
				break;
			
			for (int i=0; i<MAX_RETRIES; i++) {
				exec.checkCanceled();
				try {
					logger.info("Predicting batch of "+batch_map.size()+" sequences.");
					String jobid = proxy.submit(toFasta(batch_map), m_model.getStringValue());
					logger.info("Got job id "+jobid+ ", now waiting for batch to complete.");
					
					wait_for_completion(logger, exec, jobid);
					String result = proxy.getResult(jobid);
					
					if (result == null) {
						logger.warn("No results for batch (server problem?) -- continuing anyway!");
						continue;
					}
					//logger.info(result);
					grok_result(new BufferedReader(new StringReader(result)), batch_map, c1, c2);
					
					done += batch_map.size();
					break;
				} catch (Exception e) {
					if (e instanceof CanceledExecutionException || e instanceof InterruptedException)
						throw e;
					int delay = 200 + (200*i);
					if (i<MAX_RETRIES-1) {
						logger.warn("Unable to contact server (retrying in "+delay+" seconds): "+e.getMessage());
						e.printStackTrace();
						Thread.sleep(delay*1000);
					}
				}
			}
			exec.checkCanceled();
			exec.setProgress(((double)done)/n_rows);
		}
		
		return new BufferedDataTable[] { c1.close(), c2.close()};
	}
	
	
	public void grok_result(final BufferedReader br, Map<UniqueID, SequenceValue> batch_map, 
						final MyDataContainer c1, final MyDataContainer c2) throws Exception {
		String line;
	
		StringBuilder nuc_fasta = new StringBuilder(100 * 1024);
		StringBuilder prot_fasta= new StringBuilder(100 * 1024);
		StringBuilder sb = null;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#")) {
				if (line.indexOf("nucleotide") >= 0) {
					sb = nuc_fasta;
				} else {
					sb = prot_fasta;
				}
				continue;
			}
			sb.append(line);
			sb.append("\n");
		}	
		
		FastaIterator fi = new FastaIterator(new StringReader(nuc_fasta.toString()), SequenceType.Nucleotide);
		addSequenceRows(c1, fi, batch_map);
		
		fi = new FastaIterator(new StringReader(prot_fasta.toString()), SequenceType.AA);
		addSequenceRows(c2, fi, batch_map);
	}

	private void addSequenceRows(MyDataContainer c, FastaIterator fi, 
								 Map<UniqueID,SequenceValue> batch_map) throws Exception {
		
		int found = 0;
		while (fi.hasNext()) {
			DataCell[] cells = new DataCell[1];
			SequenceValue sv = fi.next();
			String id = sv.getID().trim();
			int   idx = id.indexOf(';');
			if (idx >= 0) {
				id = id.substring(0, idx);
			}
			
			SequenceValue orig_seq = batch_map.get(new UniqueID(id));
			if (orig_seq == null) 
				throw new Exception("Unknown sequence in batch with ID: "+id);
			else
				found++;
			sv.setID(orig_seq.getID());
			cells[0] = new SequenceCell(sv);
			c.addRow(cells);
		}
		if (found != batch_map.size()) {
			logger.warn("Expected "+batch_map.size()+" sequence predictions, but got only "+found);
		}
	}
	
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
	            throws InvalidSettingsException {
		// we set this here to have the node auto-configure to the configured sequence column
		m_seq_idx = inSpecs[0].findColumnIndex(m_seq_col.getStringValue());
		if (m_seq_idx  < 0 && hasSequenceColumn(inSpecs[0])) {
			m_seq_idx = useSequenceColumnIndex(inSpecs[0], logger);
		}
		return make_output_spec();
	}

	private DataTableSpec[] make_output_spec() {
		DataColumnSpec[] nucl_rows = new DataColumnSpec[1];
		nucl_rows[0] = new DataColumnSpecCreator("Nucleotide sequences", SequenceCell.TYPE).createSpec();
		
		DataColumnSpec[] prot_rows = new DataColumnSpec[1];
		prot_rows[0] = new DataColumnSpecCreator("Protein coding sequence", SequenceCell.TYPE).createSpec();
		
		return new DataTableSpec[] { new DataTableSpec(nucl_rows), new DataTableSpec(prot_rows) };
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

	public static String[] getAvailableModels() {
		ESTScanServicePortType proxy = getClientProxy();
		List<String> models = proxy.getAvailableScoreModels();
		if (models == null || models.size() < 1) {
			return new String[] { "Oryza_Sativa" };
		}
		return models.toArray(new String[0]);
	}

}
