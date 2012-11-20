package au.edu.unimelb.plantcell.io.ws.psort;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
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
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcell.io.ws.tmhmm.AbstractWebServiceNodeModel;
import au.edu.unimelb.plantcell.servers.psort.WolfPSortService;
import au.edu.unimelb.plantcell.servers.psort.WolfPSortServicePortType;


public class PSortNodeModel extends AbstractWebServiceNodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Wolf PSort");
    
	public static final String CFGKEY_SEQUENCE    = "sequence-column";
	public static final String CFGKEY_ORGANISM    = "organism";
	public static final String CFGKEY_URL         = "endpoint-url";

	private static final int MAX_RETRIES      = 5;
	
	private final SettingsModelString m_seq_col   = new SettingsModelString(CFGKEY_SEQUENCE, "Sequence");
	private final SettingsModelString m_organism  = new SettingsModelString(CFGKEY_ORGANISM, "plant");
	private final SettingsModelString m_url       = new SettingsModelString(CFGKEY_URL, getDefaultEndpoint());
	private int m_seq_idx;
	
	public PSortNodeModel() {
		super(1, 1);
	}
	
	public DataTableSpec[] make_output_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[4];
		cols[0] = new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Classification", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Score", DoubleCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("k used", IntCell.TYPE).createSpec();
		
		return new DataTableSpec[] { new DataTableSpec(cols) };
	}
	
	public WolfPSortServicePortType getClientProxy() {
  		 // NB: need to use the local WSDL copy rather than go online for it... so...
  		 try {
  			 Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.io.ws");
  			 URL u = FileLocator.find(bundle, new Path("/wsdl/psort@nectar.wsdl"), null);
  			 
  			 // must not call default constructor for local WSDL... so...
  			 WolfPSortService srv = new WolfPSortService(u,
  					new QName("http://nectar.plantcell.unimelb.edu.au", "WolfPSortService"));
  			 return srv.getWolfPSortServiceHttpSoap11Endpoint();
  		 } catch (Exception e) {
  			 e.printStackTrace();
  			 Logger.getAnonymousLogger().warning("Unable to get WolfPSort proxy: "+e.getMessage());
  			 return null;
  		 }
  	}
	
	public String getStatus(String jobId) {
		return getClientProxy().getStatus(jobId);
	}
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
		DataTableSpec[]        outputSpecs = make_output_spec();
    	final MyDataContainer container = new MyDataContainer(exec.createDataContainer(outputSpecs[0]), "Row");
    	 	
    	logger.info("Predicting proteins in "+m_seq_col.getStringValue()+" using "+m_organism.getStringValue()+ " model.");
    	
		int n_rows     = inData[0].getRowCount();
		RowIterator it = inData[0].iterator();
		if (m_seq_idx < 0)
			throw new InvalidSettingsException("Cannot find column: "+m_seq_col.getStringValue()+" - reconfigure the node?");
		if (!inData[0].getSpec().getColumnSpec(m_seq_idx).getType().isCompatible(SequenceValue.class))
			throw new InvalidSettingsException("Sequence column must a SequenceCell (see Sequence 2 String node)!");
		
		WolfPSortServicePortType        svr = getClientProxy();
		
		int done = 0;
        BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(it, m_seq_idx, 
				200, 			// max 200 seqs per batch
				100 * 1024, 	// max 100K residues per batch
				new SequenceProcessor() {

					@Override
					public SequenceValue process(SequenceValue sv) {
						if (sv.getSequenceType().equals(SequenceType.AA)) {
								if (!sv.isValid()) {
									logger.warn("Ignoring invalid sequence: "+sv.getID());
									return null;
								}
								if (sv.getLength() < 40) {
									logger.warn("Ignoring too short sequence for prediction: "+sv.getID());
									return null;
								}
								return sv;
						}
						logger.warn("Skipping non-protein sequence: "+sv.getID());
						return null;
					}

        	});
		
		while (bsi.hasNext()) {
			Map<UniqueID,SequenceValue> batch_map = bsi.nextAsMap();
		
			String result = null;
			for (int i=0; i<MAX_RETRIES; i++) {
				try {
					logger.info("Predicting batch of "+batch_map.size()+" sequences.");
					String jobid = svr.submit(toFasta(batch_map), m_organism.getStringValue().toLowerCase());
					logger.info("Got job id "+jobid+ ", now waiting for batch to complete.");
			
					wait_for_completion(logger, exec, jobid);
					result = svr.getResult(jobid);
					if (result != null)
						break;
				} catch (InterruptedException ie) {
					throw ie;		// user wants to cancel
				} catch (CanceledExecutionException cee) {
					throw cee;
				} catch (Exception e) {
					logger.warn(e.getMessage());
					e.printStackTrace();
					int delay = 300 + (300 * i);
					logger.warn("Waiting "+delay+" seconds before retrying (attempt #"+(i+2)+")");
					Thread.sleep(delay * 1000);
				}
			}
			//logger.info(result);
			if (result == null) {
				continue;
			}
			BufferedReader br = new BufferedReader(new StringReader(result));
			String line;
			Pattern p = Pattern.compile("^(\\S+)\\s+(.*)$");
			Pattern k_re = Pattern.compile("^\\s*k\\s+used\\s+for\\s+kNN\\s+is:\\s+(\\d+)\\s*$");
			int k = -1;
			int got_predictions = 0;
			while ((line = br.readLine()) != null) {
				String tline = line.trim();
				if (tline.length() < 1) {
					continue;
				}
				if (tline.startsWith("#")) {
					logger.info("PSort says: "+tline.substring(1));
					Matcher m = k_re.matcher(tline.substring(1));
					if (m.matches()) {
						k = new Integer(m.group(1)).intValue();
					}
					continue;
				}
				Matcher m = p.matcher(tline);
				if (m.matches()) {
					got_predictions++;
					String accsn = m.group(1);
					String classifications_txt = m.group(2);
					String[] classifications = classifications_txt.split(",\\s+");
					for (String classification : classifications) {
						DataCell[] cells = new DataCell[4];
						cells[0] = new StringCell(batch_map.get(new UniqueID(accsn)).getID());
						int space_idx = classification.indexOf(' ');
						if (space_idx < 0) 
							throw new Exception("Unable to process PSort output!");
						cells[1] = new StringCell(classification.substring(0, space_idx));
						cells[2] = new DoubleCell(new Double(classification.substring(space_idx+1)));
						cells[3] = new IntCell(k);
						container.addRow(cells);
					}
				} else {
					logger.warn("Could not process data on line: "+tline);
				}
			}
			br.close();
			if (got_predictions < batch_map.size()) {
				logger.warn("Did not receive predictions for all sequences! Sent "+batch_map.size()+", got "+got_predictions);
				logger.warn("Sometimes predictions fail for short sequences, you should check.");
			}
			done += batch_map.size();
			exec.checkCanceled();
			exec.setProgress(((double)done)/n_rows);
		}

		return new BufferedDataTable[] { container.close() };
	}
	

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
	            throws InvalidSettingsException {
		m_seq_idx    = inSpecs[0].findColumnIndex(m_seq_col.getStringValue());
		if (m_seq_idx  < 0 && hasSequenceColumn(inSpecs[0])) {
			m_seq_idx = useSequenceColumnIndex(inSpecs[0], logger);
		}
		return make_output_spec();
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_organism.saveSettingsTo(settings);
		m_seq_col.saveSettingsTo(settings);
		m_url.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_organism.validateSettings(settings);
		m_seq_col.validateSettings(settings);
		m_url.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_organism.loadSettingsFrom(settings);
		m_seq_col.loadSettingsFrom(settings);
		m_url.loadSettingsFrom(settings);
	}
}
