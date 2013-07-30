package au.edu.unimelb.plantcell.statistics;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.RankingAlgorithm;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.vector.bitvector.DenseBitVector;
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
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * This is the model implementation of Ranker.
 * Basic descriptive and inferential statistics support built using the apache commons math v3 library.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class RankerNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Ranker");
        
    public static final String CFGKEY_TIE_METHOD   = "tie-method";
    public static final String CFGKEY_NaN_METHOD   = "nan-method";
    public static final String CFGKEY_RANK_COLUMN  = "ranking column";
    public static final String CFGKEY_GROUPBY_COLUMN = "groupby column";

    private SettingsModelString m_ties     = new SettingsModelString(CFGKEY_TIE_METHOD, "average");
    private SettingsModelString m_nan      = new SettingsModelString(CFGKEY_NaN_METHOD, "fixed");
    private SettingsModelString m_rank_col = new SettingsModelString(CFGKEY_RANK_COLUMN, "");
    private SettingsModelColumnName m_groupby = new SettingsModelColumnName(CFGKEY_GROUPBY_COLUMN, "");
    
    /**
     * Constructor for the node model.
     */
    protected RankerNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	logger.info("Ranking data based on "+m_rank_col.getStringValue());
    	
    	if (inData[0].getRowCount() < 1) {
    		throw new InvalidSettingsException("No rows to rank!");
    	}
    	int rank_idx = inData[0].getSpec().findColumnIndex(m_rank_col.getStringValue());
    	if (rank_idx < 0) {
    		throw new InvalidSettingsException("Unable to find column (re-configure?): "+m_rank_col.getStringValue());
    	}
    	int groupby_idx = -1;
    	if (m_groupby.getColumnName() != null) {
    		groupby_idx = inData[0].getSpec().findColumnIndex(m_groupby.getColumnName());
    		if (groupby_idx < 0)
    			throw new InvalidSettingsException("Unable to find column: "+m_groupby.getColumnName()+" - reconfigure?");
    		logger.info("Separate ranks for each value of "+m_groupby.getColumnName());
    	}
    	
    	RowIterator it = inData[0].iterator();
    	String nan_strategy = m_nan.getStringValue();
    	boolean is_remove = nan_strategy.toLowerCase().startsWith("remove");
    	if (is_remove) {
    		/**
    		 * To integrate with the KNIME platform, we remove NaN's prior to invoking NaturalRanking.
    		 * So here we adjust the NaN strategy since the NaNs will be long gone...
    		 */
    		nan_strategy = "FIXED";
    	}
    	
    	// first build up the vec
    	exec.setProgress("Computing rank vectors... please wait.");
    	
		HashMap<String, DoubleArrayList> ranks = new HashMap<String, DoubleArrayList>(); 
    	DenseBitVector bv = new DenseBitVector(inData[0].getRowCount());		// rows to not be in the output (eg. missing values) have bit set
    	
    	// here we use the row order to do the separation into separate vectors
    	int idx = 0;
    	for (DataRow r : inData[0]) {
    		DataCell c = r.getCell(rank_idx);
    		
    		if ((c == null || c.isMissing()) && is_remove) {
    			bv.set(idx++);
    			continue;
    		}
    		idx++;
    		
    		// ensure rank collection exists for the given rank
    		String rank = (groupby_idx >= 0) ? r.getCell(groupby_idx).toString() : "<None>";
    		if (!ranks.containsKey(rank)) {
    			ranks.put(rank, new DoubleArrayList((int) (inData[0].getRowCount() * 0.01)));		// try to avoid extending the array by pre-guessing the expected size
    		}
    		
    		DoubleArrayList      vec = ranks.get(rank);
    		double           d = (c instanceof DoubleValue) ? ((DoubleValue) c).getDoubleValue() : ((IntValue)c).getIntValue();
    		vec.add(d);
    	}
    	
    	// next invoke commons math
    	exec.checkCanceled();
    
    	// NB: we dont use nan_strategy for the log message (see above comment)
    	logger.info("Using "+m_nan.getStringValue().toUpperCase()+" NaN strategy and "+m_ties.getStringValue()+" ties strategy for ranking.");
    	for (String s : ranks.keySet()) {
    		logger.info("Ranking group "+s);
	    	RankingAlgorithm nr = make_ranker(nan_strategy.toUpperCase().trim(), m_ties.getStringValue().trim().toUpperCase());
	    	
	    	double[] ret = nr.rank(ranks.get(s).toDoubleArray());
	    	// once the ranking is done, we dont need the original values anymore, so we just replace the existing vector in memory
	    	ranks.put(s, new DoubleArrayList(ret));
	    	exec.checkCanceled();
    	}
    
    	// write out rank values
    	BufferedDataContainer c = exec.createDataContainer(make_output_spec(inData[0].getSpec()));
    	it = inData[0].iterator();
    	HashMap<String,Integer> idx_by_rank = new HashMap<String,Integer>();			// keep track of which vector element to report in each group
    	
    	int cnt = 0;
    	idx = 0;
    	while (it.hasNext()) {
    		DataRow     r = it.next();
    		DataCell cell = null;
    		String      rank = (groupby_idx >= 0) ? r.getCell(groupby_idx).toString() : "<None>";
    		Integer     i  = idx_by_rank.get(rank);
    		if (i == null) {
    			i = new Integer(0);
    			idx_by_rank.put(rank, i);
    		}
    		if (bv != null) {
				cell = DataType.getMissingCell();
    			if (bv.get(cnt++)) {
    				continue;
    			} else {
    				DoubleArrayList vec = ranks.get(rank);
    				cell = new DoubleCell(vec.getDouble(i.intValue()));
    				idx_by_rank.put(rank, new Integer(i.intValue()+1));
    			}
    		} else {
    			DoubleArrayList vec = ranks.get(rank);
    			cell = new DoubleCell(vec.getDouble(i.intValue()));
    			idx_by_rank.put(rank, new Integer(i.intValue()+1));
    		}
    		c.addRowToTable(new JoinedRow(r, new DefaultRow(r.getKey().getString(), cell)));
    		
    		if (idx % 1000 == 0) {
    			exec.checkCanceled();
    			exec.setProgress(idx / inData[0].getRowCount());
    		}
    	}
        c.close();
        BufferedDataTable out = c.getTable();
        return new BufferedDataTable[]{out};
    }

    private RankingAlgorithm make_ranker(String nan_strategy, String ties_strategy) {
    	NaNStrategy  s_nan  = NaNStrategy.valueOf(nan_strategy);
    	boolean my_ties = false;
    	if (ties_strategy.equals("MINIMUM CONSECUTIVE")) {
    		ties_strategy = "MINIMUM";
    		my_ties = true;
    	}
    	TiesStrategy s_ties = TiesStrategy.valueOf(ties_strategy);
    	return my_ties ? new MyNaturalRanking() : new NaturalRanking(s_nan, s_ties);
	}

	private DataTableSpec make_output_spec(DataTableSpec inSpec) throws IllegalArgumentException {
    	DataColumnSpec[] cols = new DataColumnSpec[1];
    	String colName = "";
    	if (this.m_rank_col.getStringValue().length() > 0) {
    		colName = "("+m_rank_col.getStringValue()+")";
    	}
    	cols[0] = new DataColumnSpecCreator("Rank"+colName, DoubleCell.TYPE).createSpec();
    	
		return new DataTableSpec(inSpec, new DataTableSpec(cols));
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
    	return new DataTableSpec[] { make_output_spec(inSpecs[0]) };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_ties.saveSettingsTo(settings);
    	m_nan.saveSettingsTo(settings);
    	m_rank_col.saveSettingsTo(settings);
    	m_groupby.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_ties.loadSettingsFrom(settings);
    	m_nan.loadSettingsFrom(settings);
    	m_rank_col.loadSettingsFrom(settings);
    	if (settings.containsKey(CFGKEY_GROUPBY_COLUMN)) {
    		m_groupby.loadSettingsFrom(settings);
    	} else {
    		m_groupby.setStringValue("");
    	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_ties.validateSettings(settings);
    	m_nan.validateSettings(settings);
    	m_rank_col.validateSettings(settings);
    	if (settings.containsKey(CFGKEY_GROUPBY_COLUMN)) {
    		m_groupby.validateSettings(settings);
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

}

