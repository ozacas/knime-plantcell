package au.edu.unimelb.plantcell.blast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.workbench.ui.KNIMEUIPlugin;

import au.edu.unimelb.plantcell.core.ExternalProgram;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.PreferenceConstants;
import au.edu.unimelb.plantcell.core.PreferencePage;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.CoordinateSystem;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;
import au.edu.unimelb.plantcell.core.cells.TrackCreator;
import au.edu.unimelb.plantcell.io.read.fasta.BatchFastaIterator;
import au.edu.unimelb.plantcell.io.write.fasta.FastaWriter;
import au.edu.unimelb.plantcore.core.regions.AlignedRegionsAnnotation;
import au.edu.unimelb.plantcore.core.regions.BlastHitRegion;


/**
 * This is the model implementation of BLASTPlus.
 * Supports local execution of NCBI BLAST+ executables (which must be  installed separately)
 *
 * @author http://www.plantcell.unimelb.edu.au/
 */
public class BLASTPlusNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("NCBI BLAST+");
        
    // what sequences should be reported? (NB: dont change the order with fixing the report*() methods: see below)
    public final static String[] ANNOTATION_GROUP = new String[] { "All", "Sequences with hits", "Sequences without hits" };
    
    // blast configuration settings (for dialog)
    public static final String CFGKEY_DATABASE       = "subject-database-fasta";
    public static final String CFGKEY_QUERY_DATABASE = "query-sequences-fasta";
    public static final String CFGKEY_EVAL           = "evalue-cutoff";
    public static final String CFGKEY_BLAST_PROG     = "ncbi-blast-program";
    public static final String CFGKEY_NUM_THREADS    = "n-threads";
	public static final String CFGKEY_MATRIX         = "scoring-matrix";
    public static final String CFGKEY_OTHER			 = "other-blast-arguments";
    public static final String CFGKEY_FILTER         = "low-complexity-sequence-filter";
    public static final String CFGKEY_RESULTS        = "result-columns";
    public static final String CFGKEY_ANNOTATE_WHAT  = "annotate-which-sequences?";
    
    private SettingsModelString m_db          = new SettingsModelString(CFGKEY_DATABASE, "");
    private SettingsModelString m_q_db        = new SettingsModelString(CFGKEY_QUERY_DATABASE, "");
    private SettingsModelDouble m_eval_cutoff = new SettingsModelDouble(CFGKEY_EVAL, 0.00001);
    private SettingsModelString m_ncbi_prog   = new SettingsModelString(CFGKEY_BLAST_PROG, "blastp");
    private SettingsModelIntegerBounded m_threads = new SettingsModelIntegerBounded(CFGKEY_NUM_THREADS, 1, 1, 8);
    
    private SettingsModelString m_matrix      = new SettingsModelString(CFGKEY_MATRIX, "BLOSUM62");
    private SettingsModelString m_other       = new SettingsModelString(CFGKEY_OTHER, "");
    private SettingsModelBoolean m_low_complexity_filter = new SettingsModelBoolean(CFGKEY_FILTER, true);
    private SettingsModelStringArray m_results = new SettingsModelStringArray(CFGKEY_RESULTS, getResultDefaults());
    private SettingsModelString m_what_annotations = new SettingsModelString(CFGKEY_ANNOTATE_WHAT, "all");

    // internal state (not persisted)
    private File m_tmp_fasta;		// holds the current batch of query sequences

    /**
     * Constructor for the node model.
     */
    protected BLASTPlusNodeModel() {
            this(0, 2);
    }

    public BLASTPlusNodeModel(int n_in, int n_out) {
		super(n_in, n_out);
	}

    public boolean reportHits() {
    	return (m_what_annotations.getStringValue().equals(ANNOTATION_GROUP[0]) ||
    			m_what_annotations.getStringValue().equals(ANNOTATION_GROUP[1]));
    }
    
    public boolean reportNoHits() {
    	return (m_what_annotations.getStringValue().equals(ANNOTATION_GROUP[0]) ||
    			m_what_annotations.getStringValue().equals(ANNOTATION_GROUP[2]));
    }
    
    /**
     * Return the list of available result options (sorted alphabetically)
     */
    public static String[] getResultOptions() {
    	String[] ret = getMap().keySet().toArray(new String[0]);
    	Arrays.sort(ret);
    	return ret;
    }
    
    public static String[] getResultDefaults() {
    	String[] wanted = new String[] { "qseqid", "sseqid", "pident", "length", "mismatch",
    			"gapopen", "qstart", "qend", "sstart", "send", "evalue", "bitscore"
    	};
    	
    	ArrayList<String> ret = new ArrayList<String>();
    	Map<String,String> m = getMap();
    	for (String key : m.keySet()) {
    		String val = m.get(key);
    		for (String w : wanted) {
    			if (val.equals(w)) {
    				ret.add(key);
    				break;
    			}
    		}
    	}
    			   
    	return ret.toArray(new String[0]);
    }
    
    public final String getOutFmtString() {
    	return getOutFmtString(m_results.getStringArrayValue());
    }
    
    public final static String getOutFmtString(String[] wanted_descriptions) {
    	StringBuilder sb = new StringBuilder();
    	sb.append("7 ");		// commented TSV format
    	Map<String,String> m = getMap();
    	HashSet<String> wa = new HashSet<String>();
    	for (String w : wanted_descriptions) {
    		String opt = m.get(w);
    		if (opt == null) {
    			logger.warn("Could not get command line option for (ignoring): "+w);
    			continue;
    		}
    		if (opt.startsWith("_")) {	// computed columns need data to do the calculation...
    			for (String s : new String[] {"evalue", "bitscore", "qseqid", "sseqid", "length",
    					"pident", "qstart", "qend", "sstart", "send"}) {
    				wa.add(s);
    			}
    			// note: we dont add w to wa (of no use to BLAST!)
    		} else {
    			wa.add(opt);
    		}
    	}
    	for (String val : wa) {
    		if (val.startsWith("_")) {	// computed by node or by blast?
    			continue;
    		}
    		sb.append(val);
    		sb.append(' ');
    	}
    	return sb.toString().trim();
    }
    
    private static Map<String,String> getMap() {
    	   HashMap<String, String> m = new HashMap<String,String>(70);
    	   
    	   m.put("Query Seq-id", "qseqid");
    	   m.put("Query GI", "qgi");
           m.put("Query accession", "qacc");
           m.put("Query accesion.version", "qaccver");
           m.put("Query sequence length", "qlen");
           m.put("Subject Seq-id", "sseqid");
           m.put("All subject Seq-id(s), separated by a ';'", "sallseqid");
           m.put("Subject GI", "sgi");
           m.put("All subject GIs", "sallgi");
           m.put("Subject accession", "sacc");
           m.put("Subject accession.version", "saccver");
           m.put("All subject accessions", "sallacc");
           m.put("Subject sequence length", "slen");
           m.put("Start of alignment in query", "qstart");
           m.put("End of alignment in query", "qend");
           m.put("Start of alignment in subject", "sstart");
           m.put("End of alignment in subject", "send");
           m.put("Aligned part of query sequence", "qseq");
           m.put("Aligned part of subject sequence", "sseq");
           m.put("Expect value", "evalue");
           m.put("Bit score", "bitscore");
           m.put("Raw score", "score");
           m.put("Alignment length", "length");
           m.put("Percentage of identical matches", "pident");
           m.put("Number of identical matches", "nident");
           m.put("Number of mismatches", "mismatch");
           m.put("Number of positive-scoring matches", "positive");
           m.put("Number of gap openings", "gapopen");
           m.put("Total number of gaps", "gaps");
           m.put("Percentage of positive-scoring matches", "ppos");
           m.put("Query and subject frames separated by a '/'", "frames");
           m.put("Query frame", "qframe");
           m.put("Subject frame", "sframe");
           m.put("Blast traceback operations (BTOP)", "btop");
           
           // _ denotes computed by node, not BLAST
           m.put("Annotated query sequence (best 20 evalue-hits)", "_annotations_e20");
           m.put("Annotated query sequence (best hit only)", "_annotations_best");
           m.put("Annotated query sequence (longest alignment only)", "_annotations_longest");
           return m;
    }
    
    /**
     * Returns <code>true</code> if the specified sequence is of a suitable type
     * for the specified blast, otherwise <code>false</code>
     */
    public boolean valid_sequence_type(SequenceValue sv) {
    	assert(sv != null);
    	
    	String prog = m_ncbi_prog.getStringValue().trim().toLowerCase();
    	if (prog.endsWith("blastp")) {
    		return sv.getSequenceType().equals(SequenceType.AA);
    	} else {
    		return (!sv.getSequenceType().equals(SequenceType.AA));
    	}
    }
    
	/**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	logger.info("Locating blast program...");
    	File blast = find_blast_program(m_ncbi_prog.getStringValue());
    	if (blast == null) {
    		throw new InvalidSettingsException("Unable to locate "+m_ncbi_prog.getStringValue()+" program - re-configure?");
    	}
    	
    	/**
    	 * We batch the query sequences to 
    	 * 1) provide an estimate in the progress bar and
    	 * 2) 
    	 */
        m_tmp_fasta = File.createTempFile("in_seq", ".queries.fasta");
        

    	/**
    	 *  this node is tricky: it doesnt specify an output table spec until the fields are 
    	 *  known from the stream or, if an error occurs, a silly output table is created to keep KNIME happy
    	 */
    	
    	logger.info("Running "+blast.getAbsolutePath());
    	CommandLine cmdLine = new CommandLine(blast);
    	
    	// run makeblastdb if needed
    	make_blastdb(new File(getBLASTDatabase()));
   
    	// add arguments to BLAST
    	addArguments(cmdLine);
    	
    	// up to 1000 query sequences per invocation of blast...
    	BatchFastaIterator bfi = new BatchFastaIterator(new File(m_q_db.getStringValue()), 
    			is_protein_blast() ? SequenceType.AA : SequenceType.Nucleotide, 1000, 20 * 1024 * 1024);
    	    	
    	TabbedCSVLogger tsv = new TabbedCSVLogger(logger, exec);

    	// for the BLAST from fasta file node, we know that there is no tracks to begin
    	// with (cant be: no input ports to the node) so we dont have to merge existing tracks
    	DataColumnSpec[] col = new DataColumnSpec[] { mergeColumnProperties(null, 
						    			new Track[] {
										new Track(Track.BLAST_LONGEST_TRACK,   getTrackCreator()),
										new Track(Track.BLAST_BEST_EVAL_TRACK, getTrackCreator()),
										new Track(Track.BLAST_TOP20_TRACK,     getTrackCreator()) }
    			) };
    	
		final MyDataContainer c_seq = new MyDataContainer(exec.createDataContainer(new DataTableSpec(col)), "Query");
		
    	while (bfi.hasNext()) {
    		HashMap<UniqueID,SequenceValue> batch_map = bfi.nextAsMap();
    		if (batch_map == null)
    			break;
   
    		logger.info("Got batch of "+batch_map.size()+" sequences.");
    		new FastaWriter(m_tmp_fasta, batch_map).write();
    		logger.info("Saved batch to "+m_tmp_fasta.getAbsolutePath());
    		
    		// run blast on batch
	    	DefaultExecutor exe = new DefaultExecutor();
	    	exe.setExitValues(new int[] {0, 1});
	    	tsv.setBatch(batch_map);
	    	exe.setStreamHandler(new PumpStreamHandler(tsv, new ErrorLogger(logger)));
	    	exe.setWorkingDirectory(getQueryDatabaseFile().getParentFile());		// arbitrary choice
	    	exe.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
	    	
	    	logger.info("Running: "+cmdLine.toString());
	    	int exitCode = exe.execute(cmdLine);
	    	logger.info("got exit code: "+exitCode+" from BLAST");
	    	if (exe.isFailure(exitCode)) {
	    		if (exe.getWatchdog().killedProcess())
	    			throw new Exception("BLAST failed - watchdog says no...");
	    		else
	    			throw new Exception("BLAST failed - check console messages and input data");
	    	}
	    	
	    	// user want annotated sequences?
	    	tsv.walkResults(new BlastResultsWalker() {

				@Override
				public void hit(SequenceValue hit, List<BlastHitRegion> hits) {
					if (!reportHits())
						return;
		    		BlastHitRegion longest_bh = find_longest_bh(hits);
		    		BlastHitRegion lowest_eval= find_best_eval_bh(hits);
		    		List<BlastHitRegion> top20= find_best_20(hits);
		    		
		    		// add non-null result tracks to sc
		    		SequenceCell sc;
					try {
						sc = new SequenceCell(hit);
			    		addTrack(sc, Track.BLAST_BEST_EVAL_TRACK, lowest_eval);
			    		addTrack(sc, Track.BLAST_LONGEST_TRACK,   longest_bh);
			    		addTrack(sc, Track.BLAST_TOP20_TRACK,     top20);
			    		
			    		c_seq.addRow(new DataCell[] { sc });
					} catch (InvalidSettingsException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void nohit(SequenceValue hit) {
					if (!reportNoHits()) 
						return;
					try {
						c_seq.addRow(new DataCell[] { new SequenceCell(hit) });
					} catch (InvalidSettingsException e) {
						e.printStackTrace();
					}
				}
	    		
	    	});
	    
    	}
    	
        return new BufferedDataTable[] { tsv.getTSVTable(), c_seq.close() };
    }

    /**
     * Merges the tracks from any existing column properties and creates a single column with the
     * tracks merged in. Used from subclasses too for the various blast variants.
     * @param existing_properties the existing tracks defined in this column
     * @param tracks a non-empty (or null) vector of tracks to add to the column.
     * @return
     */
    protected DataColumnSpec mergeColumnProperties(DataColumnProperties existing_properties, Track[] tracks) {
    	DataColumnSpecCreator dcsc = new DataColumnSpecCreator("Annotated Sequence (incl. BLAST hits)", SequenceCell.TYPE);
		
    	if (existing_properties == null)
    		existing_properties = new DataColumnProperties();
    	
		TrackColumnPropertiesCreator tcpc = new TrackColumnPropertiesCreator(existing_properties, tracks);
		dcsc.setProperties(tcpc.getProperties());
		return dcsc.createSpec();
	}

	protected void addTrack(SequenceValue sv, String trackName, BlastHitRegion one) 
    				throws InvalidSettingsException {
		ArrayList<BlastHitRegion> tmp = new ArrayList<BlastHitRegion>();
		tmp.add(one);
		addTrack(sv, trackName, tmp);
	}

    protected void addTrack(SequenceValue sv, String trackName, List<BlastHitRegion> tmp)
					throws InvalidSettingsException {
		assert(sv != null);
		Track t = sv.addTrack(trackName, getTrackCreator());
		
		((AlignedRegionsAnnotation)t.getAnnotation()).addRegions(tmp);
	}

	protected TrackCreator getTrackCreator() {
    	return new TrackCreator() {

			@Override
			public Track createTrack(String name)
					throws InvalidSettingsException {
				return new Track(name, new AlignedRegionsAnnotation(), CoordinateSystem.OFFSET_FROM_START);
			}
			
		};
	}

	protected List<BlastHitRegion> find_best_20(List<BlastHitRegion> list) {
		// ascending sort: ie. best hits first
		Collections.sort(list, new Comparator<BlastHitRegion>() {

			@Override
			public int compare(BlastHitRegion a, BlastHitRegion b) {
				if (a.getEvalue() < b.getEvalue())
					return -1;
				else if (a.getEvalue() > b.getEvalue())
					return 1;
				else
					return 0;
			}
			
		});
		
		int n = 20;
		if (list.size() < 20) 
			n = list.size();
		ArrayList<BlastHitRegion> ret = new ArrayList<BlastHitRegion>();
		for (int i=0; i<n; i++) {
			ret.add(list.get(i));
		}
		return ret;
	}

	protected BlastHitRegion find_best_eval_bh(List<BlastHitRegion> l) {
		BlastHitRegion ret = null;
		Double evalue = Double.MAX_VALUE;
		for (BlastHitRegion hit : l) {
			if (hit.getEvalue() < evalue) {
				ret = hit;
				evalue = hit.getEvalue();
			}
		}
		return ret;
	}

	protected BlastHitRegion find_longest_bh(List<BlastHitRegion> l) {
		BlastHitRegion ret = null;
		int al = 0;
		
		for (BlastHitRegion hit : l) {
			if (hit.getAlignmentLength() > al) {
				ret = hit;
				al  = hit.getAlignmentLength();
			}
		}
		
		return ret;
	}

	protected double getEvalue() {
    	return m_eval_cutoff.getDoubleValue();
    }
    
    protected String getScoringMatrix() {
    	return m_matrix.getStringValue();
    }
    
    protected boolean useLowComplexityFilter() {
    	return m_low_complexity_filter.getBooleanValue();
    }
    
    protected String getOtherArguments() {
    	return m_other.getStringValue().trim();
    }
    
    protected String getDB() {
    	return m_db.getStringValue();
    }
    
    protected int getNumThreads() {
    	return m_threads.getIntValue();
    }
    
    /**
     * Called during {@link addArguments} this method is responsible for adding
     * the name/path of the query file (FASTA format typically). The implementation
     * must handle quoting as it sees fit. By default, quoting is turned on.
     * 
     * This implementation uses just the name rather than the path, since we can rely on
     * the working directory being set to the path of the query database.
     * 
     * @param cmdLine
     */
    public void addQueryDatabase(CommandLine cmdLine) {
    	cmdLine.addArgument(getQueryDatabaseFile().getName());
    }
    
    /**
     * Called during {@link addArguments} this method is responsible for adding
     * the name/path of the BLAST database to search. The implementation
     * must handle quoting as it sees fit. By default, quoting is turned on.
     * 
     * @param cmdLine
     */
    public void addBLASTDatabase(CommandLine cmdLine) {
    	cmdLine.addArgument(getBLASTDatabase());
    }
    
    protected void addArguments(CommandLine cmdLine) {
    	// invoke the BLAST program once all is good
    	cmdLine.addArgument("-db");
    	addBLASTDatabase(cmdLine);
    	cmdLine.addArgument("-query");
    	addQueryDatabase(cmdLine);
    	cmdLine.addArgument("-num_threads");
    	cmdLine.addArgument(String.valueOf(getNumThreads()));
    	cmdLine.addArgument("-outfmt");
    	// turn off automatic quoting as this breaks mac compatibility
    	cmdLine.addArgument(getOutFmtString(m_results.getStringArrayValue()), false);			// tabbed separated values
    	cmdLine.addArgument("-evalue");
    	cmdLine.addArgument(new Double(getEvalue()).toString());
    	if (is_protein_blast()) {
    		cmdLine.addArgument("-matrix");
    		cmdLine.addArgument(m_matrix.getStringValue());
    	}
    	if (useLowComplexityFilter()) {
    		cmdLine.addArgument(is_protein_blast() ? "-seg" : "-dust");
    		cmdLine.addArgument("yes");
    	} else {
    		// NO-OP
    		// we dont add an argument so that the user is free to specify something else via other...
    	}
    	if (m_other.getStringValue().trim().length() > 0) {
    		String[] additional_args = getOtherArguments().split("\\s+");
    		for (String s : additional_args) {
    			cmdLine.addArgument(s);
    		}
    	}
	}

	protected final void make_blastdb(File db) throws InvalidSettingsException, IOException {
    	if (!db.canRead()) {
    		throw new InvalidSettingsException("Unable to read (permissions?): "+db.getAbsolutePath());
    	}
    	
    	File[] phr = new File[] { new File(db.getParent(), db.getName() + ".00.phr"), new File(db.getParent(), db.getName() + ".phr") };
    	File[] pin = new File[] { new File(db.getParent(), db.getName() + ".00.pin"), new File(db.getParent(), db.getName() + ".pin") };
    	File[] psq = new File[] { new File(db.getParent(), db.getName() + ".00.psq"), new File(db.getParent(), db.getName() + ".psq") };
    	
    	if (find_file(phr) && find_file(pin) && find_file(psq)) {
    		logger.info("Found existing blast index for "+db.getName()+", not running makeblastdb again.");
    		return;
    	}
    	// else re-create db...
    	File mkdb = find_blast_program("makeblastdb");
    	// HACK: we determine dbtype based on the blast program configured (blastp ie. protein etc.)
    	String dbtype = is_protein_blast() ? "prot" : "nucl";
    	
    	logger.info("Creating BLAST database for "+db.getName()+"... please be patient...");
    	CommandLine cmdLine = new CommandLine(mkdb);
    	
    	// invoke the BLAST program once all is good
    	cmdLine.addArgument("-dbtype");
    	cmdLine.addArgument(dbtype);
    	// dont use this as it seems to cause database building failures (incorrect duplicate sequence IDs)
    	//cmdLine.addArgument("-hash_index");
    	cmdLine.addArgument("-in");
    	cmdLine.addArgument(db.getName());
    	
    	DefaultExecutor exe = new DefaultExecutor();
    	exe.setExitValues(new int[] {0});	// only 0 is considered success
    	exe.setStreamHandler(new PumpStreamHandler(new InfoLogger(logger), new ErrorLogger(logger)));
    	File cwd = db.getParentFile();
    	logger.info("Setting current directory to: "+cwd.getAbsolutePath());
    	exe.setWorkingDirectory(cwd);		// arbitrary choice
    	
    	logger.info("Running: "+cmdLine.toString());
    	int exitCode = exe.execute(cmdLine);
    	logger.info("got exit code: "+exitCode+" from makeblastdb");
	}

	protected boolean find_file(File[] in) {
		for (File f : in) {
			if (f.exists() && f.canRead() && f.isFile()) 
				return true;
		}
		return false;
	}

	protected boolean is_protein_blast() {
		String conf_prog = m_ncbi_prog.getStringValue().trim().toLowerCase();
    	String dbtype = "prot";
    	if (conf_prog.endsWith("blastn")) {
    		dbtype = "nucl";
    	}
    	return dbtype.equals("prot");
	}

	/**
	 * Find the user-specified BLAST program on the filesystem, returns a <code>File</code> to it or <code>null</code> on failure.
	 * @return
	 */
	protected File find_blast_program() {
		return find_blast_program(m_ncbi_prog.getStringValue());
	}
	
	protected File find_blast_program(String program_to_find) {
		IPreferenceStore prefs = KNIMEUIPlugin.getDefault().getPreferenceStore();
		String install_dir = prefs.getString(PreferenceConstants.PREFS_BLAST_FOLDER);
		logger.info("Got "+install_dir+" as NCBI BLAST+ software location");
		if (!new File(install_dir).isDirectory()) {
			logger.warn("Cannot locate NCBI BLAST+ software!");
		}
    	
		return ExternalProgram.find(install_dir, program_to_find);
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
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void saveSettingsTo(final NodeSettingsWO settings) {
    	m_db.saveSettingsTo(settings);
    	m_q_db.saveSettingsTo(settings);
    	m_eval_cutoff.saveSettingsTo(settings);
    	m_ncbi_prog.saveSettingsTo(settings);
    	m_threads.saveSettingsTo(settings);
    	
    	m_other.saveSettingsTo(settings);
    	m_low_complexity_filter.saveSettingsTo(settings);
    	m_matrix.saveSettingsTo(settings);
    	m_results.saveSettingsTo(settings);
    	m_what_annotations.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_db.loadSettingsFrom(settings);
    	m_q_db.loadSettingsFrom(settings);
    	m_eval_cutoff.loadSettingsFrom(settings);
    	m_ncbi_prog.loadSettingsFrom(settings);
    	m_threads.loadSettingsFrom(settings);
    	
    	m_other.loadSettingsFrom(settings);
    	m_low_complexity_filter.loadSettingsFrom(settings);
    	m_matrix.loadSettingsFrom(settings);
    	if (settings.containsKey(CFGKEY_RESULTS)) {
    		m_results.loadSettingsFrom(settings);
    	} else {
    		m_results.setStringArrayValue(getResultDefaults());
    	}
    	if (settings.containsKey(CFGKEY_ANNOTATE_WHAT)) {
        	m_what_annotations.loadSettingsFrom(settings);
    	} else {
    		m_what_annotations.setStringValue(ANNOTATION_GROUP[0]);
    	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_db.validateSettings(settings);
    	m_q_db.validateSettings(settings);
    	m_eval_cutoff.validateSettings(settings);
    	m_ncbi_prog.validateSettings(settings);
    	m_threads.validateSettings(settings);
    	
    	m_other.validateSettings(settings);
    	m_low_complexity_filter.validateSettings(settings);
    	m_matrix.validateSettings(settings);
    	if (settings.containsKey(CFGKEY_RESULTS)) {
    		m_results.validateSettings(settings);
    	}
    	if (settings.containsKey(CFGKEY_ANNOTATE_WHAT)) {
    		m_what_annotations.validateSettings(settings);
    	}
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

	protected String getBLASTDatabase() {
		return m_db.getStringValue();
	}
	
	protected String getQueryDatabase() {
		return m_tmp_fasta.getAbsolutePath();
	}

	protected final File getQueryDatabaseFile() {
		return new File(getQueryDatabase());
	}
}

