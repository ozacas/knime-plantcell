package au.edu.unimelb.plantcell.algorithms.StringFinder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;


/**
 * This is the model implementation of StringMatcher.
 * Used to match a set of strings against a column in the input table. All input columns are preserved, but the matches are appended to the table.
 *
 * @author Andrew Cassin
 */
@SuppressWarnings("unused")
public class FindGlobalNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Find (global)");
    
    /**
     *  Configuration keys and default values for model values
     */
   static final String  CFG_AS_REGEXP      = "As regular expression?";
   static final boolean DEF_AS_REGEXP      = false;
   static final String  CFG_ONLY_ROWS      = "Keep only matching rows?";
   static final boolean DEF_ONLY_ROWS      = false;
   static final String  CFG_INPUT_STRINGS  = "Match columns";
   static final String  CFG_MATCHER_STRINGS="Strings to match";
   static final String  CFG_OUTPUT_FORMAT  = "Required Output";

   private final SettingsModelBoolean m_as_regexp   = new SettingsModelBoolean(CFG_AS_REGEXP, DEF_AS_REGEXP);
   private final SettingsModelString  m_input       = new SettingsModelString(CFG_INPUT_STRINGS, "");
   private final SettingsModelString  m_matcher     = new SettingsModelString(CFG_MATCHER_STRINGS, "");
   private final SettingsModelBoolean m_keep_only   = new SettingsModelBoolean(CFG_ONLY_ROWS, DEF_ONLY_ROWS);
   private final SettingsModelStringArray m_outformat   = new SettingsModelStringArray(CFG_OUTPUT_FORMAT, new String[] { "Matches (collection)" } );
   
    // internal state during execute()
    private Pattern[] m_matchers;
    private int m_match_col_idx, m_cnt;
    private DenseBitVector m_bv;
    private ArrayList<String> m_matches;
    private ArrayList<Extent> m_match_pos;
	private boolean m_want_matches;
    private boolean m_want_pos;
    private boolean m_unique_count;
    private boolean m_unique_matches;
    private boolean m_match_count;
    private boolean m_start_pos;
    private boolean m_extents;
    private boolean m_overall_extent;
    private boolean m_startpos_density;
    private boolean m_numposmatches;
    private boolean m_coverage;
    private boolean m_search_strings;
    private boolean m_highlight_single_colour;
    private boolean m_annotate;
    private ArrayList<MatchReporter> m_reporters;
    private HashMap<Integer,String>  m_orig_patterns;
    private List<String> m_matching_search_strings;		  // only created if m_search_strings is true (performance)
    private HashMap<String,Integer> m_search_string_freq; // only created if m_search_strings is true
    
    /**
     * Constructor for the node model.
     */
    protected FindGlobalNodeModel() {
        // two incoming ports and one outgoing port
        this(2, 1);
    }

    protected FindGlobalNodeModel(int inPort, int outPort) {
    	super(inPort, outPort);
    	
    	reset();
    }
    
    protected String getSearchStringColumn() {
    	return m_matcher.getStringValue();
    }
    
    protected String getInputStringColumn() {
    	return m_input.getStringValue();
    }
    
    protected String[] getWantedOutputs() {
    	return m_outformat.getStringArrayValue();
    }
    
    protected final void initInputColumnIndex(DataTableSpec spec) throws InvalidSettingsException {
    	assert(spec != null);
    	int ret = spec.findColumnIndex(getInputStringColumn());
    	if (ret < 0) {
    		throw new InvalidSettingsException("Unable to locate column: "+getInputStringColumn()+ ": reconfigure?");
    	}
    	m_match_col_idx = ret;
    }
    
    protected DataCell getInputCell(DataRow r) {
    	assert(r != null);
    	return r.getCell(m_match_col_idx);
    }
    
    protected int getSearchColumnIndex(DataTableSpec spec) throws InvalidSettingsException {
    	assert(spec != null);
    	int ret = spec.findColumnIndex(getSearchStringColumn());
    	if (ret < 0) {
    		throw new InvalidSettingsException("Unable to locate column: "+getSearchStringColumn()+ ": reconfigure?");
    	}
    	return ret;
    }
    
    protected void warnIfInfeasibleSearch(BufferedDataTable[] inData) throws InterruptedException {
    	 if (inData[0].getRowCount() * inData[1].getRowCount() > 10000*1000*1000) {
    		logger.warn("***************** WARNING *************");
         	logger.warn("Probably infeasible search (will take a long time to complete): "+inData[0].getRowCount()+" "+inData[1].getRowCount());
         	logger.warn("Continuing in 5 seconds...");
         	Thread.sleep(5 * 1000);
    	 }
    }
    
    protected void setMatchers(Pattern[] matchers) {
    	assert(matchers != null);
    	m_matchers = matchers;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	logger.info("Starting search... ");
    	warnIfInfeasibleSearch(inData);
    	
    	// locate input data and build data structures
        initInputColumnIndex(inData[0].getSpec());
        int matching_col_idx= getSearchColumnIndex(inData[1].getSpec());
        
        // inData[0] is the data to be matched, inData[1] is the strings to use for matching (or RE's)
    	DataTableSpec new_cols = make_output_columns(inData[0].getDataTableSpec(), getWantedOutputs());
    													
        setMatchers(compile_patterns(inData[1], matching_col_idx));
      
        // create output table
        MyDataContainer c = new MyDataContainer(exec.createDataContainer(new_cols), "hit");
        RowIterator it = inData[0].iterator();
        int     n_rows = inData[0].getRowCount();
        int       done = 0;
        
        while (it.hasNext()) {
        	DataRow r = it.next();
        	match_string(getInputCell(r), c, r);
        	done++;
        	if (done % 10 == 0) {
        		exec.checkCanceled();
        		exec.setProgress(((double)done/n_rows), "");
        	}
        }
        return new BufferedDataTable[]{c.close()};
    }

    /**
     * Does nothing unless <code>m_search_strings</code> is true ie. user wants this output
     * @param orig_patterns
     */
    protected void setOriginalPatterns(Map<Integer,String> orig_patterns) {
    	if (m_search_strings)
			m_orig_patterns.putAll(orig_patterns);
    }
    
	protected void match_string(DataCell str_cell, MyDataContainer c, DataRow r) throws Exception {
		assert(str_cell != null && c != null && r != null);
		
		String str = str_cell.toString();
		//int len = str.length();
		m_bv = new DenseBitVector(str.length());
		m_cnt = 0;
		m_matches.clear();
		m_match_pos.clear();
		if (m_search_strings) {
			if (m_matching_search_strings == null)
				m_matching_search_strings = new ArrayList<String>();
			if (m_search_string_freq == null)
				m_search_string_freq = new HashMap<String,Integer>();
			m_matching_search_strings.clear();
			m_search_string_freq.clear();
		}
		for (int i=0; i<m_matchers.length; i++) {
    		Matcher m = m_matchers[i].matcher(str);
    		int base = 0;
    		while (m.find(base)) {
    			m_cnt++;
    			m_matches.add(m.group(0));
    			int start = m.start();
    			int end   = m.end();
    			m_bv.set(start, end);
    			m_match_pos.add(new Extent(start,end));
    			base      = m.start() + 1;
    			if (m_search_strings) {
    				String pat = m_orig_patterns.get(new Integer(i));
    				m_matching_search_strings.add(pat);
    				if (m_search_string_freq.containsKey(pat)) {
    					Integer j = m_search_string_freq.get(pat);
    					m_search_string_freq.put(pat, new Integer(j.intValue()+1));
    				} else {
    					m_search_string_freq.put(pat, new Integer(1));
    				}
    			}
    		}
    	}
		
		int n_rpt = m_reporters.size();
		boolean hits_only = m_keep_only.getBooleanValue();
		if (n_rpt > 0 && (!hits_only || (hits_only && m_cnt > 0))) {
			// add columns for each reported configured by the user
			int idx=0;
			DataCell[] cells = new DataCell[c.getTableSpec().getNumColumns()];
			for (MatchReporter rpt : m_reporters) {
				cells[idx++] = rpt.report(this, str_cell);
			}
			// add input cells from current row (exception: sequence annotation)
			int done = 0;
			int i = 0;
			while (done < (cells.length - n_rpt)) {
				// annotated sequence cells which are searched are not copied from input row
				if (m_annotate && i == m_match_col_idx)
					continue;
				// but every other cell is
				cells[idx++] = r.getCell(i++);
				done++;
			}
			c.addRow(cells);
		}
	}
	
    protected DataTableSpec make_output_columns(DataTableSpec inSpec, String[] wanted) throws InvalidSettingsException {
    	DataType list_of_string_type = ListCell.getCollectionType(StringCell.TYPE);
    	ArrayList<DataColumnSpec> cols = new ArrayList<DataColumnSpec>();
    	
    	reset();
    	initInputColumnIndex(inSpec);

		for (String want : wanted) {
			if (want.equals("Matches (collection)")) {
				cols.add(new DataColumnSpecCreator("Matches", ListCell.getCollectionType(StringCell.TYPE)).createSpec());
				m_want_matches = true;
				m_reporters.add(new MatchesReporter(false));
			} else if (want.startsWith("Match Positions")) {
				m_want_pos = true;
				cols.add(new DataColumnSpecCreator("Match Positions", ListCell.getCollectionType(StringCell.TYPE)).createSpec());
				m_reporters.add(new MatchPositionsReporter());
			} else if (want.equals("Unique Match Count")) {
				m_unique_count = true;
				cols.add(new DataColumnSpecCreator("Unique Match Count", IntCell.TYPE).createSpec());
				m_reporters.add(new UniqueMatchReporter(true));
			} else if (want.equals("Unique Matches")) {
				m_unique_matches = true;
				cols.add(new DataColumnSpecCreator("Unique Matches", SetCell.getCollectionType(StringCell.TYPE)).createSpec());
				m_reporters.add(new UniqueMatchReporter(false));
			} else if (want.equals("Unique Match Distribution")) {
				m_unique_matches = true;
				cols.add(new DataColumnSpecCreator("Unique Match Distribution", SetCell.getCollectionType(StringCell.TYPE)).createSpec());
				m_reporters.add(new UniqueMatchReporter(want));
			} else if (want.equals("Match Count")) {
				m_match_count = true;
				cols.add(new DataColumnSpecCreator("Match Count", IntCell.TYPE).createSpec());
				m_reporters.add(new MatchesReporter(true));
			} else if (want.startsWith("Start Positions")) {
				m_start_pos = true;
				cols.add(new DataColumnSpecCreator("Match Start Positions", ListCell.getCollectionType(IntCell.TYPE)).createSpec());
				m_reporters.add(new StartPositionsReporter());
			} else if (want.startsWith("Extent of matches")) {
				m_extents = true;
				cols.add(new DataColumnSpecCreator("Match Lengths", ListCell.getCollectionType(IntCell.TYPE)).createSpec());
				m_reporters.add(new MatchLengthsReporter());
			} else if (want.startsWith("Highlight Matches (HTML, single colour)")) {
				m_highlight_single_colour = true;
				cols.add(new DataColumnSpecCreator("Highlighted (HTML, single colour)", StringCell.TYPE).createSpec());
				m_reporters.add(new HighlightMatchReporter(true));	// true: use single colour for all matches
			} else if (want.startsWith("Match Extent")) {
				m_overall_extent = true;
				// NB: KNIME wont allow duplicate column names, so we must adjust the title depending on want
				String title = "Matched Region";
				if (want.endsWith("(substring)")) {
					title += " (substring)";
				} else {
					title += " (position)";
				}
				cols.add(new DataColumnSpecCreator(title, StringCell.TYPE).createSpec());
				m_reporters.add(new ExtentReporter(want.endsWith("(substring)")));
			} else if (want.startsWith("Patterns (successful, distinct") || want.startsWith("Pattern distribution")) {
				m_search_strings = true;
				String title = "Successful Patterns (distinct)";
				if (want.startsWith("Pattern distribution")) 
					title = "Successful Pattern Frequency";
				cols.add(new DataColumnSpecCreator(title, SetCell.getCollectionType(StringCell.TYPE)).createSpec());
				m_orig_patterns = new HashMap<Integer,String>();
				m_search_string_freq = new HashMap<String,Integer>();
				m_reporters.add(new SearchStringReporter(want));
			} else if (want.startsWith("Non-overlapping matches")) {
				cols.add(new DataColumnSpecCreator("Non-overlapping matches", list_of_string_type).createSpec());
				m_reporters.add(new NonOverlappingMatchesReporter(false));
			} else if (want.startsWith("Non-overlapping match count")) {
				cols.add(new DataColumnSpecCreator("Non-overlapping match count", IntCell.TYPE).createSpec());
				m_reporters.add(new NonOverlappingMatchesReporter(true));
			} else if (want.startsWith("Match Start Position Density")) {
				m_startpos_density = true;
				cols.add(new DataColumnSpecCreator("Match Start Position Density", DenseBitVectorCell.TYPE).createSpec());
				m_reporters.add(new StartDensityPositionReporter());
			} else if (want.startsWith("Match Position Density")) {
				cols.add(new DataColumnSpecCreator("Match Position Density", DenseBitVectorCell.TYPE).createSpec());
				m_reporters.add(new MatchDensityPositionReporter());
			} else if (want.startsWith("Number of matches per position")) {
				m_numposmatches = true;
				cols.add(new DataColumnSpecCreator("Match count by string length", ListCell.getCollectionType(IntCell.TYPE)).createSpec());
				m_reporters.add(new MatchesPerPositionReporter());
			} else if (want.startsWith("Input String Coverage")) {
				cols.add(new DataColumnSpecCreator("Input Coverage (%)", DoubleCell.TYPE).createSpec());
				m_coverage = true;
				m_reporters.add(new CoverageReporter());
			} else if (want.startsWith("Annotate sequences")) {
				if (m_match_col_idx >= 0) {		
					DataColumnSpec seq_col = inSpec.getColumnSpec(m_match_col_idx);
					if (!seq_col.getType().isCompatible(SequenceValue.class)) {
						throw new InvalidSettingsException("Must search sequences to annotate them!");
					}
					DataColumnSpecCreator dcsc = new DataColumnSpecCreator("Annotated Sequence with (incl. match track)", SequenceCell.TYPE);
					// HACK TODO: cohesion violation using m_match_col_idx here...
					SequenceAnnotatorReporter rptr = new SequenceAnnotatorReporter();
					TrackColumnPropertiesCreator tc = new TrackColumnPropertiesCreator(
															seq_col.getProperties(), 
															rptr.getTrack());
					dcsc.setProperties(tc.getProperties());
					cols.add(dcsc.createSpec());
					m_annotate = true;
					m_reporters.add(rptr);
				}
			} else {
				throw new InvalidSettingsException("Unknown match datum: "+want);
			}
		}
	
		// now add columns from input port to output table (exception: sequence column if any)
		for (int i=0; i<inSpec.getNumColumns(); i++) {
			if (m_annotate && i==m_match_col_idx)
				continue;
			cols.add(inSpec.getColumnSpec(i));
		}
		return new DataTableSpec(cols.toArray(new DataColumnSpec[0]));
	}
    
    
    public int getNumMatches() {
    	return (m_matches != null) ? m_matches.size() : 0;
    }
    
    
	protected Pattern[] compile_patterns(BufferedDataTable in_data, int col_idx) throws Exception {
    	int n_rows = in_data.getRowCount();
    	if (n_rows < 1) {
    		throw new Exception("No regular expressions provided to match!");
    	}
    	Pattern[] ret = new Pattern[n_rows];
    	RowIterator i = in_data.iterator();
    	int idx = 0;
    	String s = null;
    	try {
    		Map<Integer,String> orig_patterns = new HashMap<Integer,String>();
    		while (i.hasNext()) {
    			DataRow  r = i.next();
    			DataCell c = r.getCell(col_idx);
    			if (c == null || c.isMissing())
    				throw new InvalidSettingsException("Missing value for pattern not permitted!");
    			s = c.toString();
    			if (s.length() < 1)
    				throw new InvalidSettingsException("Empty value for pattern not permitted!");
    			logger.info("Compiling pattern: "+s);
    			orig_patterns.put(new Integer(idx), s);
    			ret[idx++] = Pattern.compile(s);
    		}
    		
    		setOriginalPatterns(orig_patterns);
    	} catch (Exception e) {
    		logger.warn("Unable to compile: "+s+" as a regular expression - bad syntax?");
    		throw(e);
    	}
    	
    	return ret;
    }
      
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    	m_matches                 = new ArrayList<String>();
        m_match_pos               = new ArrayList<Extent>();
        m_reporters               = new ArrayList<MatchReporter>();
        m_search_string_freq      = new HashMap<String,Integer>();
        m_matching_search_strings = new ArrayList<String>();
        m_orig_patterns           = new HashMap<Integer,String>();
        m_reporters               = new ArrayList<MatchReporter>();
        m_bv                      = null;
        m_matchers                = null;
        m_want_matches            = false;
    	m_want_pos                = false;
    	m_unique_count            = false;
    	m_unique_matches          = false;
    	m_match_count             = false;
    	m_start_pos               = false;
    	m_extents                 = false;
    	m_overall_extent          = false;
    	m_startpos_density        = false;
    	m_numposmatches           = false;
    	m_coverage                = false;
    	m_search_strings          = false;
    	m_highlight_single_colour = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
    	DataTableSpec spec = make_output_columns(inSpecs[0], m_outformat.getStringArrayValue());
    	//logger.info("Configured " + spec.getNumColumns()+" columns in output table.");
		return new DataTableSpec[]{spec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_as_regexp.saveSettingsTo(settings);
        m_input.saveSettingsTo(settings);
        m_matcher.saveSettingsTo(settings);
        m_outformat.saveSettingsTo(settings);
        m_keep_only.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_as_regexp.loadSettingsFrom(settings);
        m_input.loadSettingsFrom(settings);
        m_matcher.loadSettingsFrom(settings);
        m_outformat.loadSettingsFrom(settings);
        m_keep_only.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_as_regexp.validateSettings(settings);
        m_input.validateSettings(settings);
        m_matcher.validateSettings(settings);
        m_outformat.validateSettings(settings);
        m_keep_only.validateSettings(settings);
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

	public DenseBitVector getResultsBitVector() {
		return m_bv;
	}
	
	List<Extent> getMatchPos() {
		return m_match_pos;
	}

	public List<String> getMatches() {
		return m_matches;
	}

	public List<String> getMatchingPatterns() {
		assert(m_search_strings);		// logical error: these results are only available when chosen by the user
		return m_matching_search_strings;
	}
	
	public Map<String,Integer> getMatchPatternFrequency() {
		assert(m_search_strings);
		return m_search_string_freq;
	}
}

