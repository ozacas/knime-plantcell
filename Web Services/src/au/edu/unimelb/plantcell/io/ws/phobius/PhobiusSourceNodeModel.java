package au.edu.unimelb.plantcell.io.ws.phobius;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
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
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import uk.ac.ebi.jdispatcher.soap.phobius.InputParameters;
import uk.ac.ebi.jdispatcher.soap.phobius.JDispatcherService;
import uk.ac.ebi.jdispatcher.soap.phobius.JDispatcherService_Service;
import uk.ac.ebi.jdispatcher.soap.phobius.ObjectFactory;
import uk.ac.ebi.jdispatcher.soap.phobius.WsResultType;
import uk.ac.ebi.jdispatcher.soap.phobius.WsResultTypes;
import au.edu.unimelb.plantcell.core.Cache;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.cells.CoordinateSystem;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;
import au.edu.unimelb.plantcell.core.cells.TrackCreator;
import au.edu.unimelb.plantcore.core.regions.PhobiusRegionsAnnotation;
import au.edu.unimelb.plantcore.core.regions.Region;
import au.edu.unimelb.plantcore.core.regions.RegionsAnnotation;





/**
 * This is the model implementation of PhobiusSource.
 * Takes a list of sequences and appends the results of Phobius webservice invocations (text only for now) to the output port
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class PhobiusSourceNodeModel extends NodeModel {
    

    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Phobius");
    
    /** the settings key which is used to retrieve and 
    store the settings (from the dialog or from a settings file)    
   (package visibility to be usable from the dialog). */
    static final String CFGKEY_SEQUENCE_COL = "sequence";
    static final String CFGKEY_EMAIL        = "email";
    static final String CFGKEY_FRESHNESS    = "cache-freshness-days";
    static final String CFGKEY_CACHE_FILE   = "cache-filename";
    public final int    MAX_RETRIES         = 3;
    
    private static final String DEFAULT_SEQUENCE_COL = "Biological Sequence";
	private static final String DEFAULT_EMAIL        = "must@specify.this.to.use.this.node";
	
	// internal state (persisted as part of workflow)
	private final SettingsModelString m_seq_col     = make_as_string(CFGKEY_SEQUENCE_COL);
	private final SettingsModelString m_email       = make_as_string(CFGKEY_EMAIL);
	
	// internal state (not persisted)
	private int                m_done_rows;
	private JDispatcherService m_phobius;
	private Cache              m_cache;
	
    /**
     * Constructor for the node model.
     */
    protected PhobiusSourceNodeModel() {
        super(1, 1);
        m_phobius = null;
        m_cache = null;
    }
    
    public static SettingsModel make (String k) {
    	if (k.equals(CFGKEY_SEQUENCE_COL)) {
    		return new SettingsModelString(k, DEFAULT_SEQUENCE_COL);
    	} else if (k.equals(CFGKEY_EMAIL)) {
    		return new SettingsModelString(k, DEFAULT_EMAIL);
    	} 
    	return null;
    }
    
    public static SettingsModelString make_as_string(String k) {
    	SettingsModel sm = make(k);
    	return (SettingsModelString) sm;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        int seq_col_idx = inData[0].getDataTableSpec().findColumnIndex(m_seq_col.getStringValue());
        int batch_size = 20;
        if (seq_col_idx < 0) {
        	throw new Exception("Cannot find column: "+m_seq_col.getStringValue()+" - reset the node?");
        }
        
        
		DataTableSpec[] oSpec = make_output_spec(new DataTableSpec[] { inData[0].getSpec() });
        MyDataContainer c1 = new MyDataContainer(exec.createDataContainer(oSpec[0]), "Seq");
          
        RowIterator it = inData[0].iterator();

        JDispatcherService_Service srv = new JDispatcherService_Service();
        m_phobius     = srv.getJDispatcherServiceHttpPort();
        int batch_cnt = 0;
        m_done_rows   = 0;
    	@SuppressWarnings("rawtypes")
		ArrayList<HashMap> batch = new ArrayList<HashMap>();
    	
    	// setup data cache
    	try {
            logger.info("Initialising Phobius cache");
            Properties p = new Properties();
            p.setProperty(Cache.CACHE_MAX_SIZE, "100000");
            p.setProperty(Cache.CACHE_AUTO_COMMIT, "true");
			m_cache = new Cache("phobius_v2", p);
			m_cache.init();
    	} catch (Exception e) {
    		e.printStackTrace();
    		logger.warn("Cache disabled: unable to setup cache -"+e.getMessage());
    		m_cache = null;
    		// fallthru, cache will be disabled...
    	}
     	
    	// validate email address
    	String email = m_email.getStringValue();
    	if (email.equals(DEFAULT_EMAIL) || email.trim().length() < 1) {
    		throw new Exception("Must set email address to be valid for you! EBI require this!");
    	}
    	
        // process rows
    	try {
	        while (it.hasNext()) {
	        	DataRow r = it.next();
	        	DataCell seq_cell = r.getCell(seq_col_idx);
	        	if (seq_cell == null || seq_cell.isMissing() || !(seq_cell instanceof SequenceValue) ) {
	        		logger.warn("Skipping unsuitable value for row "+r.getKey().getString());
	        		continue;
	        	}
	        	
	        	SequenceValue sv = (SequenceValue) seq_cell;
	        	if (!sv.isValid()) {
	        		logger.warn("Sequence "+sv.getID()+" is not valid (illegal symbols?)... skipping");
	        		continue;
	        	}
	    
	        	// try to find a suitable cache object and use that...
	        	if (m_cache != null) {
	        		try {
		        		String key = CacheablePhobiusRecord.makeKey(sv.getStringValue());
		        		if (m_cache.contains(key)) {
			        		String s = m_cache.get(key);
		        			CacheablePhobiusRecord rec = new CacheablePhobiusRecord(key, s);
		        			grok_cells(rec.getJobID(), rec.getPhobiusResult(), sv, c1);
		        			
		        			logger.info("Retrieved cached Phobius record for "+sv.getID());
		        			m_done_rows++;
		        			updateProgress(exec, inData[0].getRowCount());
		        			continue;
		        		}
	        		// else fallthru to normal processing...
	        		} catch (Exception ex) {
	    				ex.printStackTrace();
	    				// fallthru, just pretend the object wasnt cached...
	    			}
	        	}
	        	
	        	// else not in the cache so...
	        	HashMap<String,Object> f = new HashMap<String,Object>();
	        	
	       
	        	f.put("key", r.getKey().getString());
	        	f.put("email", email);
	        	f.put("sequence", sv);
	        	f.put("async", "true");
	      
	        	batch.add(f);
	        	batch_cnt++;
	        
	        	if (batch_cnt >= batch_size) {
	        		try {
	            		HashMap<String,SequenceValue> job2seq = new HashMap<String,SequenceValue>();
	        			String[] jobs = runBatch(exec, batch, job2seq);
	        			waitForBatchCompletion(exec, jobs, inData[0].getRowCount());	// entire batch must finish (EBI requirement)
	        			grok_batch(jobs, exec, c1, job2seq);
	        			batch_cnt = 0;
	        			batch.clear();
	        			job2seq.clear();
	        		} catch (Exception e) {
	        			e.printStackTrace();
	        			System.err.println(e.getMessage());
	        			throw e;
	        		}
	        	}
	        }
	        
	        // run any leftovers (ie. not cached entries in the last batch)
	        if (batch.size() > 0) {
	    		HashMap<String,SequenceValue> job2seq = new HashMap<String,SequenceValue>();
	        	String[] jobs = runBatch(exec, batch, job2seq);
    			waitForBatchCompletion(exec, jobs, inData[0].getRowCount());	// entire batch must finish (EBI requirement)
	        	grok_batch(jobs, exec, c1, job2seq);
	        }
	        
    	} catch (Exception e) {
    		logger.warn(e.getMessage());
    		e.printStackTrace();
    	} finally {
    		if (m_cache != null) {
    			m_cache.shutdown();
    			m_cache = null;
    		}
    	}
    	
        // once we are done, we close the container and return its table
        return new BufferedDataTable[]{ c1.close() };
    }
    
    private TrackCreator getTrackCreator() {
		return new TrackCreator() {

			@Override
			public Track createTrack(String name)
					throws InvalidSettingsException {
				return new Track(name, new PhobiusRegionsAnnotation(), CoordinateSystem.OFFSET_FROM_START);
			}
			
		};
	}
   
    protected void grok_batch(String[] jobs, 
    		ExecutionContext exec, MyDataContainer c1, Map<String,SequenceValue> job2seq) throws Exception {

		int idx = 0;
		for (String jobId : jobs) {
        	String result= getJobResult(jobId);
        	
        	// fill in first output port
        	if (jobId.length() > 0 && result.length() > 0) {
        		SequenceValue sv = job2seq.get(jobId);
        		if (sv == null) 
        			throw new InvalidSettingsException("No sequence for jobId: "+jobId);
        		
        		grok_cells(jobId, result, sv, c1);
        		
        		// place in phobius cache for future reference...
        		if (m_cache != null) {
        			CacheablePhobiusRecord cpr = new CacheablePhobiusRecord(sv.getStringValue(), jobId, result);
        			m_cache.put(cpr.getKey(), cpr.toString());
        		}
        	}
        	
        	// check if the execution monitor was canceled
            exec.checkCanceled();
            idx++;
		}
    }
    
    protected byte[] getResult(String jId, String ident) throws Exception {
    	for (int retry=0; retry<MAX_RETRIES; retry++) {
    		try {
    			byte[] ret = m_phobius.getResult(jId, ident, null);
    			return ret;
    		} catch (Exception e) {
    			int delay = 500 + (200 * retry);
    			logger.warn("Failed to get result, will retry in "+delay+" seconds");
    			Thread.sleep(delay * 1000);
    		}
    	}
    	
    	throw new Exception("Unable to get result for "+jId+"... aborting.");
    }
    
    public String getJobResult(String jobId) throws Exception {
    	WsResultTypes results = m_phobius.getResultTypes(jobId);
    	//System.err.println("Found "+results.length+" files for "+jobId);
    	for (WsResultType file : results.getType()) {
    		if (file.getIdentifier().equals("out")) {
    			byte[] ret = getResult(jobId, file.getIdentifier());
    			if (ret == null) {
    				logger.warn("Could not get results for "+jobId+": assuming nothing to report!");
    				return "";
    			}
    			return new String(ret);
    		} /* else {
    			System.err.println("WARNING: Unused result "+i+" filetype: "+file.getIdentifier());
    		} */
    	}
    	return "";
    }
    
    @SuppressWarnings("unused")
	protected void grok_cells(String jobId, String result, SequenceValue sv, MyDataContainer c) throws Exception {
    	DataCell[] cells = new DataCell[8];		// must match make_output_spec()
    	
    	cells[0] = new StringCell(jobId);
		cells[1] = new StringCell(result);
    	String[] lines = result.split("\n");
    	Pattern p = Pattern.compile("\\s*(FT)\\s*(\\w+)\\s*(\\d+)\\s*(\\d+)\\s*(.*)");
    	int matched_cnt = 0;
    	int n_signals = 0;
    	int n_tm = 0;
    	int n_dom = 0;
    	int n_cyto = 0;
    	int n_non_cyto = 0;
    	for (String l : lines) {
    		Matcher m = p.matcher(l);
			//logger.info(l);
    		if (m.matches()) {
    			String entry_type = m.group(1).toUpperCase();
    			String type       = m.group(2).toUpperCase();
    			String start_pos  = m.group(3).toUpperCase();
    			String end_pos    = m.group(4).toUpperCase();
    			String descr      = m.group(5).toUpperCase();
    			matched_cnt++;
    			if (type.startsWith("SIGNAL")) {
    				n_signals++;
    			} else if (type.startsWith("DOM")) {
    				n_dom++;
    			} else if (type.startsWith("TRANS")) {
    				n_tm++;
    			}
    			
    			if (descr.startsWith("CYTO")) {
    				n_cyto++;
    			} else if (descr.startsWith("NON CYTO")) {
    				n_non_cyto++;
    			}
    			
    			Track t = sv.getTrackByName(Track.PHOBIUS_TRACK, getTrackCreator());
    			RegionsAnnotation ra = (RegionsAnnotation) t.getAnnotation();
    			ra.addRegion(new Region(Integer.parseInt(start_pos), Integer.parseInt(end_pos), type + " " + descr));
    		}
    	}
    	cells[2] = new IntCell(n_signals);
    	cells[3] = new IntCell(n_tm);
    	cells[4] = new IntCell(n_dom);
    	cells[5] = new IntCell(n_cyto);
    	cells[6] = new IntCell(n_non_cyto);
    	cells[7] = new SequenceCell(sv);
    	
    	if (matched_cnt < 1) {
    		logger.warn("Did not match any records from job: "+jobId);
    	}
    	
    	c.addRow(cells);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     *  Called when each job completes, this routine is responsible for updating the progress bar
     */
    protected void updateProgress(ExecutionContext exec, int n_rows) {
    	 // and update node progress "traffic light"
        exec.setProgress(((double) m_done_rows) / n_rows, "Searched " + m_done_rows);
    }
    
    /**
     * Waits for the entire batch to complete. Since the batch has just been submitted, we wait
     * for at least 60s before checking the first job for completion
     * 
     * @param jobs
     * @throws Exception
     */
    protected void waitForBatchCompletion(ExecutionContext exec, String[] jobs, int n_rows) throws Exception {
    	int to_go = jobs.length;		// assume none have completed
    	
    	for (int i=0; i<12; i++) {
    		Thread.sleep(5 * 1000);     // sleep for five seconds and then check for cancel
    		exec.checkCanceled();
    	}
    	while (to_go > 0) {
    		for (int retry=0; retry<MAX_RETRIES; retry++) {
    			try {
    	    		waitForCompletion(exec, jobs[jobs.length - to_go]);
    	    		break;	// exit loop normally
    			} catch (Exception ev) {
    				logger.warn("Failed to wait for phobius job: "+ev.getMessage());
    				// retry
    			}
    		}
    		m_done_rows++;
    		logger.info("Job completed: "+jobs[jobs.length - to_go]);
    		exec.checkCanceled();
    		updateProgress(exec, n_rows);
    		to_go--;
    	}
    	logger.info("Batch completed.");
    }
    
    protected String checkStatus(String jobId) throws Exception {
    	for (int retry=0; retry<MAX_RETRIES; retry++) {
    		try {
	    		String status = m_phobius.getStatus(jobId);
	    		return status;
    		} catch (Exception ev) {
    			logger.warn("Failed to get status: "+ev.getMessage());
    			ev.printStackTrace();
    			int delay = 500+(retry*200);
    			logger.warn("Retrying in "+delay+" seconds");
        		Thread.sleep(delay*1000);
    		}
    	}
    	
    	throw new Exception("Failed to get job status after "+MAX_RETRIES+" attempts. Aborting...");
    }
    
    @SuppressWarnings("unused")
	protected void waitForCompletion(ExecutionContext exec, String jobId) throws Exception {
    	if (jobId.length() > 0) {
    		int check_period = 20 * 1000; // every 10s
    		String status = "PENDING";
    		while (status.equals("PENDING") || status.equals("RUNNING")) {
    			try {
    				logger.info("Waiting for "+jobId);
    				status = checkStatus(jobId);
    				if (status.equals("RUNNING") || status.equals("PENDING")) {
    					logger.info(jobId + " " + status + ", sleeping for "+check_period+ " milliseconds");
    					
    					// check ten times each check_period to see if the user pressed cancel
    					for (int i=0; i<10; i++) {
    						Thread.sleep(check_period / 10);
    						exec.checkCanceled();
    					}
    					
    					// each time job is still going, we double check_period to reduce likelihood of overloading EBI
    					check_period *= 2;
    					if (check_period > 200000) {
    						check_period = 200000;
    					}
    				}
    			} catch (IOException e) {
    				e.printStackTrace();
    				String msg = e.getMessage();
    				
    				throw new Exception("Cannot connect with Phobius (EBI)... aborting "+e);
    			}
    		}
    	} else {
    		throw new Exception("Bogus EBI job id... aborting!");
    	}
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return make_output_spec(inSpecs);
    }

    private DataTableSpec[] make_output_spec(DataTableSpec[] inSpecs) throws InvalidSettingsException {
    	DataColumnSpec[] cols = new DataColumnSpec[8];
        cols[0] = new DataColumnSpecCreator("JobID (EBI)", StringCell.TYPE).createSpec();
        cols[1] = new DataColumnSpecCreator("EBI Results (raw)", StringCell.TYPE).createSpec();
        cols[2] = new DataColumnSpecCreator("Count(Predicted Signal Peptides)", IntCell.TYPE).createSpec();
        cols[3] = new DataColumnSpecCreator("Count(Predicted Transmembrane Helices)", IntCell.TYPE).createSpec();
        cols[4] = new DataColumnSpecCreator("Count(Predicted Domain)", IntCell.TYPE).createSpec();
        cols[5] = new DataColumnSpecCreator("Count(Predicted Cytoplasmic Regions)", IntCell.TYPE).createSpec();
        cols[6] = new DataColumnSpecCreator("Count(Predicted non-Cytoplasmic Regions)", IntCell.TYPE).createSpec();
       
        DataColumnSpecCreator dcsc = new DataColumnSpecCreator("Sequence (incl. Phobius track)", SequenceCell.TYPE);
        
        // we must keep the list of tracks as a property associated with the column. We must do this
		// so that annotation dialogs can correctly display the tracks for the user
        int                index = inSpecs[0].findColumnIndex(m_seq_col.getStringValue());
        if (index >= 0) {		// column configured yet?
	        DataColumnProperties isp = inSpecs[0].getColumnSpec(index).getProperties();
			TrackColumnPropertiesCreator tcpc = new TrackColumnPropertiesCreator(isp,
											new Track(Track.PHOBIUS_TRACK, getTrackCreator())
										);
			dcsc.setProperties(tcpc.getProperties());
        }
		cols[7] = dcsc.createSpec();
		
        return new DataTableSpec[] { new DataTableSpec(cols) };
	}

	protected String getJob(String email, String key, InputParameters ip) throws Exception {
    	for (int retry = 0; retry<MAX_RETRIES; retry++) {
    		try {
    			String jobId = m_phobius.run(email, key, ip);
    			return jobId;
    		} catch (Exception e) {
    			int delay = 500 + (200*retry);
    			logger.warn("Unable to submit job, delaying for "+delay+" seconds.");
    			Thread.sleep(delay * 1000);
    		}
    	}
    	
    	throw new Exception("Unable to submit job, aborting after "+MAX_RETRIES+" attempts.");
    }
    
    /**
     * Submits a batch of jobs to EBI and returns the EBI-assigned job-id's to the caller. Returns probably before the jobs complete.
     * @param ip
     * @param d
     * @return
     */
    @SuppressWarnings("rawtypes")
	protected String[] runBatch(ExecutionContext exec, List<HashMap> batch, HashMap<String,SequenceValue> job2seq) throws Exception {
    	String[] jobs = new String[batch.size()];
    	int i = 0;
		ObjectFactory of = new ObjectFactory();

    	for (HashMap h : batch) {
    		exec.checkCanceled(); // stop submitting once cancel chosen by user
    		InputParameters ip = of.createInputParameters();
    		SequenceValue sv = (SequenceValue) h.get("sequence");
    		ip.setSequence(of.createInputParametersSequence(sv.getStringValue()));
    		ip.setFormat(of.createInputParametersFormat("long"));
    			
    		jobs[i] = getJob(h.get("email").toString(),
    				           h.get("key").toString(), ip);
    		
    		job2seq.put(jobs[i++], sv);
    		
    		logger.info("Submitted Phobius job for row: " + h.get("key"));
    	}
    	return jobs;
    }
   
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

    	m_email.saveSettingsTo(settings);
    	m_seq_col.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	m_email.loadSettingsFrom(settings);
    	m_seq_col.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	m_email.validateSettings(settings);
    	m_seq_col.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
        // TODO load internal data. 
        // Everything handed to output ports is loaded automatically (data
        // returned by the execute method, models loaded in loadModelContent,
        // and user settings set through loadSettingsFrom - is all taken care 
        // of). Load here only the other internals that need to be restored
        // (e.g. data used by the views).

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
        // TODO save internal models. 
        // Everything written to output ports is saved automatically (data
        // returned by the execute method, models saved in the saveModelContent,
        // and user settings saved through saveSettingsTo - is all taken care 
        // of). Save here only the other internals that need to be preserved
        // (e.g. data used by the views).

    }

}

