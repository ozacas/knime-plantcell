package au.edu.unimelb.plantcell.io.ws.blast.wublast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import net.n3.nanoxml.IXMLElement;
import net.n3.nanoxml.IXMLParser;
import net.n3.nanoxml.IXMLReader;
import net.n3.nanoxml.StdXMLReader;
import net.n3.nanoxml.XMLParserFactory;

import org.biojava.bio.Annotation;
import org.biojava.bio.program.sax.BlastLikeSAXParser;
import org.biojava.bio.program.ssbind.BlastLikeSearchBuilder;
import org.biojava.bio.program.ssbind.SeqSimilarityAdapter;
import org.biojava.bio.search.SeqSimilaritySearchHit;
import org.biojava.bio.search.SeqSimilaritySearchResult;
import org.biojava.bio.search.SeqSimilaritySearchSubHit;
import org.biojava.bio.seq.db.DummySequenceDB;
import org.biojava.bio.seq.db.DummySequenceDBInstallation;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.image.png.PNGImageCell;
import org.knime.core.data.image.png.PNGImageContent;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.osgi.framework.Bundle;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import uk.ac.ebi.jdispatcher.soap.wublast.ArrayOfString;
import uk.ac.ebi.jdispatcher.soap.wublast.InputParameters;
import uk.ac.ebi.jdispatcher.soap.wublast.JDispatcherService;
import uk.ac.ebi.jdispatcher.soap.wublast.JDispatcherService_Service;
import uk.ac.ebi.jdispatcher.soap.wublast.ObjectFactory;
import uk.ac.ebi.jdispatcher.soap.wublast.WsParameterDetails;
import uk.ac.ebi.jdispatcher.soap.wublast.WsParameterValue;
import uk.ac.ebi.jdispatcher.soap.wublast.WsResultType;
import uk.ac.ebi.jdispatcher.soap.wublast.WsResultTypes;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.cells.CoordinateSystem;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;
import au.edu.unimelb.plantcell.core.cells.TrackCreator;
import au.edu.unimelb.plantcell.io.read.fasta.BatchFastaIterator;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcore.core.regions.AlignedRegionsAnnotation;
import au.edu.unimelb.plantcore.core.regions.BlastHitRegion;


/**
 * This is the model implementation of WUBlast.
 * Performs a WU-Blast with the chosen parameters using the EBI webservices. Rate controlled so as not to overload EBI computer systems.
 *
 * @author Andrew Cassin
 */
public class WUBlastNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("EBI WU-BLAST");
        
    private final int NUM_FORMATTED_COLUMNS = 13;		// number of *additional* columns per blast hit in second output port
    private final int MAX_RETRIES = 4;					// maximum retries to deal with intermittent network failures
    
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_DB     = "databases";
    static final String CFGKEY_FILTER = "filter";
	static final String CFGKEY_FILTERSTR = "database-filter-string";
    static final String CFGKEY_MATRIX = "matrix";
    static final String CFGKEY_PROGRAMS="programs";
    static final String CFGKEY_SENSITIVITY = "sensitivity";
    static final String CFGKEY_SORT = "sort-by";
    static final String CFGKEY_STATS= "statistics";
    static final String CFGKEY_SEQUENCE_COL = "sequence";
    static final String CFGKEY_EMAIL = "email";
    static final String CFGKEY_NUM_ALIGNMENTS = "num-alignments";
    static final String CFGKEY_NUM_SCORES = "num-scores";
    static final String CFGKEY_EVAL_THRESHOLD = "eval-threshold";
    static final String CFGKEY_EBI_BATCH_SIZE = "ebi-batch-size";
    static final String CFGKEY_STYPE = "sequence-type";
    static final String CFGKEY_SAVE_IMAGE = "save-image";
    static final String CFGKEY_EBI_LOADED = "loaded-ebi-settings?";
    
    // internal state to the dialog which must be persisted (all StringArray's)
	public static final String CFGKEY_EBI_SETTINGS_PROGS = "ebi-blast-programs";		// saved/loaded from the dialog code
	public static final String CFGKEY_EBI_SETTINGS_DATABASES = "ebi-databases";
	public static final String CFGKEY_EBI_SETTINGS_FILTERS = "ebi-filters";
	public static final String CFGKEY_EBI_SETTINGS_MATRICES = "ebi-matrices";
	public static final String CFGKEY_EBI_SETTINGS_SENSITIVITY = "ebi-sensitivity";
	public static final String CFGKEY_EBI_SETTINGS_SORT = "ebi-sort";
	public static final String CFGKEY_EBI_SETTINGS_STATISTICS = "ebi-statistics";
	
	private static final String DEFAULT_DB = "nr";
	private static final String DEFAULT_FILTER = "";
	private static final String DEFAULT_MATRIX = "blosum62";
	private static final String DEFAULT_PROGRAM= "blastp";
	private static final String DEFAULT_SENSITIVITY = "normal";
	private static final String DEFAULT_SORT = "pvalue";
	private static final String DEFAULT_STATS= "sump";
	private static final String DEFAULT_SEQUENCE_COL = "Sequence";
	private static final String DEFAULT_EMAIL = "must@specify.this.to.use.this.node";
	//private static final String DEFAULT_STYPE = "Protein";
	private static final String DEFAULT_EVAL = "1e-5";

	
    // parameters which must be persisted (see saveSettings() below)
    private final SettingsModelString m_db     = make_as_string(CFGKEY_DB);
    private final SettingsModelString m_db_filter = make_as_string(CFGKEY_FILTERSTR);
    private final SettingsModelString m_filter = make_as_string(CFGKEY_FILTER);
    private final SettingsModelString m_matrix = make_as_string(CFGKEY_MATRIX);
    private final SettingsModelString m_program= make_as_string(CFGKEY_PROGRAMS);
    private final SettingsModelString m_sensitivity = make_as_string(CFGKEY_SENSITIVITY);
    private final SettingsModelString m_sortby      = make_as_string(CFGKEY_SORT);
    private final SettingsModelString m_stats       = make_as_string(CFGKEY_STATS);
    private final SettingsModelString m_seq_col     = make_as_string(CFGKEY_SEQUENCE_COL);
    private final SettingsModelString m_email       = make_as_string(CFGKEY_EMAIL);
    private final SettingsModelIntegerBounded m_num_alignments = (SettingsModelIntegerBounded) make(CFGKEY_NUM_ALIGNMENTS);
    private final SettingsModelIntegerBounded m_num_scores = (SettingsModelIntegerBounded) make(CFGKEY_NUM_SCORES);
    private final SettingsModelString  m_eval_threshold =  make_as_string(CFGKEY_EVAL_THRESHOLD);
    private final SettingsModelIntegerBounded m_ebi_batch_size = (SettingsModelIntegerBounded) make(CFGKEY_EBI_BATCH_SIZE);
    private final SettingsModelBoolean m_save_image = (SettingsModelBoolean) make(CFGKEY_SAVE_IMAGE);
	
    private final SettingsModelBoolean     m_ebi_loaded = new SettingsModelBoolean(WUBlastNodeModel.CFGKEY_EBI_LOADED, false);
    private final SettingsModelStringArray m_ebi_programs = new SettingsModelStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_PROGS, new String[] {"blastp"} );
	private final SettingsModelStringArray m_full_db_list = new SettingsModelStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_DATABASES, new String[] { "" });
	private final SettingsModelStringArray m_ebi_filters  = new SettingsModelStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_FILTERS, new String[] { "none" } );
	private final SettingsModelStringArray m_ebi_matrices = new SettingsModelStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_MATRICES, new String[] { "blosum62" });
	private final SettingsModelStringArray m_ebi_sensitivity = new SettingsModelStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_SENSITIVITY, new String[] { "normal" });
	private final SettingsModelStringArray m_ebi_sort        = new SettingsModelStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_SORT, new String[] { "psort" });
	private final SettingsModelStringArray m_ebi_statistics  = new SettingsModelStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_STATISTICS, new String[] { "none" });
	
	
    // internal state to support Configure dialog
    //private static WsParameterValue[] m_ebi_progs, m_ebi_filters, m_ebi_matrices, m_ebi_sensitivity, m_ebi_sort, m_ebi_databases, m_ebi_stats;
        
    // internal stuff to each instance of the model
	private int m_done_rows = 0;
    private ExecutionContext m_exec;
    // as each EBI job is returned, these member variables reflect key results from the BLAST
    private DataCell m_result_png;
    private byte[]   m_result;		// either XML or text
    private boolean m_first_job, m_has_png, m_has_xml;
    
    /**
     * Constructor for the node model.
     */
    protected WUBlastNodeModel() {
        // one incoming port and two outgoing ports
        super(1, 2);
    }
    
    private boolean isFirstBatch() {
    	return (m_done_rows == 0);
    }
    
    private void incrDone(int how_many_sequences) {
    	assert(how_many_sequences >= 0);
    	m_done_rows += how_many_sequences;
    }
    
    public static SettingsModel make (String k) {
    	if (k.equals(CFGKEY_DB)) {
    		return new SettingsModelString(k, DEFAULT_DB);
    	} else if (k.equals(CFGKEY_FILTER)) {
    		return new SettingsModelString(k, DEFAULT_FILTER);
    	} else if (k.equals(CFGKEY_MATRIX)) {
    		return new SettingsModelString(k, DEFAULT_MATRIX);
    	} else if (k.equals(CFGKEY_PROGRAMS)) {
    		return new SettingsModelString(k, DEFAULT_PROGRAM);
    	} else if (k.equals(CFGKEY_SENSITIVITY)) {
    		return new SettingsModelString(k, DEFAULT_SENSITIVITY);
    	} else if (k.equals(CFGKEY_SORT)) {
    		return new SettingsModelString(k, DEFAULT_SORT);
    	} else if (k.equals(CFGKEY_STATS)) {
    		return new SettingsModelString(k, DEFAULT_STATS);
    	} else if (k.equals(CFGKEY_SEQUENCE_COL)) {
    		return new SettingsModelString(k, DEFAULT_SEQUENCE_COL);
    	} else if (k.equals(CFGKEY_EMAIL)) {
    		return new SettingsModelString(k, DEFAULT_EMAIL);
    	} else if (k.equals(CFGKEY_NUM_ALIGNMENTS)) {
    		return new SettingsModelIntegerBounded(k, 50, 0, 1000);
    	} else if (k.equals(CFGKEY_NUM_SCORES)) {
    		return new SettingsModelIntegerBounded(k, 50, 0, 1000);
    	} else if (k.equals(CFGKEY_EVAL_THRESHOLD)) {
    		return new SettingsModelString(k, DEFAULT_EVAL);
    	} else if (k.equals(CFGKEY_EBI_BATCH_SIZE)) {
    		return new SettingsModelIntegerBounded(k, 10, 1, 25);
    	//} else if (k.equals(CFGKEY_STYPE)) {
    	//	return new SettingsModelString(k, DEFAULT_STYPE);
    	} else if (k.equals(CFGKEY_FILTERSTR)) {
    		return new SettingsModelString(k, "uniprot");		// uniprot databases only by default
    	} else if (k.equals(CFGKEY_SAVE_IMAGE)) {
    		return new SettingsModelBoolean(CFGKEY_SAVE_IMAGE, false);
    	}
    	return null;
    }
    
    public static SettingsModelString make_as_string(String k) {
    	SettingsModelString sm = (SettingsModelString) make(k);
    	return sm;
    }
    
    public boolean is_protein_blast() {
    	boolean ret = true;
    	
    	String prog = m_program.getStringValue().toLowerCase().trim();
    	if (prog.endsWith("x") || prog.endsWith("n")) {
    		ret = false;
    	}
    	return ret;
    }
    
    public static JDispatcherService getClientProxy() {
   		 // NB: need to use the local WSDL copy rather than go online for it... so...
   		 try {
   			 Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.io.ws");
   			 URL u = FileLocator.find(bundle, new Path("/wsdl/wublast.wsdl"), null);
   			 
   			 // must not call default constructor for local WSDL... so...
   			 JDispatcherService_Service cli = new JDispatcherService_Service(u,
   					new QName("http://soap.jdispatcher.ebi.ac.uk", "JDispatcherService"));
   			 return cli.getJDispatcherServiceHttpPort();
   		 } catch (Exception e) {
   			 e.printStackTrace();
   			 Logger.getAnonymousLogger().warning("Unable to get WU-BLAST proxy: "+e.getMessage());
   			 return null;
   		 }
   	}
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        int seq_col_idx = inData[0].getDataTableSpec().findColumnIndex(m_seq_col.getStringValue());
        if (seq_col_idx < 0) {
        	throw new Exception("Cannot find column: "+m_seq_col.getStringValue()+" - reset the node?");
        }
        
    	// report what the node will do to the log, so the user knows what will happen
    	StringBuffer sb = new StringBuffer(10 * 1024);
    	
    	sb.append("Performing "+inData[0].getRowCount()+" BLAST searches using ");
    	sb.append(m_seq_col.getStringValue());
    	sb.append(" with parameters:\n");
    	sb.append("matrix = ");
    	sb.append(m_matrix.getStringValue());
    	sb.append(", e-value cutoff ");
    	sb.append(m_eval_threshold.getStringValue());
    	sb.append(", database = ");
    	sb.append(m_db.getStringValue());
    	sb.append('\n');
    	sb.append("sequence filter = ");
    	sb.append(m_filter.getStringValue());
    	sb.append(", num alignments = ");
    	sb.append(m_num_alignments.getIntValue());
    	sb.append(", num scores = ");
    	sb.append(m_num_scores.getIntValue());
    	sb.append('\n');
    	sb.append("algorithm = ");
    	sb.append(m_program.getStringValue());
    	sb.append(", sensitivity = ");
    	sb.append(m_sensitivity.getStringValue());
    	sb.append('\n');
    	if (is_protein_blast()) {
    		sb.append("blast'ing protein sequence.");
    	} else {
    		sb.append("blasting nucleotide sequence.");
    	}
    	logger.info(sb.toString());
    	
    	// warn if (likely) incorrect low-complexity sequence filter is used...
    	boolean is_dust = m_filter.getStringValue().startsWith("dust");
    	boolean is_seg  = m_filter.getStringValue().startsWith("seg");
    	if ((is_protein_blast() && is_dust) ||
    			(!is_protein_blast() && is_seg) ||
    			(is_blastx() && is_dust)) {
    		logger.warn("Likely incorrect low complexity filter for type of sequence: "+m_filter.getStringValue());
    		logger.warn("*** re-configuring the advanced settings is recommended, but will continue in 10 seconds...");
    		Thread.sleep(10 * 1000);
    	}
    		
        int batch_size = m_ebi_batch_size.getIntValue();

    	// create output port table specifications
    	m_exec = exec;
    	DataTableSpec[] output_tables = make_output_tables(inData[0].getSpec());
    	assert(output_tables.length == 2);
    	
    	// here we ensure the XML is put straight onto the disk... not encouraged by knime.org, but realistically the best 
        // option on memory constrained computers
        MyDataContainer c2 = new MyDataContainer(exec.createDataContainer(output_tables[0], false, 0), "Hit");
        MyDataContainer c1 = new MyDataContainer(exec.createDataContainer(output_tables[1], false, 0), "Seq");
        
        int n_rows     = inData[0].getRowCount();
        m_done_rows    = 0;
        IXMLParser p   = XMLParserFactory.createDefaultXMLParser();
        JDispatcherService proxy = getClientProxy();
        
        BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(inData[0].iterator(), seq_col_idx, batch_size, 
        		BatchFastaIterator.DEFAULT_TOTAL_MB_LIMIT, new SequenceProcessor() {
        			private boolean warned = false;
        			
					@Override
					public SequenceValue process(SequenceValue sv) {
						if (!warned && (!sv.isValid() || likely_wrong_sequence(sv))) {
			        		logger.warn("Note: likely wrong type of sequence (got "+sv.getSequenceType()+" for "+sv.getID() + " - are you sure?");
			        		warned = true;
			        	}
						return sv;
					}
        	
        });
        
        // process a batch of sequences at a time
        while (bsi.hasNext()) {
        	List<SequenceValue> batch = bsi.next();
        	
        	if (batch == null)
        		break;
        	
        	logger.info("First sequence in batch is: "+batch.get(0).getID());
        	String email = m_email.getStringValue();
        	if (email.equals(DEFAULT_EMAIL) || email.length() < 1) {
        		throw new Exception("Must set email address to be valid for you! EBI require this!");
        	}
        	
        	logger.info("Running batch of "+batch.size()+" sequences.");
        	Map<String,SequenceValue> jobs2seq = runBatch(proxy, batch);
        	for (int i=0; i<4; i++) {		// a blast should take at least 10s so...
    			exec.checkCanceled();
    			Thread.sleep(2500);
    		}
    		waitForBatchCompletion(proxy, jobs2seq);
        	
    		for (String jobId : jobs2seq.keySet()) {
    			m_result = null;
    			m_result_png = null;
            	boolean has_xml = getJobResult(proxy, jobId);
            	
            	// 1. fill in the second output port to build up the annotations
            	ArrayList<BlastHitRegion> l = new ArrayList<BlastHitRegion>();
            	
            	/**
            	 * Assume the blast returns XML and try to fill in second output port, falling
            	 * back to plain text if <code>hasXML()</code> says no...
            	 * 
            	 */
            	if (has_xml) {
            		process_xml(p, jobId, c2, l);
            	} else {
            		process_text(new ByteArrayInputStream(m_result), jobs2seq.get(jobId), c2, l);
            	}
            	
            	// 2. fill in first output port last since we need the sequence annotations to be built up...
            	if (m_result.length > 0) {
            		SequenceCell sc = new SequenceCell(jobs2seq.get(jobId));
            		Track t = sc.addTrack(Track.WUBLAST_TRACK, getTrackCreator());
            		AlignedRegionsAnnotation ara = new AlignedRegionsAnnotation();
            		ara.addRegions(l);
            		t.addAnnotation(ara);
                	c1.addRow(new DataCell[] {sc, new StringCell(jobId), 
                							safe_xml_cell(m_result), safe_image_cell(m_result_png)});
            	} else {
            		c1.addRow(new DataCell[] { new SequenceCell(jobs2seq.get(jobId)), 
            				new StringCell(jobId),
            				DataType.getMissingCell(), DataType.getMissingCell()});
            	}
            	       	               	
            	logger.debug("Processed and downloaded results for job: "+jobId);
                exec.checkCanceled();
    		}
    		
    		incrDone(batch.size());
    		exec.setProgress(((double)m_done_rows) / n_rows);
            exec.checkCanceled();

        	jobs2seq.clear();
        }
	    
        // once we are done, we close the containers and return tables
        return new BufferedDataTable[]{c2.close(), c1.close()}; 
    }

    private boolean is_blastx() {
		return m_program.getStringValue().toLowerCase().equals("blastx");
	}

	private InputParameters getInputParameters(SequenceValue sv) {
        ObjectFactory of = new ObjectFactory();
    	InputParameters ip = of.createInputParameters();
    	String prog = m_program.getStringValue().toLowerCase().trim();
    	ip.setProgram(prog);
    	ArrayOfString db = of.createArrayOfString();
    	db.getString().add(m_db.getStringValue());
    	ip.setDatabase(db);
    	ip.setMatrix(of.createInputParametersMatrix(m_matrix.getStringValue()));
    	ip.setFilter(of.createInputParametersFilter(m_filter.getStringValue()));
    	ip.setSensitivity(of.createInputParametersSensitivity(m_sensitivity.getStringValue()));
    	ip.setStats(of.createInputParametersStats(m_stats.getStringValue()));
    	ip.setSort(of.createInputParametersSort(m_sortby.getStringValue()));
    	ip.setExp(of.createInputParametersExp(m_eval_threshold.getStringValue()));
    	ip.setAlignments(of.createInputParametersAlignments(new Integer(m_num_alignments.getIntValue())));
    	ip.setScores(of.createInputParametersScores(new Integer(m_num_scores.getIntValue())));
    	ip.setSequence(of.createInputParametersSequence(sv.getStringValue()));
    	
    	ip.setStype(sv.getSequenceType().equals(SequenceType.AA) ? "protein" : "dna" );
    	
    	return ip;
	}

	/**
     * Returns true if the likely residues in the sequence do not match the type of blast
     * being invoked. Based on heuristics, it is possibly wrong in some situations. If non
     * whitespace and letter characters are encountered (eg. - or *) it will return true, giving
     * the user a chance to correct things before too much time is wasted...
     * 
     * @param stype
     * @param prog
     * @param sequence
     * @return
     */
    private boolean likely_wrong_sequence(SequenceValue sv) {
    	String upcase = sv.getStringValue().replaceAll("\\s+", "").toUpperCase();
		int acgtn = 0;
		int others= 0;
		for (int i=0; i<upcase.length(); i++) {
			char c = upcase.charAt(i);
			if (c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N') {
				acgtn++;
			}
			if (!Character.isLetter(c) && !Character.isWhitespace(c)) {
				others++;
			}
		}
		double ratio = ((double)acgtn) / upcase.length();
	
		if (others > 0) {
			logger.warn("Encountered non-single letter residue in "+sv.getID()+": bad data?");
			return true;
		}
		
		SequenceType st = sv.getSequenceType();
		if (!st.equals(SequenceType.AA)) {
			return (ratio < 0.8);
		} else { 						// AA
			return (ratio > 0.7);		// NB: allow up to 30% ACGT in protein sequence
		} 
	}

	/**
     * This routine may throw if the formatted result port contains duplicate column names (which we dont handle at the moment TODO)
     * @param input_table_spec
     * @return
     * @throws IllegalArgumentException
     * @throws NullPointerException
	 * @throws InvalidSettingsException 
     */
	private DataTableSpec[] make_output_tables(DataTableSpec inSpec) throws IllegalArgumentException,NullPointerException, InvalidSettingsException {
		DataColumnSpec[] cols = new DataColumnSpec[4];
        DataColumnSpecCreator dcsc = new DataColumnSpecCreator("Annotated Sequence (incl. WU-BLAST track)", SequenceCell.TYPE);
		int  index = inSpec.findColumnIndex(m_seq_col.getStringValue());
	    if (index >= 0) {		// column configured yet?
		        DataColumnProperties isp = inSpec.getColumnSpec(index).getProperties();
				TrackColumnPropertiesCreator tcpc = new TrackColumnPropertiesCreator(isp,
												new Track(Track.WUBLAST_TRACK, getTrackCreator())
											);
				dcsc.setProperties(tcpc.getProperties());
	     }
	    cols[0] = dcsc.createSpec();
        cols[1] = new DataColumnSpecCreator("EBI JobID", StringCell.TYPE).createSpec();
        cols[2] = new DataColumnSpecCreator("Blast Result", StringCell.TYPE).createSpec();
        cols[3] = new DataColumnSpecCreator("Graphical Result Summary", DataType.getType(PNGImageCell.class)).createSpec();
        
        DataTableSpec outputSpec = new DataTableSpec(cols);
        
        cols = new DataColumnSpec[NUM_FORMATTED_COLUMNS];
        cols[0] = new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
        cols[1] = new DataColumnSpecCreator("Alignment Length", IntCell.TYPE).createSpec();
        cols[2] = new DataColumnSpecCreator("Hit Accession (EBI)", StringCell.TYPE).createSpec();
        cols[3] = new DataColumnSpecCreator("Hit Description", StringCell.TYPE).createSpec();
        cols[4] = new DataColumnSpecCreator("Alignment Score", DoubleCell.TYPE).createSpec();
        cols[5] = new DataColumnSpecCreator("Alignment Bits", DoubleCell.TYPE).createSpec();
        cols[6] = new DataColumnSpecCreator("Alignment E-Value", DoubleCell.TYPE).createSpec();
        cols[7] = new DataColumnSpecCreator("Alignment Identities", DoubleCell.TYPE).createSpec();
        cols[8] = new DataColumnSpecCreator("Alignment Positives", DoubleCell.TYPE).createSpec();
        cols[9] = new DataColumnSpecCreator("Alignment Query Sequence", StringCell.TYPE).createSpec();
        cols[10] = new DataColumnSpecCreator("Alignment Pattern", StringCell.TYPE).createSpec();
        cols[11] = new DataColumnSpecCreator("Alignment Match Sequence", StringCell.TYPE).createSpec();
        cols[12] = new DataColumnSpecCreator("Alignment Query Start,Query End/Match Start,Match End", StringCell.TYPE).createSpec();
      
        DataTableSpec processedResultsOutputSpec = new DataTableSpec(cols);
        
        return new DataTableSpec[] { processedResultsOutputSpec, outputSpec };
	}

	private TrackCreator getTrackCreator() {
		return new TrackCreator() {

			@Override
			public Track createTrack(String name)
					throws InvalidSettingsException {
				return new Track(name, new AlignedRegionsAnnotation(), CoordinateSystem.OFFSET_FROM_START);
			}
			
		};
	}

	private DataCell safe_xml_cell(byte[] xml) {
    	if (xml == null || xml.length < 1) 
    		return DataType.getMissingCell();
    	// TODO: we cant return an XML cell since BLAST sometimes returns text... depending on the variant of blast
    	//return XMLUtilityFactory.createCell(xml, null);
    	return new StringCell(new String(xml));
    }

	private DataCell safe_image_cell(DataCell png_image) {
		if (png_image == null)
			return DataType.getMissingCell();
		return png_image;
	}

	protected String getServiceEndpoint() {
    	return null;
    }
  
    /**
     * Responsible for decoding text blast results into the second output port. Similar settings 
     * as per <code>process_xml()</code>
     * 
     * @throws Exception
     */
    
    private void process_text(InputStream is, SequenceValue sv, MyDataContainer c2, List<BlastHitRegion> l) {
    	assert(is != null && sv != null && c2 != null);
    	XMLReader rdr = (XMLReader) new BlastLikeSAXParser();
    	DummySequenceDB queryDB  = new DummySequenceDB("test");
    	ArrayList<SeqSimilaritySearchResult> searchResults = new ArrayList<SeqSimilaritySearchResult>();
    	BlastLikeSearchBuilder builder = new BlastLikeSearchBuilder(searchResults, queryDB, new DummySequenceDBInstallation());
    	
    	SeqSimilarityAdapter adapter = new SeqSimilarityAdapter();
    	adapter.setSearchContentHandler(builder);
    	rdr.setContentHandler(adapter);
    	try {
    		rdr.parse(new InputSource(is));
    		
    		for (SeqSimilaritySearchResult sssr : searchResults) {
    			for (Object o : sssr.getHits()) {
    				SeqSimilaritySearchHit sssh = (SeqSimilaritySearchHit) o;
    				
    				for (Object sh : sssh.getSubHits()) {
    					SeqSimilaritySearchSubHit s2 = (SeqSimilaritySearchSubHit) sh;
    					DataCell[] cells = new DataCell[NUM_FORMATTED_COLUMNS];
    					for (int i=0; i<cells.length; i++) {
    						cells[i] = DataType.getMissingCell();
    					}
    					Annotation a_sssh = sssh.getAnnotation();
    					cells[0] = new StringCell(sv.getID());
    					cells[1] = new StringCell(m_db.getStringValue());
    					// the subject id is typically a gene name, but we decide how to populate
    					// the table based on available information. Hopefully this is correct!
    					String hit_accsn = "";
    					if (a_sssh != null && a_sssh.getProperty("subjectDescription") != null) {
    						String descr = a_sssh.getProperty("subjectDescription").toString();
    						String[] tokens = descr.split("\\s+");
    						cells[2] = new StringCell(tokens[0]);		// accsn
    						hit_accsn= tokens[0];
    						cells[3] = new StringCell(descr.substring(tokens[0].length()).trim());
    					} else {
    						cells[2] = new StringCell(sssh.getSubjectID());
    						hit_accsn= sssh.getSubjectID();
    						// cells[3] (description) is missing...
    					}
    					cells[4] = new DoubleCell(s2.getScore());
    					cells[6] = new DoubleCell(s2.getEValue());
    					
    					Annotation annot = s2.getAnnotation();
    					int al = -1;
    					double bs = -1.0;
    					if (annot != null) {
    						bs = Double.parseDouble(annot.getProperty("score").toString());
    						cells[5] = new DoubleCell(bs);
    						cells[7] = new DoubleCell(new Double(annot.getProperty("numberOfIdentities").toString()));
    						cells[8] = new DoubleCell(new Double(annot.getProperty("numberOfPositives").toString()));
    						al = Integer.parseInt(annot.getProperty("alignmentSize").toString());
    						cells[1] = new IntCell(al);
    					}
    					cells[12] = new StringCell(""+s2.getQueryStart() + "," + s2.getQueryEnd() + "/" +
    							s2.getSubjectStart() +  ", "+ s2.getSubjectEnd());
    					c2.addRow(cells);
    					if (l != null) {
    						BlastHitRegion bhr = new BlastHitRegion(s2.getQueryStart(), s2.getQueryEnd(), sv.getID(), hit_accsn);
    						bhr.setLabel(hit_accsn);
    						bhr.setAlignmentLength(al);
    						bhr.setScore(bs);
    						bhr.setEvalue(s2.getEValue());
    						bhr.setOffset(1);
    						l.add(bhr);
    					}
    				}
    			}
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
	
    public static String[] get_printable_names(String parameter) {
    	return get_printable_names(parameter, true);
    }
    
    public static String[] get_printable_names(String parameter, boolean use_cached_results) {
        JDispatcherService proxy = getClientProxy();
        
        WsParameterDetails info = proxy.getParameterDetails(parameter);
        ArrayList<String> ret = new ArrayList<String>();
        for (WsParameterValue s : info.getValues().getValue()) {
        	ret.add(s.getValue());
        }
        
        return ret.toArray(new String[0]);
    }
    
    /**
     * Responsible for parsing the XML results from EBI and storing the de-normalised data into the table as multiple rows per result
     * @param proc_results where to put the results
     * @param r current row being processed in the input data
     * @return 
     */
    @SuppressWarnings("rawtypes")
	protected void process_xml(IXMLParser p, String jobId, MyDataContainer c2, List<BlastHitRegion> l) throws Exception { 	
    	IXMLReader rdr = new StdXMLReader(new ByteArrayInputStream(m_result));
    	p.setReader(rdr);
    	//System.err.println(xml);
    	
    	IXMLElement root = (IXMLElement) p.parse();
    	Vector v_ss = root.getChildrenNamed("SequenceSimilaritySearchResult");
    	if (v_ss == null) {
    		logger.warn("Unable to find hit for "+jobId+" - maybe a problem at EBI?");
    		System.err.println(new String(m_result));
    		return;
    	}
    	Iterator i_sssr = v_ss.iterator();
    	while (i_sssr.hasNext()) {
    		IXMLElement sssr = (IXMLElement) i_sssr.next();
	    	
	    	IXMLElement hits = sssr.getFirstChildNamed("hits");
	    	Vector each_hit  = hits.getChildrenNamed("hit");
	    	Iterator i = each_hit.iterator();
	    	//int n_rows = 0;
	    	while (i.hasNext()) {
	    		IXMLElement hit = (IXMLElement) i.next();
	    		IXMLElement alignments = hit.getFirstChildNamed("alignments");
	    		Vector al = alignments.getChildrenNamed("alignment");
	    		Iterator ia = al.iterator();
	    		
	    		while (ia.hasNext()) {
	    			IXMLElement alignment  = (IXMLElement) ia.next();
	    			IXMLElement score      = alignment.getFirstChildNamed("score");
	    			IXMLElement bits       = alignment.getFirstChildNamed("bits");
	    			IXMLElement eval       = alignment.getFirstChildNamed("expectation");
	    			IXMLElement identities = alignment.getFirstChildNamed("identity");
	    			IXMLElement positives  = alignment.getFirstChildNamed("positives");
	    			IXMLElement query      = alignment.getFirstChildNamed("querySeq");
	    			IXMLElement pattern    = alignment.getFirstChildNamed("pattern");
	    			IXMLElement match      = alignment.getFirstChildNamed("matchSeq");
	    			
	    			DataCell[] cells = new DataCell[13];
	            	for (int k=0; k<cells.length; k++) {
	            		cells[k] = DataType.getMissingCell();
	            	}
	            	
	    			cells[0]             = new StringCell(jobId);
	    			String acsn = hit.getAttribute("ac", "");
	    			String descr= hit.getAttribute("description", "");
	    			
	    			cells[2]             = new StringCell(acsn);
	    			cells[3]             = new StringCell(descr);
	    			String str_score     = score.getContent();
	    			String str_bits      = bits.getContent();
	    			String str_eval      = eval.getContent();
	    			String str_ident     = identities.getContent();
	    			String str_positives = positives.getContent();
	    			double bs = Double.parseDouble(str_score);
	    			double escore = Double.parseDouble(str_eval);
	    			cells[4] = new DoubleCell(bs);
	    			cells[5] = new DoubleCell(new Double(str_bits).doubleValue());
	    			cells[6] = new DoubleCell(escore);
	    			cells[7] = new DoubleCell(new Double(str_ident).doubleValue());
	    			cells[8] = new DoubleCell(new Double(str_positives).doubleValue());
	    			cells[9] = new StringCell(query.getContent());
	    			cells[10]= new StringCell(pattern.getContent());
	    			cells[11]= new StringCell(match.getContent());
	    			
	    			String qstart = query.getAttribute("start", "");
	    			String qend   = query.getAttribute("end", "");
	    			String attr =  qstart+"," +
	                				 qend+ "/" + 
	                				match.getAttribute("start", "") + "," +
	                				match.getAttribute("end", "");
	    			cells[12]= new StringCell(attr);
	    			
	     			c2.addRow(cells);
	     			if (l != null) {
	     				int q_start = Integer.parseInt(qstart);
	     				int q_end   = Integer.parseInt(qend);
						BlastHitRegion bhr = new BlastHitRegion(q_start, q_end, query.getContent(), acsn);
						bhr.setLabel(acsn);
						bhr.setAlignmentLength(-1);
						bhr.setScore(bs);
						bhr.setEvalue(escore);
						
						l.add(bhr);
	     			}
	     			cells = null;
	     			attr = null;
	     			str_score = null;
	     			str_bits  = null;
	     			str_eval  = null;
	     			str_ident = null;
	     			str_positives = null;
	     			acsn = null;
	     			descr=null;
	    		}
	    		ia = null;
	    		al = null;
	    	}
	    	i        = null;
	    	rdr      = null;
	    	each_hit = null;
    	}
    }
    
    /**
     * Returns true if the result format of the hits is XML, false if its text
     * 
     * @param jobId
     * @return
     * @throws Exception
     */
    public boolean getJobResult(JDispatcherService proxy, String jobId) throws Exception {
    	// retry in case of intermittent failure
    	m_result_png = null;
    	m_result     = null;
    	for (int i=0; i<MAX_RETRIES; i++) {
    		try {
    		    if (isFirstJob()) {
    		       WsResultTypes types = proxy.getResultTypes(jobId);
    		       setHasPNG(false);
    		       setHasXML(false);
    			   for (WsResultType t : types.getType()) {
    				   String id = t.getIdentifier();
    				   if (id.equalsIgnoreCase("xml")) {
    					   setHasPNG(true);
    				   } else if (id.equals("visual-png")) {
    					   setHasPNG(true);
    				   }
    			   }
    		    }
    			
    		    // get the xml or text result if XML is not available
    		    m_result = proxy.getResult(jobId, hasXML() ? "xml" : "out", null); // m_output_format is either 'xml' or 'out' for what this node needs
    			
    			// save the graphical results summary if configured by the user
    			m_result_png = DataType.getMissingCell();
    			if (m_save_image.getBooleanValue()) {
    				if (!hasPNG() && isFirstJob()) {
    					logger.warn("PNG BLAST results not available - perhaps reconfigure the node.");
    				}
    				byte[] result = proxy.getResult(jobId, "visual-png", null);
    				if (result != null) {
    					m_result_png = new PNGImageContent(result).toImageCell();
    				} 
    			}
    			
	    	   return hasXML();
    		} catch (Exception ce) {
    			// an error at the first job usually indicates a parameter problem ie. no retry
    			if (isFirstBatch() || i == MAX_RETRIES-1)		
    				throw ce;
    			
				logger.info(ce.getMessage());
				logger.warn("Blast getJobResult(): could not connect, retrying in "+((i+1)*500)+" seconds.");
				Thread.sleep((i+1)*500*1000);
    			// else fallthru
    		} 
    	}
    	
    	return false;	// if this is executed, something is very wrong so assume text...
    }
    
    private boolean hasPNG() {
		return m_has_png;
	}

	private boolean hasXML() {
		return m_has_xml;
	}
	
	private void setHasXML(boolean b) {
		m_has_xml = b;
	}

	private void setHasPNG(boolean b) {
		m_has_png = b;
	}

	private boolean isFirstJob() {
		return m_first_job;
	}

	/**
     * Waits for the entire batch to complete. Since the batch has just been submitted, we wait
     * for at least 60s before checking the first job for completion
     * 
     * @param jobs2seq
     * @throws Exception
     */
    protected void waitForBatchCompletion(JDispatcherService proxy, Map<String, SequenceValue> jobs2seq) throws Exception {    	
    	/* this system produces a lot of objects: XML etc. so keep the garbage collector busy... */
		System.gc();	
		System.runFinalization();
    	for (int i=0; i<12; i++) {   		
    		Thread.sleep(5 * 1000);     // sleep for five seconds and then check for cancel
    		m_exec.checkCanceled();
    	}
    	for (String jobId : jobs2seq.keySet()) {
    		waitForCompletion(proxy, jobId);
    		logger.info("Job completed: "+jobId);
    		m_exec.checkCanceled();
    	}
    	logger.info("Batch completed.");
    }
    
    protected void waitForCompletion(JDispatcherService proxy, String jobId) throws Exception {
    	if (jobId.length() > 0) {
    		int check_period = 20 * 1000; // every 10s
    		String status = "PENDING";
    		int retry = 0;
    		while (status.equals("PENDING") || status.equals("RUNNING")) {
    			try {
    				logger.info("Waiting for "+jobId);
    				
    				status = proxy.getStatus(jobId);
    				if (status.equals("RUNNING") || status.equals("PENDING")) {
    					logger.info(jobId + " " + status + ", sleeping for "+check_period+ " milliseconds");
    					
    					// check ten times each check_period to see if the user pressed cancel
    					for (int i=0; i<10; i++) {
    						Thread.sleep(check_period / 10);
    						m_exec.checkCanceled();
    					}
    					
    					// each time job is still going, we double check_period to reduce likelihood of overloading EBI
    					check_period *= 2;
    					if (check_period > 200000) {
    						check_period = 200000;
    					}
    				}
    				if (status == "FAILED") {
    					logger.error("WU-BLAST job failed: "+jobId);
    				}
    			} catch (InterruptedException ie) {
    				throw ie;
    			} catch (CanceledExecutionException ce) {
    				throw ce;
    			} catch (Exception e) {
    				if (isFirstBatch())		// an error at the first job usually indicates a parameter problem ie. no retry
        				throw e;
    				if (retry < MAX_RETRIES) {
    					logger.warn("Unable to check job "+jobId+" retrying (after linear-backoff delay)... ");
    					Thread.sleep(((420 * retry) + 120)* 1000);
    					status = "PENDING";
    					retry++;
    				} else {
    					throw new Exception("Cannot check job "+jobId+" via WU-BLAST (EBI)... aborting"+e);
    				}
    			} 
    		}
    	} else {
    		throw new Exception("Bogus EBI job id... aborting!");
    	}
    }
    
    /**
     * Submits a batch of jobs to EBI and returns the EBI-assigned job-id's to the caller. Returns probably before the jobs complete.
     * @param is_first 
     * @param ip
     * @param d
     * @return
     */
    protected Map<String,SequenceValue> runBatch(JDispatcherService proxy, List<SequenceValue> batch) throws Exception {
    	assert (proxy != null && batch != null && batch.size() > 0);
    	Map<String,SequenceValue> ret = new HashMap<String,SequenceValue>();
    	for (SequenceValue sv : batch) {
    		m_exec.checkCanceled(); // stop submitting once cancel chosen by user
    		String jobId = doBLAST(proxy, getInputParameters(sv));
    		ret.put(jobId, sv);
    	}
    	return ret;
    }
    
    /**
     * Submits a single job to EBI. Does not wait for the job to complete before returning to the caller.
     * @param ip
     * @param data
     * @return
     * @throws Exception
     */
    protected String doBLAST(JDispatcherService proxy, InputParameters ip) throws Exception {
    	 String jobId = "";
    	 //System.err.println(ip);
    	 //System.err.println(data);
    	 for (int retry = 0; retry < MAX_RETRIES; retry++) {
    		 try {
    			 jobId = proxy.run(m_email.getStringValue(), "", ip);
    			 if (jobId.length() > 0) {
            		 logger.info("Successfully submitted WU-BLAST job: "+jobId);
            	 }
    			 break;
    		 }
    		 catch (Exception e) {
    			 // an error at the first job usually indicates a parameter problem ie. no retry
    			 // repeated errors indicate EBI is down or some local network issue
    			 if (isFirstBatch() || retry == MAX_RETRIES-1)		
        				throw e;
    			 
				 logger.warn("Unable to submit job: "+e+ " .... retrying....");
				 Thread.sleep((420 * retry + 120) * 1000);
    		 }
    	 }
    	
    	 //System.err.println(jobId);
    	 return jobId;
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
    	try {
    		return make_output_tables(inSpecs[0]);
    	} catch (Exception e) {
    		e.printStackTrace();
    		// always throw a type consistent with the throws declaration above
    		throw new InvalidSettingsException(e.getMessage());
    	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	 m_db.saveSettingsTo(settings);
    	 m_db_filter.saveSettingsTo(settings);
    	 m_filter.saveSettingsTo(settings);
    	 m_matrix.saveSettingsTo(settings);
    	 m_program.saveSettingsTo(settings);
    	 m_sensitivity.saveSettingsTo(settings);
    	 m_sortby.saveSettingsTo(settings);
    	 m_stats.saveSettingsTo(settings);
    	 m_seq_col.saveSettingsTo(settings);
    	 m_email.saveSettingsTo(settings);
    	 m_num_alignments.saveSettingsTo(settings);
    	 m_num_scores.saveSettingsTo(settings);
    	 m_eval_threshold.saveSettingsTo(settings);
    	 m_ebi_batch_size.saveSettingsTo(settings);
    	 m_save_image.saveSettingsTo(settings);
    	 
    	 // dialog state
    	 m_ebi_loaded.saveSettingsTo(settings);
    	 m_ebi_programs.saveSettingsTo(settings);
    	 m_full_db_list.saveSettingsTo(settings);
    	 m_ebi_filters.saveSettingsTo(settings);
    	 m_ebi_matrices.saveSettingsTo(settings);
    	 m_ebi_sensitivity.saveSettingsTo(settings);
    	 m_ebi_sort.saveSettingsTo(settings);
    	 m_ebi_statistics.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	 m_db.loadSettingsFrom(settings);
    	 m_db_filter.loadSettingsFrom(settings);
    	 m_filter.loadSettingsFrom(settings);
    	 m_matrix.loadSettingsFrom(settings);
    	 m_program.loadSettingsFrom(settings);
    	 m_sensitivity.loadSettingsFrom(settings);
    	 m_sortby.loadSettingsFrom(settings);
    	 m_stats.loadSettingsFrom(settings);
    	 m_seq_col.loadSettingsFrom(settings);
    	 m_email.loadSettingsFrom(settings);
    	 m_num_alignments.loadSettingsFrom(settings);
    	 m_num_scores.loadSettingsFrom(settings);
    	 m_eval_threshold.loadSettingsFrom(settings);
    	 m_ebi_batch_size.loadSettingsFrom(settings);
    	 m_save_image.loadSettingsFrom(settings);
    	 
    	 m_ebi_loaded.setBooleanValue(false);
    	 if (settings.containsKey(CFGKEY_EBI_LOADED)) {
    		 m_ebi_loaded.loadSettingsFrom(settings);
    		 if (m_ebi_loaded.getBooleanValue()) {
    			 m_ebi_programs.loadSettingsFrom(settings);
    	    	 m_full_db_list.loadSettingsFrom(settings);
    	    	 m_ebi_filters.loadSettingsFrom(settings);
    	    	 m_ebi_matrices.loadSettingsFrom(settings);
    	    	 m_ebi_sensitivity.loadSettingsFrom(settings);
    	    	 m_ebi_sort.loadSettingsFrom(settings);
    	    	 m_ebi_statistics.loadSettingsFrom(settings);
    		 }
    	 }    	
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	 m_db.validateSettings(settings);
    	 m_db_filter.validateSettings(settings);
    	 m_filter.validateSettings(settings);
    	 m_matrix.validateSettings(settings);
    	 m_program.validateSettings(settings);
    	 m_sensitivity.validateSettings(settings);
    	 m_sortby.validateSettings(settings);
    	 m_stats.validateSettings(settings);
    	 m_seq_col.validateSettings(settings);
    	 m_email.validateSettings(settings);
    	 m_num_alignments.validateSettings(settings);
    	 m_num_scores.validateSettings(settings);
    	 m_eval_threshold.validateSettings(settings);
    	 m_ebi_batch_size.validateSettings(settings);
    	 m_save_image.validateSettings(settings);
    	 
    	 m_ebi_programs.validateSettings(settings);
    	 m_full_db_list.validateSettings(settings);
    	 m_ebi_filters.validateSettings(settings);
    	 m_ebi_matrices.validateSettings(settings);
    	 m_ebi_sensitivity.validateSettings(settings);
    	 m_ebi_sort.validateSettings(settings);
    	 m_ebi_statistics.validateSettings(settings);
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

