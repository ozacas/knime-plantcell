package au.edu.unimelb.plantcell.algorithms.StringFinder;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.collection.ListDataValue;
import org.knime.core.data.collection.SetDataValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import au.edu.unimelb.plantcell.core.MyDataContainer;


/**
 * This is the model implementation of StringMatcher.
 * Used to match a set of strings against a column in the input table. All input columns are preserved, but the matches are appended to the table.
 *
 * @author Andrew Cassin
 */
public class FindLocalNodeModel extends FindGlobalNodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Find (local)");
    
   
    /**
     * Constructor for the node model.
     */
    protected FindLocalNodeModel() {
        // one incoming port and one outgoing port
        super(1, 1);
    }
   
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	logger.info("Starting independent search for each row... ");
    	warnIfInfeasibleSearch(inData);
    	
    	// locate input data and build data structures
        initInputColumnIndex(inData[0].getSpec());
        int   matching_col_idx = getSearchColumnIndex(inData[0].getSpec());
        
        // inData[0] is the data to be matched, inData[1] is the strings to use for matching (or RE's)
    	DataTableSpec new_cols = make_output_columns(inData[0].getDataTableSpec(), getWantedOutputs());
    													
        // create output table
        MyDataContainer c = new MyDataContainer(exec.createDataContainer(new_cols), "hit");
        RowIterator it = inData[0].iterator();
        int     n_rows = inData[0].getRowCount();
        int       done = 0;
        logger.info("Searching "+n_rows+" rows.");
        while (it.hasNext()) {
        	DataRow r = it.next();
        	setMatchers(compile_patterns(r.getCell(matching_col_idx), inData[0].getSpec().getRowColor(r).getColor()));
        	match_string(getInputCell(r), c, r);
        	done++;
        	if (done % 10 == 0) {
        		exec.checkCanceled();
        		exec.setProgress(((double)done/n_rows));
        	}
        }
        return new BufferedDataTable[]{c.close()};
    }
    
    /**
     * Computes the set of patterns to search and returns it. The patterns are unique values from the collection
     * cell (set or list). Will throw if the data is unsuitable for searching.
     * 
     * @param cell
     * @return
     * @throws Exception
     */
    private Pattern[] compile_patterns(DataCell cell, Color colour) throws Exception {
		if (cell == null || cell.isMissing()) {
			throw new InvalidSettingsException("Search strings cannot be missing!");
		}
		if (!cell.getType().isCollectionType()) {
			throw new InvalidSettingsException("Set or list of search strings required");
		}
		Iterator<DataCell> it = null;
		if (cell instanceof SetDataValue) {
			it = ((SetDataValue)cell).iterator();
		} else if (cell instanceof ListDataValue) {
			it = ((ListDataValue)cell).iterator();
		}
		ArrayList<Pattern> ret = new ArrayList<Pattern>();
		HashSet<String> done = new HashSet<String>();
		String s = "";
		int idx = 0;
    	try {
    		Map<Integer,String> orig_patterns = new HashMap<Integer,String>();
    		m_patterns2colours = new HashMap<String,Color>();
    		while (it.hasNext()) {
    			DataCell c = it.next();
    			if (c == null || c.isMissing())
    				throw new InvalidSettingsException("Missing value for pattern not permitted!");
    			s = c.toString();
    			if (done.contains(s))		// NB: only search each distinct pattern once
    				continue;
    			if (s.length() < 1)
    				throw new InvalidSettingsException("Empty value for pattern not permitted!");
    			//logger.info("Compiling pattern: "+s);
    			orig_patterns.put(new Integer(idx++), s);
    			ret.add(Pattern.compile(s));
    			done.add(s);
    			m_patterns2colours.put(s, colour);
    		}
    		setOriginalPatterns(orig_patterns);
    	} catch (Exception e) {
    		logger.warn("Unable to compile: "+s+" as a regular expression - bad syntax?");
    		throw(e);
    	}
    	
    	return ret.toArray(new Pattern[0]);
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void warnIfInfeasibleSearch(BufferedDataTable[] inData) throws InterruptedException {
    	// NO-OP: since only a few patterns are likely per row, we disable this for now
    }
 
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	super.saveSettingsTo(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
    	super.loadValidatedSettingsFrom(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    	super.validateSettings(settings);
    }
}

