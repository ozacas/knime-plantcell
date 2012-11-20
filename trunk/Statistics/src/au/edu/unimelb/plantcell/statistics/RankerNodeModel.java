package au.edu.unimelb.plantcell.statistics;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


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

    private SettingsModelString m_ties     = new SettingsModelString(CFGKEY_TIE_METHOD, "average");
    private SettingsModelString m_nan      = new SettingsModelString(CFGKEY_NaN_METHOD, "fixed");
    private SettingsModelString m_rank_col = new SettingsModelString(CFGKEY_RANK_COLUMN, "");
    
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
    	int seq_idx = inData[0].getSpec().findColumnIndex(m_rank_col.getStringValue());
    	if (seq_idx < 0) {
    		throw new InvalidSettingsException("Unable to find column (re-configure?): "+m_rank_col.getStringValue());
    	}
    	RowIterator it = inData[0].iterator();
    	double[] vec = new double[inData[0].getRowCount()];
    	int len = 0;
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
    	exec.setProgress("Computing rank vector... please wait a few moments");
    	DenseBitVector bv = null;
    	
    	if (!is_remove) {
	    	while (it.hasNext()) {
	    		DataRow r = it.next();
	    		DataCell c = r.getCell(seq_idx);
	    		if (c == null || c.isMissing()) {
	    			vec[len++] = Double.NaN;
	    			continue;
	    		}
	    		vec[len++] = ((DoubleValue) c).getDoubleValue();
	    	}
    	} else {
    		bv = new DenseBitVector(inData[0].getRowCount());		// bit set means that the data has been removed
    	
    		int idx = 0;
    		while (it.hasNext()) {
    			DataCell c = it.next().getCell(seq_idx);
    			if (c == null || c.isMissing()) {
    				bv.set(idx++);
    				continue;
    			}
    			DoubleValue dv = (DoubleValue) c;
    			double d = dv.getDoubleValue();
    			if (!Double.isNaN(d)) {
    				vec[len++] = d;
    			} else {
    				bv.set(idx);
    			}
    			idx++;
    		}
    		double[] tmp = new double[len];
    		System.arraycopy(vec, 0, tmp, 0, len);
    		vec = tmp;
    	}
    
    	
    	// next invoke commons math
    	exec.checkCanceled();
    	
    	NaNStrategy  s_nan  = NaNStrategy.valueOf(nan_strategy.toUpperCase());
    	TiesStrategy s_ties = TiesStrategy.valueOf(m_ties.getStringValue().trim().toUpperCase());
    	// NB: we dont use nan_strategy for the log message (see above comment)
    	logger.info("Using "+m_nan.getStringValue().toUpperCase()+" NaN strategy and "+s_ties+" ties strategy for ranking.");
    	NaturalRanking nr = new NaturalRanking(s_nan, s_ties);
    	double[] ret = nr.rank(vec);
    	exec.checkCanceled();
    
    	// write out rank values
    	BufferedDataContainer c = exec.createDataContainer(make_output_spec(inData[0].getSpec()));
    	it = inData[0].iterator();
    	int idx = 0;
    	int cnt = 0;
    	while (it.hasNext()) {
    		DataRow     r = it.next();
    		DataCell cell = null;
    		if (bv != null) {
				cell = DataType.getMissingCell();
    			if (bv.get(cnt++)) {
    				continue;
    			} else {
    				cell = new DoubleCell(ret[idx++]);
    			}
    		} else {
    			cell = new DoubleCell(ret[idx++]);
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

