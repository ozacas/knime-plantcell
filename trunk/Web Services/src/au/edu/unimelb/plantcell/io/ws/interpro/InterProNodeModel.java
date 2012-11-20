package au.edu.unimelb.plantcell.io.ws.interpro;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.osgi.framework.Bundle;

import uk.ac.ebi.jdispatcher.soap.interpro.ArrayOfString;
import uk.ac.ebi.jdispatcher.soap.interpro.InputParameters;
import uk.ac.ebi.jdispatcher.soap.interpro.JDispatcherService;
import uk.ac.ebi.jdispatcher.soap.interpro.JDispatcherService_Service;
import uk.ac.ebi.jdispatcher.soap.interpro.ObjectFactory;
import au.edu.unimelb.plantcell.core.Cache;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.ProteinSequenceRowIterator;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.cells.CoordinateSystem;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;
import au.edu.unimelb.plantcell.io.ws.interpro.results.EBIInterProScanResults;
import au.edu.unimelb.plantcell.io.ws.interpro.results.TClassification;
import au.edu.unimelb.plantcell.io.ws.interpro.results.TInterPro;
import au.edu.unimelb.plantcell.io.ws.interpro.results.TMatch;
import au.edu.unimelb.plantcell.io.ws.interpro.results.TProtein;
import au.edu.unimelb.plantcore.core.regions.InterProRegion;
import au.edu.unimelb.plantcore.core.regions.InterProRegionsAnnotation;

/**
 * This is the model implementation of InterProScan.
 * Accesses the EBI webservice: interproscan with the user-specified settings
 *
 * @author Andrew Cassin
 */
public class InterProNodeModel extends NodeModel {
	/**
	 * Sequences shorter than 80aa are unlikely to match anything in InterProScan, so
	 * we skip them to avoid upsetting EBI with useless jobs
	 */
	public static final int MIN_LIKELY_INTERPROSCAN_DB = 80;
	
	/**
	 * maximum cached objects for interpro
	 */
	private static final int MAX_INTERPRO_CACHE_ELEMENTS = 100 * 1000;


    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("InterPro");
    
    static final String CFGKEY_EMAIL    = "email-address";
    static final String CFGKEY_SEQ      = "sequences-from";
    static final String CFGKEY_USE_CRC  = "use-crc?";
    static final String CFGKEY_USE_APPL = "algorithms-to-use";
  
    private static final String DEFAULT_EMAIL    = "who@what.ever.some.where";
    private static final String DEFAULT_SEQ      = "Sequence";
    private static final boolean DEFAULT_USE_CRC = true;
    private static final String[] DEFAULT_USE_APPL = new String[] { "HMMPfam", "SignalP", "BlastProDom", "patternScan", "HMMSmart" };

    /**
     * The number of seconds to wait if a running job has not yet completed. 
     * Scales linearly with the number of retries. Must be greater than zero
     */
	private static final int MANDATORY_DELAY = 20;


    
    // configure-dialog state which must be persistent
    private final SettingsModelString m_email = new SettingsModelString(CFGKEY_EMAIL, DEFAULT_EMAIL);
    private final SettingsModelString m_seq   = new SettingsModelString(CFGKEY_SEQ, DEFAULT_SEQ);
    private final SettingsModelBoolean m_crc  = new SettingsModelBoolean(CFGKEY_USE_CRC, DEFAULT_USE_CRC);
    private final SettingsModelStringArray m_vec = new SettingsModelStringArray(CFGKEY_USE_APPL, DEFAULT_USE_APPL);
    private int m_seq_idx = -1;
	
    /**
     * Constructor for the node model.
     */
    protected InterProNodeModel() {
            super(1, 3);
    }
    
    public JDispatcherService getClientProxy() {
		 // NB: need to use the local WSDL copy rather than go online for it... so...
		 try {
			 Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.io.ws");
			 URL u = FileLocator.find(bundle, new Path("/wsdl/interpro.wsdl"), null);
			 
			 // must not call default constructor for local WSDL... so...
			 JDispatcherService_Service cli = new JDispatcherService_Service(u,
					 new QName("http://soap.jdispatcher.ebi.ac.uk", "JDispatcherService"));
			 return cli.getJDispatcherServiceHttpPort();
		 } catch (Exception e) {
			 e.printStackTrace();
			 Logger.getAnonymousLogger().warning("Unable to initialise proxy: "+e.getMessage());
			 return null;
		 }
	}
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    
    	if (m_seq_idx < 0) {
    		throw new Exception("Invalid sequence column... re-configure the node?");
    	}
    	if (m_email.getStringValue().equals(DEFAULT_EMAIL) || m_email.getStringValue().trim().length() < 1) {
    		throw new Exception("You must provide a valid email address to use EBI. Re-configure the node.");
    	}
    	
    	// setup cache
    	Properties p = new Properties();
    	p.setProperty(Cache.CACHE_MAX_SIZE, new Integer(MAX_INTERPRO_CACHE_ELEMENTS).toString());
    	p.setProperty(Cache.CACHE_AUTO_COMMIT, "true");
    	Cache cache = null;
    	try {
			cache = new Cache("interpro_4", p);
    		cache.init();
    		if (cache.isEnabled()) {
    			logger.info("InterPro cache initialised.");
    		} else {
    			logger.warn("Cache disabled (freshness is "+cache.getFreshness()+" days)");
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    		cache = null;
			logger.warn("Unable to initialise cache: "+e.getMessage()+" - cache disabled.");
    	}
    	
    	// traversal over input sequences
    	ProteinSequenceRowIterator it = new ProteinSequenceRowIterator(inData[0].iterator(), m_seq_idx, logger, new SequenceProcessor() {

			@Override
			public SequenceValue process(SequenceValue sv) {
				if (sv == null || sv.getLength() < MIN_LIKELY_INTERPROSCAN_DB) {
					logger.warn("Sequence too short for interpro... skipping "+sv.getID());
					return null;
				}
				
				if (sv.getStringValue().indexOf('-') > 0) {
					logger.warn("Gaps not suitable for InterPro in "+sv.getID());
					return null;
				}
				
				SequenceType  st = sv.getSequenceType();
				if (st.equals(SequenceType.DNA) || st.equals(SequenceType.RNA)) {
					logger.warn("Skipping DNA/RNA sequence "+sv.getID());
					return null;
				}
				// else ok..
				return sv;
			}
    		
    	});
    
    	// creator of objects for webservice
    	JDispatcherService cli = getClientProxy();
		ObjectFactory       of = new ObjectFactory();
    	
    	// create output containers
    	DataTableSpec[]      outputSpec = make_output_spec(inData[0].getSpec(), m_seq_idx);
        MyDataContainer c3 = new MyDataContainer(exec.createDataContainer(outputSpec[0]), "Site");
    	MyDataContainer c2 = new MyDataContainer(exec.createDataContainer(outputSpec[1]), "Hit");
    	MyDataContainer c1 = new MyDataContainer(exec.createDataContainer(outputSpec[2]), "Seq");
        
    	// run the jobs...
        double n_rows = inData[0].getRowCount();
       
        exec.setProgress(0.0);
        HashMap<String,SequenceValue> outstanding_jobs = new HashMap<String,SequenceValue>();
    	
        int done = 0;
        int limit=20;
		while (it.hasNext()) {
			SequenceValue sv = it.nextSequence();
			DataRow r    = it.next();
        	String rowid = r.getKey().getString();
        	String prot  = sv.getStringValue();
        	
			logger.info("Looking for cache entry "+prot);
        	if (cache != null && 
        			cache.contains(prot)) {
        		logger.info("Retrieving cached InterPro data for row "+rowid);
        		String xml = cache.get(prot);
        		
        		// HACK BUG: use of create(String) is probably buggy for non-UTF encoded XML documents but thats not likely for interpro... ;)
    			grokXML(sv, XMLCellFactory.create(xml), c3, c2, c1);
    			done++;
    			logger.info("Loaded cached result for "+prot);
    			if (!it.hasNext() && outstanding_jobs.size() > 0 )
    				; // fallthru to run last batch
    			else
    				continue;
        	} else { // else...
        		outstanding_jobs.put(rowid, sv);
        	}
			done++;
			if (outstanding_jobs.size() < limit && it.hasNext())
				continue;
			
			process_jobs(cli, cache, outstanding_jobs, of, c3, c2, c1);
			outstanding_jobs.clear();
			
			if (it.hasNext()) {
				exec.setProgress(((double)done)/n_rows);
				exec.checkCanceled();
				logger.info("Waiting 30 sec. to be nice to EBI servers");
				Thread.sleep(30 * 1000);
			}
		}
		
		if (outstanding_jobs.size() > 0)
			throw new InvalidSettingsException("PROGRAMMER ERROR: jobs not run!");
    
    	cache.shutdown();
    	return new BufferedDataTable[] {c3.close(), c2.close(), c1.close()};
    }

    private void grokXML(SequenceValue prot, DataCell xml,
			MyDataContainer c_site, MyDataContainer c_prot, MyDataContainer c_raw) throws InvalidSettingsException {
    	DataCell[] cells;
    	SequenceCell sc  = new SequenceCell(prot);
    	InterProRegionsAnnotation ipra = new InterProRegionsAnnotation();
    	Track t = new Track(Track.INTERPRO_TRACK, ipra, CoordinateSystem.OFFSET_FROM_START);
    	sc.addTrack(t);
    	
		// ports 2 and 3
		if (xml instanceof XMLValue) {
			XMLValue  c_xml = (XMLValue) xml;
			try {
			    // setup object mapper using the AppConfig class
			    JAXBContext context = JAXBContext.newInstance(EBIInterProScanResults.class);
			    // parse the XML and return an instance of the AppConfig class
			    EBIInterProScanResults results = (EBIInterProScanResults) context.createUnmarshaller().unmarshal(new StringReader(c_xml.toString()));
			    
			    for (TProtein protein_hit : results.getInterproMatches().getProtein()) {
			    	for (TInterPro intpro : protein_hit.getInterpro()) {
			    		// 1. retrieve classification of proteins (eg. gene ontology) 
			    		for (TClassification c : intpro.getClassification()) {
			    			cells    = new DataCell[8];
			    			cells[0] = new StringCell(prot.getID());
			    			cells[1] = asInterproID(intpro);
			    			cells[2] = new StringCell(intpro.getName());
			    			cells[3] = new StringCell(intpro.getType());
			    			cells[4] = new StringCell(c.getClassType());
			    			cells[5] = new StringCell(c.getId());
			    			cells[6] = new StringCell(c.getCategory());
			    			cells[7] = new StringCell(c.getDescription());
			    			c_prot.addRow(cells);
			    		}
			    		
			    		// 2. matches from various interpro data sources
			    		for (TMatch m : intpro.getMatch()) {
			    			for (TMatch.Location l : m.getLocation()) {
			    				cells    = new DataCell[10];
			    				cells[0] = new StringCell(prot.getID());
			    				cells[1] = new StringCell(m.getDbname());
			    				cells[2] = new StringCell(m.getId());
			    				cells[3] = new StringCell(l.getEvidence());
			    				cells[4] = new StringCell(l.getStatus());
			    				cells[5] = asDoubleCell(l.getScore());
			    				cells[6] = asIntCell(l.getStart());
			    				cells[7] = asIntCell(l.getEnd());
			    				cells[8] = asNameCell(m.getName());
			    				cells[9] = asInterproID(intpro);
			    				
			    				InterProRegion ipr = new InterProRegion(m.getDbname(), m.getId(), l.getStart(), l.getEnd());
			    				ipr.setInterProID(cells[9].toString());
			    				try {
			    					ipr.setScore(Double.parseDouble(l.getScore()));
			    				} catch (NumberFormatException nfe) {
			    					ipr.setScore(Double.NaN);		// denotes "missing" score
			    				}
			    				ipr.setStatus(l.getStatus());
			    				ipr.setEvidence(l.getEvidence());
			    				ipr.setOffset(1);
			    				ipra.addRegion(ipr);
			    				c_site.addRow(cells);
			    			}
			    		}
			    	}
			    }
			} catch(JAXBException e) {
				e.printStackTrace();
			    logger.warn("Unable to process results (continuing anyway) for "+prot.getID());
			}
		}
		
		// raw output port (topmost)
    	cells            = new DataCell[2];
		cells[0]         = sc;
		cells[1] = (xml == null) ? DataType.getMissingCell() : xml;
		c_raw.addRow(cells);
	
	}

    private DataCell asNameCell(String name) {
		if (name == null || name.trim().length() < 1 || name.toLowerCase().equals("no description")) {
			return DataType.getMissingCell();
		}
		return new StringCell(name);
	}

	private DataCell asInterproID(TInterPro intpro) {
		if (intpro == null || intpro.getId().length() < 1 || intpro.getId().toLowerCase().equals("noipr"))
			return DataType.getMissingCell();
		return new StringCell(intpro.getId());
	}

	private DataCell asDoubleCell(String val) {
    	try {
    		if (val.equals("?") || val.equals("NA") || val.trim().length() < 1)
    			return DataType.getMissingCell();
    		double d = Double.parseDouble(val);
    		return new DoubleCell(d);
    	} catch (NumberFormatException nfe) {
    		logger.info("Unable to convert: "+val+" to a number, assuming missing value.");
    		return DataType.getMissingCell();
    	}
    }

    private DataCell asIntCell(int val) {
    	return new IntCell(val);
    }
    
	private void process_jobs(JDispatcherService cli, Cache ip_cache,
			HashMap<String, SequenceValue> batch, ObjectFactory of,
			final MyDataContainer c_raw, final MyDataContainer c_seq,
			final MyDataContainer c_site) throws Exception {
    	Map<String,SequenceValue>     prots = new HashMap<String,SequenceValue>();
    	Map<String,String> outstanding_jobs = new HashMap<String,String>();
    	
    	for (String key : batch.keySet()) {
    		SequenceValue prot = batch.get(key);
			String job_id  = submit_job_async(cli, m_email.getStringValue(), prot.getStringValue(), key, of);
			outstanding_jobs.put(key, job_id);
			prots.put(job_id, prot);
		}
		wait_for_completion(cli, outstanding_jobs.values());
		
		Collection<String> keys = new HashSet<String>();		
		keys.addAll(outstanding_jobs.values());	// NB: careful to deep copy! we are about to clear the map...
		batch.clear();
		for (String key : keys) {
			DataCell xml = getXMLResult(cli, key);
			SequenceValue  prot = prots.get(key);
			grokXML(prot, xml, c_raw, c_seq, c_site);
			// cache only if grok'ing doesnt throw an Exception
			if (ip_cache != null) {
				ip_cache.put(prot.getStringValue(), xml.toString());
				logger.info("Saved cache result for "+prot);
			}
		}
	}

	private String submit_job_async(JDispatcherService cli, String email_address, String seq, String rkey, ObjectFactory of) throws Exception {
		for (int retry=0; retry < 4; retry++) { 
			try {
				InputParameters job_params = new InputParameters();
				// TODO BUG: why are the results from EBI the same despite m_vec changes?
	    		ArrayOfString aos = new ArrayOfString();
	    		for (String appl : m_vec.getStringArrayValue()) {
	    			aos.getString().add(appl.toLowerCase());
	    		}
	    		job_params.setAppl(of.createInputParametersAppl(aos));
	    		job_params.setSequence(of.createInputParametersSequence(seq));
	    		job_params.setGoterms(of.createInputParametersGoterms(new Boolean(true)));
	    		job_params.setNocrc(of.createInputParametersNocrc(new Boolean(!m_crc.getBooleanValue())));
   	    		
				String jid = cli.run(m_email.getStringValue(), rkey, job_params);
				logger.info("Submitted job for row "+rkey+", got job-id: "+jid);
				return jid;
			} catch (SOAPFaultException soape) {
				throw soape;
			} catch (Exception e) {
				int delay = (retry+1)*500;	// seconds
				logger.warn("Problem when submitting job: "+e.getMessage()+ "... retrying in "+delay+" seconds");
				Thread.sleep(delay*1000);
			}
		}
		throw new FailedJobException("Cannot submit job after four attempts... giving up on "+rkey+"!");
	}

	private void wait_for_completion(JDispatcherService cli, Collection<String> keySet) 
    				throws ServiceException, InterruptedException, FailedJobException, IOException {
    	boolean wait = true;	// mandatory wait for first job in batch
    	for (String s : keySet) {
    		for (int idx=0; idx < 1000; idx++) {
    			if (wait) {
    				int delay = (MANDATORY_DELAY+(idx*MANDATORY_DELAY));		// seconds
    				logger.info("Pausing to meet EBI requirements: "+delay+" seconds.");
    				Thread.sleep(delay*1000);
    			}
        		String status = cli.getStatus(s).toLowerCase();
        		// completed or finished?
        		if (status.startsWith("complete") || status.startsWith("finish")) {
        			wait = false;	// check status without waiting for rest of batch
        			break;		// wait for next job
        		} else if (status.startsWith("fail") || status.startsWith("error")) {			// something go wrong?
        			throw new FailedJobException("Job "+s+" has failed at EBI. Aborting run.");
        		} else {
        			// incomplete so just go around again...
        			wait = true;
        		}
    		}
    	}
    	
    	// once we get here the entire batch is done
	}
	
	public DataCell getXMLResult(JDispatcherService cli, String jobID) throws Exception {
		byte[] ret = null;
		for (int retry=0; retry<4; retry++) {
			try {
				ret = cli.getResult(jobID, "xml", null);
				if (ret != null)
					break;
			} catch (Exception e) {
				logger.warn("Unable to fetch result for "+jobID+"!");
				int delay = 300+((retry+1)*200);
				logger.info("Delaying for "+delay+" seconds before retrying.");
				Thread.sleep(delay*1000);	
			}
		}
		if (ret == null) {
			return DataType.getMissingCell();
		}
		ByteArrayInputStream bis = new ByteArrayInputStream(ret);
		DataCell c = XMLCellFactory.create(bis);
		logger.info("Got XML result for job "+jobID);
		bis.close();
		return c;
	}
	

	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
      
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    	m_seq_idx = inSpecs[0].findColumnIndex(m_seq.getStringValue());
        return make_output_spec(inSpecs[0], m_seq_idx);
    }

    private DataTableSpec[] make_output_spec(DataTableSpec inSpec, int seq_idx) {
		// columns for this node are dependant on what the user wants to see, so...
    	DataColumnSpec[] cols = new DataColumnSpec[2];
    	DataColumnSpecCreator my_annot_spec = new DataColumnSpecCreator("InterPro Annotated Sequence", SequenceCell.TYPE);
    	
		// we must keep the list of tracks as a property associated with the column. We must do this
		// so that annotation dialogs can correctly display the tracks for the user
    	DataColumnProperties isp = new DataColumnProperties();
    	if (seq_idx >= 0) {
    		isp = inSpec.getColumnSpec(seq_idx).getProperties();
    	}
    	TrackColumnPropertiesCreator tcpc = new TrackColumnPropertiesCreator(isp,
				new Track(Track.INTERPRO_TRACK, new InterProRegionsAnnotation(), CoordinateSystem.OFFSET_FROM_START)
);
		my_annot_spec.setProperties(tcpc.getProperties());
		cols[0] = my_annot_spec.createSpec();
    	cols[1] = new DataColumnSpecCreator("Raw XML result", XMLCell.TYPE).createSpec();
    	
    	DataColumnSpec[] c2 = new DataColumnSpec[8];
    	c2[0] = new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
    	c2[1] = new DataColumnSpecCreator("InterPro ID", StringCell.TYPE).createSpec();
    	c2[2] = new DataColumnSpecCreator("InterPro Name", StringCell.TYPE).createSpec();
    	c2[3] = new DataColumnSpecCreator("InterPro Type", StringCell.TYPE).createSpec();
    	c2[4] = new DataColumnSpecCreator("Classification ID", StringCell.TYPE).createSpec();
    	c2[5] = new DataColumnSpecCreator("Category", StringCell.TYPE).createSpec();
    	c2[6] = new DataColumnSpecCreator("Field", StringCell.TYPE).createSpec();
    	c2[7] = new DataColumnSpecCreator("Value", StringCell.TYPE).createSpec();
    	
    	DataColumnSpec[] c3 = new DataColumnSpec[10];
    	c3[0] = new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
    	c3[1] = new DataColumnSpecCreator("Database", StringCell.TYPE).createSpec();
    	c3[2] = new DataColumnSpecCreator("Database ID", StringCell.TYPE).createSpec();
    	c3[3] = new DataColumnSpecCreator("Evidence (algorithm)", StringCell.TYPE).createSpec();
    	c3[4] = new DataColumnSpecCreator("Status", StringCell.TYPE).createSpec();
    	c3[5] = new DataColumnSpecCreator("Score", DoubleCell.TYPE).createSpec();
    	c3[6] = new DataColumnSpecCreator("Start", IntCell.TYPE).createSpec();
    	c3[7] = new DataColumnSpecCreator("End", IntCell.TYPE).createSpec();
    	c3[8] = new DataColumnSpecCreator("Name", StringCell.TYPE).createSpec();
    	c3[9] = new DataColumnSpecCreator("InterPro ID", StringCell.TYPE).createSpec();
	
    	return new DataTableSpec[] { new DataTableSpec(c3),
    			new DataTableSpec(c2),
    			new DataTableSpec(cols)
    			 };
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_email.saveSettingsTo(settings);
    	m_seq.saveSettingsTo(settings);
    	m_crc.saveSettingsTo(settings);
    	m_vec.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_email.loadSettingsFrom(settings);
    	m_seq.loadSettingsFrom(settings);
    	m_crc.loadSettingsFrom(settings);
    	m_vec.loadSettingsFrom(settings);
    	
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_email.validateSettings(settings);
    	m_seq.validateSettings(settings);
    	m_crc.validateSettings(settings);
    	m_vec.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
   

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
  

    }

}

