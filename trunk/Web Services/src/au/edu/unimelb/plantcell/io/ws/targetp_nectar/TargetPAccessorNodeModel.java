package au.edu.unimelb.plantcell.io.ws.targetp_nectar;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.osgi.framework.Bundle;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcell.io.ws.tmhmm.AbstractWebServiceNodeModel;
import au.edu.unimelb.plantcell.servers.nectar.targetp.TargetPService;
import au.edu.unimelb.plantcell.servers.nectar.targetp.TargetPServicePortType;


/**
 * This is the model implementation of TargetPAccessor.
 * Integrates the CBS TargetP web service into KNIME providing subcellular location predictions of given protein sequences
 *
 * @author Andrew Cassin
 */
public class TargetPAccessorNodeModel extends AbstractWebServiceNodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("TargetP");
        
    private final int NUM_COLUMNS = 7;			// number of columns in output table
    public static final String CFGKEY_SEQUENCE = "sequence-column";
    public static final String CFGKEY_ORGANISM = "organism";
    public static final String CFGKEY_CTP_CUTOFF = "ctp-cutoff";
    public static final String CFGKEY_SP_CUTOFF = "sp-cutoff"; 
    public static final String CFGKEY_MTP_CUTOFF = "mtp-cutoff"; 
    public static final String CFGKEY_OTHER_CUTOFF = "other-cutoff";
   
    
    private final SettingsModelString m_seq = new SettingsModelString(CFGKEY_SEQUENCE, "Sequence");
    private final SettingsModelString     m_organism = new SettingsModelString(CFGKEY_ORGANISM, "plant");
    private final SettingsModelDoubleBounded    m_ctp = new SettingsModelDoubleBounded(CFGKEY_CTP_CUTOFF, 0.0, 0.0, 1.0);
    private final SettingsModelDoubleBounded    m_sp = new SettingsModelDoubleBounded(CFGKEY_SP_CUTOFF, 0.0, 0.0, 1.0);
    private final SettingsModelDoubleBounded    m_mtp = new SettingsModelDoubleBounded(CFGKEY_MTP_CUTOFF, 0.0, 0.0, 1.0);
    private final SettingsModelDoubleBounded    m_other = new SettingsModelDoubleBounded(CFGKEY_OTHER_CUTOFF, 0.0, 0.0, 1.0);
    private int m_seq_idx = -1;

    /**
     * Constructor for the node model.
     */
    protected TargetPAccessorNodeModel() {
        super(1, 1);
    }

    protected TargetPServicePortType getClientProxy() {
 		 // NB: need to use the local WSDL copy rather than go online for it... so...
 		 try {
 			 Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.io.ws");
 			 URL u = FileLocator.find(bundle, new Path("/wsdl/targetp@nectar.wsdl"), null);
 			 
 			 // must not call default constructor for local WSDL... so...
 			 TargetPService srv = new TargetPService(u,
 					new QName("http://nectar.plantcell.unimelb.edu.au", "TargetPService"));
 			 return srv.getTargetPServiceHttpSoap11Endpoint();
 		 } catch (Exception e) {
 			 e.printStackTrace();
 			 Logger.getAnonymousLogger().warning("Unable to get TargetP proxy: "+e.getMessage());
 			 return null;
 		 }
 	}
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	logger.info("Predicting " + inData[0].getRowCount() + " sequences for subcellular location");
    	if (m_seq_idx < 0)
    		throw new InvalidSettingsException("Cannot find column: "+ m_seq.getStringValue());
    	
    	DataTableSpec[]      outputSpec = make_output_spec(inData[0].getDataTableSpec());
    	
    	final MyDataContainer container = new MyDataContainer(exec.createDataContainer(outputSpec[0]), "Seq");
    	
    	BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(inData[0].iterator(), m_seq_idx, 
    			40, 180 * 1000, new SequenceProcessor() {

					@Override
					public SequenceValue process(SequenceValue sv) {
						if (!sv.getSequenceType().isProtein()) {
							logger.warn("Skipping non-protein sequence "+sv.getID());
							return null;
						}
						int len = sv.getLength();
						if (len < 5) {
			        		logger.warn("Ignoring too short sequence for row "+sv.getID());
			        		return null;
						}
						if (len > 4000) {
			        		logger.warn("Sequence "+sv.getID()+" is too long for TargetP, ignored.");
			        		return null;
						}
			        	return sv;
					}
    		
    	});
    
    	int done = 0;
    	bsi.setSaveRows(true);
    	while (bsi.hasNext()) {
    		Map<UniqueID,SequenceValue> batch = bsi.nextAsMap();
    		List<DataRow> rows_in_batch = bsi.lastBatchRows();
    		
    		// submit and process entire batch
    		String job_id = submit_job(getClientProxy(), batch);
    		// wait until done (or exception thrown)
    		wait_for_completion(logger, exec, job_id);
    		
    		logger.info("Processing result for TargetP job "+job_id+ ": "+batch.size()+" sequences.");
    		int got_results = grok_results(batch, container, job_id, rows_in_batch);
			if (got_results < batch.size()) {
				logger.warn("Expected results for "+batch.size()+" sequences, only got results for "+got_results);
			}
			done += batch.size();
			
    		exec.setProgress(((double)done) / inData[0].getRowCount());
    		exec.checkCanceled();
    	}
    
        return new BufferedDataTable[]{container.close()};
    }

    private void add_row(MyDataContainer c, DataRow input_row, DataCell[] cells) {
    	assert(input_row != null && cells != null && cells.length > 0);
    	
    	ArrayList<DataCell> new_cells = new ArrayList<DataCell>(cells.length + input_row.getNumCells());
    	for (int i=0; i<input_row.getNumCells(); i++) {
    		new_cells.add(input_row.getCell(i));
    	}
    	for (int i=0; i<cells.length; i++) {
    		new_cells.add(cells[i]);
    	}
    	c.addRow(new_cells.toArray(new DataCell[0]));
    }
    
    private int grok_results(Map<UniqueID, SequenceValue> batch, MyDataContainer c, String job_id, List<DataRow> rows) throws NumberFormatException,InvalidSettingsException {
		String result = getClientProxy().getResult(job_id);
		if (result == null || m_seq_idx < 0)
			return 0;
		
		// 1. establish the map between the SequenceValue and DataRow so we can merge the output cells
		Map<String,UniqueID> rev_batch = new HashMap<String,UniqueID>();
		Map<UniqueID,DataRow> rev_rows = new HashMap<UniqueID,DataRow>();
		for (UniqueID uid : batch.keySet()) {
			rev_batch.put(batch.get(uid).getID(), uid);
		}
		for (DataRow r : rows) {
			DataCell cell = r.getCell(m_seq_idx);
			if (cell == null || cell.isMissing() || !(cell instanceof SequenceValue))
				continue;
			SequenceValue sv = (SequenceValue) cell;
			
			rev_rows.put(rev_batch.get(sv.getID()), r);
		}
		
		// 2. process the results
		int found = 0;
		String[] lines = result.split("\n");
		boolean processing = false;
		for (String line : lines) {
			if (line.startsWith("----")) {
				if (processing)
					break;
				processing = true;
				continue;
			}
			
			if (processing) {
				String[]  fields = line.split("\\s+");
				DataCell[] cells = missing_cells(NUM_COLUMNS);
				UniqueID uid = new UniqueID(fields[0]);
				
				DataRow input_row = rev_rows.get(uid);
				if (input_row == null)
					throw new InvalidSettingsException("Unable to find data row");
				
				if (fields.length == 9) {	// plant?
					cells[0] = new DoubleCell(Double.valueOf(fields[2]));
					cells[1] = new DoubleCell(Double.valueOf(fields[3]));
					cells[2] = new DoubleCell(Double.valueOf(fields[4]));
					cells[3] = new DoubleCell(Double.valueOf(fields[5]));
					if (!fields[6].equals("_") && fields[6].length() > 0)
						cells[4] = new StringCell(fields[6]);
					if (fields[7].length() > 0)
						cells[5] = new IntCell(Integer.valueOf(fields[7]));
					if (fields[8].length() > 0 && !fields[8].equals("-"))
						cells[6] = new IntCell(Integer.valueOf(fields[8]));
					add_row(c, input_row, cells);
					found++;
				} else if (fields.length == 8) { // non-plant?
					//cells[0] = DataType.getMissingCell();	// non-plants have no chloroplast (usually)
					cells[1] = new DoubleCell(Double.valueOf(fields[2]));
					cells[2] = new DoubleCell(Double.valueOf(fields[3]));
					cells[3] = new DoubleCell(Double.valueOf(fields[4]));
					if (!fields[5].equals("_") && fields[5].length() > 0)
						cells[4] = new StringCell(fields[5]);
					if (fields[6].length() > 0)
						cells[5] = new IntCell(Integer.valueOf(fields[6]));
					if (fields[7].length() > 0 && !fields[7].equals("-"))
						cells[6] = new IntCell(Integer.valueOf(fields[7]));
					add_row(c, input_row, cells);
					found++;
				} else {
					throw new InvalidSettingsException("Unknown results format from TargetP server: "+line);
				}
			}
		}
		return found;
	}

    /**
     * Send the job via the specified <code>proxy</code> for the batch of sequences specified by <code>batch</code>
     * @param proxy
     * @param batch
     * @return
     * @throws Exception
     */
	private String submit_job(TargetPServicePortType proxy, Map<UniqueID,SequenceValue> batch) throws Exception {
		logger.info("Submitting batch of "+batch.size()+" sequences.");
		String jobID = proxy.submit(m_organism.getStringValue().toLowerCase().equals("plant"), 
				m_ctp.getDoubleValue(), m_sp.getDoubleValue(), m_mtp.getDoubleValue(), 
				m_other.getDoubleValue(), toFasta(batch));
		if (jobID == null) 
			throw new Exception("Unable to submit job to TargetP server - do you have a working Internet connection?");
		logger.info("Got batch job id: "+jobID);
		return jobID;
	}


	private DataTableSpec[] make_output_spec(DataTableSpec input_table_spec) throws IllegalArgumentException, NullPointerException {
    	DataColumnSpec[] cols = new DataColumnSpec[NUM_COLUMNS];
    		
    	cols[0] = new DataColumnSpecCreator("cTP Score", DoubleCell.TYPE).createSpec();
    	cols[1] = new DataColumnSpecCreator("mTP Score", DoubleCell.TYPE).createSpec();
    	cols[2] = new DataColumnSpecCreator("SP Score", DoubleCell.TYPE).createSpec();
    	cols[3] = new DataColumnSpecCreator("Other Score", DoubleCell.TYPE).createSpec();
    	cols[4] = new DataColumnSpecCreator("Localisation Prediction", StringCell.TYPE).createSpec();
    	cols[5] = new DataColumnSpecCreator("Reliability Class (lower is better)", IntCell.TYPE).createSpec();
    	cols[6] = new DataColumnSpecCreator("TPlen", IntCell.TYPE).createSpec();
    
		return new DataTableSpec[] { new DataTableSpec(input_table_spec, new DataTableSpec(cols)) };
	}


    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    	try {
        	m_seq_idx = inSpecs[0].findColumnIndex(m_seq.getStringValue());
        	if (m_seq_idx < 0 && hasSequenceColumn(inSpecs[0])) {
        		m_seq_idx = useSequenceColumnIndex(inSpecs[0], logger);
        	}
    		return make_output_spec(inSpecs[0]);
    	} catch (Exception e) {
    		e.printStackTrace();
    		throw new InvalidSettingsException(e.getMessage());
    	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_seq.saveSettingsTo(settings);
    	m_organism.saveSettingsTo(settings);
    	m_ctp.saveSettingsTo(settings);
    	m_sp.saveSettingsTo(settings);
    	m_mtp.saveSettingsTo(settings);
    	m_other.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_seq.loadSettingsFrom(settings);
    	m_organism.loadSettingsFrom(settings);
    	m_ctp.loadSettingsFrom(settings);
    	m_sp.loadSettingsFrom(settings);
    	m_mtp.loadSettingsFrom(settings);
    	m_other.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_seq.validateSettings(settings);
    	m_organism.validateSettings(settings);
    	m_ctp.validateSettings(settings);
    	m_sp.validateSettings(settings);
    	m_mtp.validateSettings(settings);
    	m_other.validateSettings(settings);
    }

	@Override
	public String getStatus(String jobID) throws Exception {
		return getClientProxy().getStatus(jobID);
	}
}

