package au.edu.unimelb.plantcell.io.ws.quickgo;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
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
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * This is the model implementation of GoaSource.
 * Provides an interface to GOA (Gene Ontology Annotation) websites (esp. EBI) to KNIME
 *
 * @author Andrew Cassin
 */
public class QuickGOSourceNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("QuickGO");
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_URL           = "url";
	static final String CFGKEY_TYPE          = "type";
	static final String CFGKEY_TERMINFO_COL  = "terminfo-column";
	static final String CFGKEY_ADVQUERY      = "adv-query";
	static final String CFGKEY_FIELD_DB      = "field-db";
	static final String CFGKEY_FIELD_TERM    = "field-term";
	static final String CFGKEY_FIELD_ANCESTOR="field-ancestor";
	static final String CFGKEY_FIELD_EVIDENCE="field-evidence";
	static final String CFGKEY_FIELD_SOURCE  = "field-source";
	static final String CFGKEY_FIELD_REF     = "field-ref";
	static final String CFGKEY_FIELD_WITH    = "field-with";
	static final String CFGKEY_FIELD_TAX     = "field-taxonomy";
	static final String CFGKEY_FIELD_PROTEIN = "field-protein";
	static final String CFGKEY_MAX_ENTRIES   = "max-entries";
	
	final int CONSTANT_DELAY_EACH_TERM = 5; // seconds
	final int CONSTANT_DELAY_EVERY_50_TERMS = 30; // seconds
	
    /** initial default count value. */
    private static final String DEFAULT_URL = "http://www.ebi.ac.uk/QuickGO/";
    private static final String DEFAULT_TYPE= "Term Information";
    private static final String DEFAULT_TERMINFO_COL= "GO Term";
    private static final String DEFAULT_ADVQUERY = "";
    private static final String DEFAULT_FIELD_DB = "UniGene";
    //private static final String DEFAULT_FIELD_TERM="";
    private static final String DEFAULT_FIELD_ANCESTOR="";
    private static final String DEFAULT_FIELD_EVIDENCE="All";
    private static final String DEFAULT_FIELD_SOURCE  ="";
    private static final String DEFAULT_FIELD_REF     ="";
    private static final String DEFAULT_FIELD_WITH    ="";
    private static final String DEFAULT_FIELD_TAXONOMY="";
    private static final String DEFAULT_FIELD_PROTEIN ="";
    private static final int    DEFAULT_MAX_ENTRIES = 1000;

    /* node values to be saved with the workflow */
    private final SettingsModelString m_url                 = (SettingsModelString) make(CFGKEY_URL);
    private final SettingsModelString m_type                = (SettingsModelString) make(CFGKEY_TYPE);
    private final SettingsModelString m_terminfo_col        = (SettingsModelString) make(CFGKEY_TERMINFO_COL);
    private final SettingsModelString m_advquery            = (SettingsModelString) make(CFGKEY_ADVQUERY);
    
    private final SettingsModelString m_field_db            = (SettingsModelString) make(CFGKEY_FIELD_DB);
    private final SettingsModelString m_field_ancestor      = (SettingsModelString) make(CFGKEY_FIELD_ANCESTOR);
    private final SettingsModelString m_field_evidence      = (SettingsModelString) make(CFGKEY_FIELD_EVIDENCE);
    private final SettingsModelString m_field_source        = (SettingsModelString) make(CFGKEY_FIELD_SOURCE);
    private final SettingsModelString m_field_ref           = (SettingsModelString) make(CFGKEY_FIELD_REF);
    private final SettingsModelString m_field_with          = (SettingsModelString) make(CFGKEY_FIELD_WITH);
    private final SettingsModelString m_field_tax           = (SettingsModelString) make(CFGKEY_FIELD_TAX);
    private final SettingsModelString m_field_protein       = (SettingsModelString) make(CFGKEY_FIELD_PROTEIN);
    private final SettingsModelIntegerBounded m_max_entries = (SettingsModelIntegerBounded) make(CFGKEY_MAX_ENTRIES); // the maximum number of annotation entries for a single request. The node will print a warning if this number is reached for a single query
 
    /**
     * Constructor for the node model.
     */
    protected QuickGOSourceNodeModel() {
        super(1, 1);
    }
    
    /**
     * NB: this method must set the enabled parameter consistently for the initial display of the dialog to be correct
     * @param cfgkey
     * @return
     */
    public static SettingsModel make(String cfgkey) {
    	/*
    	 *  Given DEFAULT_TYPE is Term Information, we must set all the enabled flags for the following
    	 *  fields to false: advquery, db, evidence, source, ancestor, ref, with, taxonomy, protein
    	 *  
    	 *  This must be kept the same as GoaSourceNodeDialog::set_controls()
    	 */
    	if (cfgkey.equals(CFGKEY_URL)) {
    		return new SettingsModelString(CFGKEY_URL, DEFAULT_URL);
    	} else if (cfgkey.equals(CFGKEY_TYPE)) {
    		return new SettingsModelString(CFGKEY_TYPE, DEFAULT_TYPE);
    	} else if (cfgkey.equals(CFGKEY_TERMINFO_COL)) {
    		return new SettingsModelString(CFGKEY_TERMINFO_COL, DEFAULT_TERMINFO_COL);
    	} else if (cfgkey.equals(CFGKEY_ADVQUERY)) {
    		SettingsModelString sms = new SettingsModelString(CFGKEY_ADVQUERY, DEFAULT_ADVQUERY);
    		sms.setEnabled(false);
    		return sms;
    	} else if (cfgkey.equals(CFGKEY_FIELD_DB)) {
    		SettingsModelString sms = new SettingsModelString(CFGKEY_FIELD_DB, DEFAULT_FIELD_DB);
    		sms.setEnabled(false);
    		return sms;
    	} else if (cfgkey.equals(CFGKEY_FIELD_ANCESTOR)) {
    		SettingsModelString sms = new SettingsModelString(CFGKEY_FIELD_ANCESTOR, DEFAULT_FIELD_ANCESTOR);
    		sms.setEnabled(false);
    		return sms;
    	} else if (cfgkey.equals(CFGKEY_FIELD_EVIDENCE)) {
    		SettingsModelString sms = new SettingsModelString(CFGKEY_FIELD_EVIDENCE, DEFAULT_FIELD_EVIDENCE);
    		sms.setEnabled(false);
    		return sms;
    	} else if (cfgkey.equals(CFGKEY_FIELD_SOURCE)) {
    		SettingsModelString sms = new SettingsModelString(CFGKEY_FIELD_SOURCE, DEFAULT_FIELD_SOURCE);
    		sms.setEnabled(false);
    		return sms;
    	} else if (cfgkey.equals(CFGKEY_FIELD_REF)) {
    		SettingsModelString sms = new SettingsModelString(CFGKEY_FIELD_REF, DEFAULT_FIELD_REF);
    		sms.setEnabled(false);
    		return sms;
    	} else if (cfgkey.equals(CFGKEY_FIELD_WITH)) {
    		SettingsModelString sms = new SettingsModelString(CFGKEY_FIELD_WITH, DEFAULT_FIELD_WITH);
    		sms.setEnabled(false);
    		return sms;
    	} else if (cfgkey.equals(CFGKEY_FIELD_TAX)) {
    		SettingsModelString sms = new SettingsModelString(CFGKEY_FIELD_TAX, DEFAULT_FIELD_TAXONOMY);
    		sms.setEnabled(false);
    		return sms;
    	} else if (cfgkey.equals(CFGKEY_FIELD_PROTEIN)) {
    		SettingsModelString sms = new SettingsModelString(CFGKEY_FIELD_PROTEIN, DEFAULT_FIELD_PROTEIN);
    		sms.setEnabled(false);
    		return sms;
    	} else if (cfgkey.equals(CFGKEY_MAX_ENTRIES)) {
    		SettingsModelIntegerBounded me = new SettingsModelIntegerBounded(CFGKEY_MAX_ENTRIES, DEFAULT_MAX_ENTRIES, 10, 10000);
    		return me;
    	} 
    
    	return null;    
    }
    
    public static SettingsModelString make_string(String cfgkey) {
    	return (SettingsModelString) make(cfgkey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        String url = m_url.getStringValue();
        int go_term_col = inData[0].getDataTableSpec().findColumnIndex(m_terminfo_col.getStringValue());
        if (go_term_col < 0) {
        	throw new Exception("Cannot find Gene Ontology (GO) term column!");
        }
        
        logger.info("Loading data from GOA... via URL "+url);
        String url_params = "";
        
        boolean is_term_search = m_type.getStringValue().startsWith("Term");
        DataTableSpec outputSpec = make_output_spec(inData[0].getDataTableSpec(), is_term_search);
        if (!is_term_search) {
        	url_params = make_url_parameters();
        }
       
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        
        int cnt = 0;
        int rk_id = 1;
        int errors = 0;
        double so_far = 0.0;
        double p_size = inData[0].getRowCount();
        RowIterator it = inData[0].iterator();
        int no_hit_cnt = 0;
        int max_retries = 5;
        
        while (it.hasNext()) {
        	DataRow r = it.next();
        	String go_term = cleanup_id(r.getCell(go_term_col).toString());

        	int new_rkid;
        	for (int i=0; i<max_retries; i++) {
        		try {
			        if (isGOTermSearch()) {
			        	new_rkid = process_term(url, go_term, rk_id, container, r);
			        } else {
			        	new_rkid = process_annotation(url, go_term, rk_id, container, url_params, r);
			        }
			        if (new_rkid > rk_id ) {
		        		cnt++;
		        		rk_id = new_rkid;
		        	} else {
		        		errors++;
		        	}
			        break;		// dont retry "successful" queries ;-)
        		} catch (MalformedURLException mfue) {
        			throw mfue;		// not retry-able
        		} catch (Exception e) {
        			logger.warn("Unable to fetch from QuickGO: "+e.getMessage());
        			int delay = (i + 1) * 200;
        			logger.info("Retrying in "+delay+" seconds");
        			Thread.sleep(delay * 1000);
        		}
        	}
	        
        	// check if the execution monitor was canceled
	        try {
	        	exec.checkCanceled();
	        } catch (CanceledExecutionException ce) {
	        	container.close();
	        	// TODO... avoid leaking other objects
	        	throw ce;
	        }
            so_far += 1.0;
            // and update node progress "traffic light"
            exec.setProgress(so_far / p_size, "Processing term " + go_term);
            
            // delay before next search
            logger.info("Delaying for "+CONSTANT_DELAY_EACH_TERM+" seconds to be nice to server provider");
        	Thread.sleep(CONSTANT_DELAY_EACH_TERM*1000);
        }
     
        
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        if (cnt < 1) {
        	logger.warn("Unable to load any data -- is the QuickGO query correct?");
        } else if (no_hit_cnt > 0) {
            logger.warn(no_hit_cnt + " rows produced no hits. Consider refining your query.");
        }
        logger.info("Loaded Gene Ontology data for " + cnt + " terms.");
        return new BufferedDataTable[]{out};
    }
    
    private DataTableSpec make_output_spec(DataTableSpec inSpec, boolean is_term_search) {
         DataColumnSpec[] allColSpecs = null;

    	 if (is_term_search) {
         	allColSpecs = new DataColumnSpec[6];
 	        allColSpecs[0] = 
 	            new DataColumnSpecCreator("Name", StringCell.TYPE).createSpec();
 	        allColSpecs[1] = 
 	            new DataColumnSpecCreator("Definition", StringCell.TYPE).createSpec();
 	        allColSpecs[2] = 
 	            new DataColumnSpecCreator("Database Cross-References", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
 	        allColSpecs[3] = new DataColumnSpecCreator("Synonyms", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
            allColSpecs[4] = new DataColumnSpecCreator("Raw OBO data", StringCell.TYPE).createSpec();
            allColSpecs[5] = new DataColumnSpecCreator("Is A?", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
    	 } else {
         	allColSpecs = new DataColumnSpec[14];
         	allColSpecs[0] = new DataColumnSpecCreator("Database", StringCell.TYPE).createSpec();
         	allColSpecs[1] = new DataColumnSpecCreator("ID", StringCell.TYPE).createSpec();
         	allColSpecs[2] = new DataColumnSpecCreator("Alt", StringCell.TYPE).createSpec();
         	allColSpecs[3] = new DataColumnSpecCreator("Symbol", StringCell.TYPE).createSpec();
         	allColSpecs[4] = new DataColumnSpecCreator("Taxonomy ID (NCBI)", StringCell.TYPE).createSpec();
         	allColSpecs[5] = new DataColumnSpecCreator("Qualifier", StringCell.TYPE).createSpec();
         	allColSpecs[6] = new DataColumnSpecCreator("GO Term", StringCell.TYPE).createSpec();
         	allColSpecs[7] = new DataColumnSpecCreator("GO Term Name", StringCell.TYPE).createSpec();
         	allColSpecs[8] = new DataColumnSpecCreator("Reference", StringCell.TYPE).createSpec();
         	allColSpecs[9] = new DataColumnSpecCreator("Evidence", StringCell.TYPE).createSpec();
         	allColSpecs[10] = new DataColumnSpecCreator("With", StringCell.TYPE).createSpec();
         	allColSpecs[11] = new DataColumnSpecCreator("Aspect", StringCell.TYPE).createSpec();
         	allColSpecs[12] = new DataColumnSpecCreator("Date", StringCell.TYPE).createSpec();
         	allColSpecs[13] = new DataColumnSpecCreator("From", StringCell.TYPE).createSpec();
         }
         
         return new DataTableSpec(inSpec, new DataTableSpec(allColSpecs));
	}

	protected int process_annotation(String url, String go_term, int rk_id, 
			BufferedDataContainer container, String url_params, DataRow r) throws Exception {
    	
    	// searching for annotations by GO Term identifier (ID=) or accession (protein=) ?
    	String id_type = "id=";
    	if (!isGOTermSearch()) {
    		id_type = "protein=";
    	}
    	
    	// make the final URL and log it to the knime console (at info level)
    	URL u = new URL(url+"GAnnotation?"+id_type+go_term+"&format=tsv&limit="+m_max_entries.getIntValue()+url_params);
    	logger.info(u);
    	
    	try {
    		HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        	InputStream         is = conn.getInputStream();
        	BufferedReader     rdr = new BufferedReader(new InputStreamReader(is));
        	// skip headings, since we know what is expected
        	String line = rdr.readLine();
        	//System.err.println(line);
        	int num_entries = 0;
    		while (( line = rdr.readLine()) != null) {
    			//logger.debug(line);
    			String[] entries = line.split("\t");
    			if (entries.length == 0) 
    				continue;
    			DataCell[] cells = new DataCell[14];
    			assert entries.length == cells.length;
    			for (int i=0; i<cells.length; i++) {
    				cells[i] = new StringCell(entries[i]);
    			}
    			String rk = "Row"+rk_id;
    			num_entries++;
    			rk_id++;
    			container.addRowToTable(new AppendedColumnRow(new RowKey(rk), r, cells));
    		}
    		//System.err.println(num_entries);
    		if (num_entries >= m_max_entries.getIntValue()-1) { // BUG: -1 for the header line???
    			logger.warn("Results for "+go_term+" might be truncated. Probably partial results: consider refining the query.");
    			// FALLTHRU
    		} else if (num_entries > 0) {
    			// FALLTHRU
    		} else if (num_entries == 0) {
    			logger.warn("No hits for "+go_term+" - are you sure the settings are correct?");
    			//no_hit_cnt++;
    		}
    		conn.disconnect();
    		rdr.close();
    		return rk_id;
    	} catch (Exception e) {
    		logger.info("Unable to get data for "+go_term+": "+e.getMessage());
    		throw e;
    	}
    }
    
	protected int process_term(String url, String go_term, int rk_id, BufferedDataContainer container, 
			DataRow r) throws Exception {
    	URL u = new URL(url+"GTerm?id="+go_term+"&format=obo");
    	logger.info("Fetching "+u);
    	
		StringBuffer sb = new StringBuffer(100 * 1024);
		OntologyMap om = new OntologyMap();

    	try {
			HttpURLConnection conn = (HttpURLConnection) u.openConnection();
			java.io.InputStream is = conn.getInputStream();
			BufferedReader rdr = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = rdr.readLine()) != null) {
				om.build(line);
				sb.append(line);
				sb.append('\n');
			}	
			rdr.close();
			conn.disconnect();
			// FALLTHRU
    	} catch (Exception e) {
    		throw e;
    	}
    	
    	// got some data to process (either from cache or from EBI)?
    	if (om != null) {
        	try {
        		DataCell[] cells = new DataCell[6];
        		cells[0] = safe_string(om.getName());
        		cells[1] = safe_string(om.getDefinition());
        		cells[2] = safe_string_list(om.getXrefs());
        		cells[3] = safe_string_list(om.getSynonyms());
        		cells[4] = safe_string(sb.toString());
        		cells[5] = safe_string_list(om.getISArelations());
        		String row_id = "Row"+rk_id++;
        		container.addRowToTable(new JoinedRow(new DefaultRow(row_id, r), new DefaultRow(row_id, cells)));
        	
            	return rk_id;
        	} catch (Exception e) {
        		e.printStackTrace();
        		logger.warn("Failed to process data for row (ignored): "+r.getKey().getString());
        		logger.warn("Message was: "+e.getMessage());
        		// FALLTHRU
        	}
    	}
    	
    	return rk_id;
    }
    private DataCell safe_string_list(List<String> s) {
    	if (s == null || s.size() < 1)
    		return DataType.getMissingCell();
		List<StringCell> ret = new ArrayList<StringCell>();
		for (String t : s) {
			ret.add(new StringCell(t));
		}
		return CollectionCellFactory.createListCell(ret);
	}

	private DataCell safe_string(String s) {
		if (s == null)
			return DataType.getMissingCell();
		return new StringCell(s);
	}

	/** 
	 * Returns true if the user has configured the node to lookup GO terms (GO:000...) or false
	 * otherwise.
	 * 
	 * @return
	 */
	public boolean isGOTermSearch() {
		return m_type.getStringValue().startsWith("Term");
	}
	
	/**
     *  Tests to see if the supplied identifier is valid for the type of search and "cleans it up" as convenience for the user, to avoid
     *  common repeated bad searches using invalid web parameters
     */
    public String cleanup_id(String messy_id) throws Exception {
    	String ret = messy_id.trim();
    	if (isGOTermSearch()) {
    		ret = ret.toUpperCase();
    		if (!messy_id.startsWith("GO:")) {
    			throw new Exception("Invalid GO term: "+ret+" aborting search.");
    		}
    	} else {
    		if (ret.length() < 1 || ret.length() > 100) {
    			throw new Exception("Invalid protein accession "+ret+" aborting search.");
    		}
    	}
    	return ret;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
  
    }

    /**
     * For GAnnotation QuickGO queries, this routine examines the configuration settings and returns the
     * relevant settings as a url-encoded HTTP-ready string
     * @return the encoded parameters, based on current configuration settings
     */
    public String make_url_parameters() {
    	String params = "";
    	String db = m_field_db.getStringValue();
    	if (db.equals("UniProtKB: both Swiss-Prot and TrEMBL")) {
    		// params += ...
    		logger.info("Not constraining GO database: all UniProtKB requested");
    	} else if (db.startsWith("NCBI RefSeq")) {
    		params += "&db=RefSeq_Prot";
    	} else {
    		params += "&db="+db;
    	}
    	
    	// handle evidence filter
    	String ev = m_field_evidence.getStringValue();
    	if (ev.equals("Manual Experimental")) { 
    		params += "&evidence=IMP,IGI,IPI,IDA,IEP,EXP";
    	} else if (ev.equals("Manual All")) {
    		params += "&evidence=IMP,IGI,IPI,IDA,IEP,EXP,ISS,TAS,NAS,ND,IC,RCA";
    	} else if (ev.equals("All")) {
    		params += ""; // default, no need to send to server
    	} else {
    		params += "&evidence="+ev;
    	}
    	
    	// handle source
    	String src = m_field_source.getStringValue();
    	if (src.equals("Any")) {
    		params += ""; // default, no need to send to server
    	} else if (src.equals("UniProt")) {
    		params += "&source=UniProt";
    	} else {
    		params += "&source=HGNC";
    	}
    	
    	// handle advanced query
    	String qadv = m_advquery.getStringValue().trim();
    	if (qadv.length() > 0) {
    		params += "&q=" + qadv;
    	}
    	
    	// reference
    	String ref = m_field_ref.getStringValue().trim();
    	if (ref.length() > 0) {
    		params += "&ref=" + ref;
    	}
    	
    	// with
    	String wth = m_field_with.getStringValue().trim();
    	if (wth.length() > 0) {
    		params += "&with=" + wth;
    	}
    	
    	// taxonomy
    	String taxon = m_field_tax.getStringValue().trim();
    	if (taxon.length() > 0) {
    		params += "&taxonomy="+taxon;
    	}
    	
    	// ancestor
    	String ancestor = m_field_ancestor.getStringValue().trim();
    	if (ancestor.length() > 0) {
    		params += "&ancestor="+ancestor;
    	}
    	
    	// protein
    	String protein = m_field_protein.getStringValue().trim();
    	if (protein.length() > 0) {
    		params += "&prot=" + protein;
    	}
    	return params;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec out = make_output_spec(inSpecs[0], 
        							m_type.getStringValue().startsWith("Term"));
        return new DataTableSpec[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_url.saveSettingsTo(settings);
        m_type.saveSettingsTo(settings);
        m_advquery.saveSettingsTo(settings);
        m_terminfo_col.saveSettingsTo(settings);
        
        m_field_db.saveSettingsTo(settings);
        m_field_ancestor.saveSettingsTo(settings);
        m_field_evidence.saveSettingsTo(settings);
        m_field_source.saveSettingsTo(settings);
        m_field_ref.saveSettingsTo(settings);
        m_field_with.saveSettingsTo(settings);
        m_field_tax.saveSettingsTo(settings);
        m_field_protein.saveSettingsTo(settings);
        m_max_entries.saveSettingsTo(settings);        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        m_url.loadSettingsFrom(settings);
        m_type.loadSettingsFrom(settings);
        m_advquery.loadSettingsFrom(settings);
        m_terminfo_col.loadSettingsFrom(settings);
        
        m_field_db.loadSettingsFrom(settings);
      
        m_field_ancestor.loadSettingsFrom(settings);
        m_field_evidence.loadSettingsFrom(settings);
        m_field_source.loadSettingsFrom(settings);
        m_field_ref.loadSettingsFrom(settings);
        m_field_with.loadSettingsFrom(settings);
        m_field_tax.loadSettingsFrom(settings);
        m_field_protein.loadSettingsFrom(settings);
        m_max_entries.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        m_url.validateSettings(settings);
        m_type.validateSettings(settings);
        m_advquery.validateSettings(settings);
        m_terminfo_col.validateSettings(settings);
        
        m_field_db.validateSettings(settings);
    
        m_field_ancestor.validateSettings(settings);
        m_field_evidence.validateSettings(settings);
        m_field_source.validateSettings(settings);
        m_field_ref.validateSettings(settings);
        m_field_with.validateSettings(settings);
        m_field_tax.validateSettings(settings);
        m_field_protein.validateSettings(settings);
        m_max_entries.validateSettings(settings);
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

