package au.edu.unimelb.plantcell.statistics;

import java.io.File;
import java.io.IOException;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;


/**
 * This is the model implementation of Ranker.
 * Basic descriptive and inferential statistics support built using the apache commons math v3 library.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class DescriptiveStatisticsNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Descriptive Stats");
        
    public static final String CFGKEY_COLUMNS   = "columns-to-process";
   
    private SettingsModelFilterString m_cols = new SettingsModelFilterString(CFGKEY_COLUMNS);
    
    
    /**
     * Constructor for the node model.
     */
    protected DescriptiveStatisticsNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	int n_samples = m_cols.getIncludeList().size();
    	if (n_samples < 1)
    		throw new InvalidSettingsException("No samples to process!");
    	logger.info("Computing descriptive statistics on "+n_samples+" columns (samples).");
    	
    	int[] idx = new int[n_samples];
    	SummaryStatistics[] idx_stats = new SummaryStatistics[n_samples];
    	Kurtosis[] k_stats   = new Kurtosis[n_samples];
    	Skewness[] skew_stats = new Skewness[n_samples];
    	
    	for (int i=0; i<n_samples; i++) {
    		String colName = m_cols.getIncludeList().get(i);
    		idx[i] = inData[0].getSpec().findColumnIndex(colName);
    		if (idx[i] < 0) {
    			throw new InvalidSettingsException("Unable to find column: "+colName+" - reconfigure?");
    		}
    		idx_stats[i] = new SummaryStatistics();
    		k_stats[i] = new Kurtosis();
    		k_stats[i].clear();
    		skew_stats[i] = new Skewness();
    		skew_stats[i].clear();
    	}
    	
    	RowIterator it = inData[0].iterator();
    	int done = 0;
    	while (it.hasNext()) {
    		DataRow r = it.next();
    		try {
	    		for (int i=0; i<n_samples; i++) {
	    			SummaryStatistics ss = idx_stats[i];
	    			DataCell c = r.getCell(idx[i]);
	    			if (c == null || c.isMissing()) 
	    				continue;
	    			double d = Double.parseDouble(c.toString());
	    			ss.addValue(d);
	    			k_stats[i].increment(d);
	    			skew_stats[i].increment(d);
	    		}
    		} catch (NumberFormatException e) {
    			logger.warn("Unable to process number from row "+r.getKey().getString()+", ignoring rest of row. Results may be inaccurate!");
    		}
    		done++;
    		if (done % 100 == 0) {
    			exec.setProgress(((double)done) / inData[0].getRowCount());
    			exec.checkCanceled();
    		}
    	}
    	
    	BufferedDataContainer c = exec.createDataContainer(make_output_spec(inData[0].getSpec()));
    	for (int i=0; i<n_samples; i++) {
    		DataCell[] cells = new DataCell[13];
    		for (int j=0; j<cells.length; j++) {
    			cells[j] = DataType.getMissingCell();
    		}
    		cells[0] = new StringCell(m_cols.getIncludeList().get(i));
    		SummaryStatistics ss = idx_stats[i];
    		if (ss.getN() > 0) {
    			cells[1] = new DoubleCell(ss.getMin());
    			cells[2] = new DoubleCell(ss.getMax());
    			cells[3] = new DoubleCell(ss.getMean());
    			cells[4] = new DoubleCell(ss.getGeometricMean());
    			cells[5] = new DoubleCell(ss.getSum());
    			cells[6] = new DoubleCell(ss.getVariance());
    			cells[7] = new DoubleCell(ss.getSumsq());
    			cells[8] = new DoubleCell(ss.getStandardDeviation());
    			cells[9] = new DoubleCell(ss.getStandardDeviation() / Math.sqrt(ss.getN()));
    			cells[10]= new LongCell(ss.getN());
    			cells[11]= (ss.getN() >= 4) ? new DoubleCell(k_stats[i].getResult()) : DataType.getMissingCell();
    			cells[12]= new DoubleCell(skew_stats[i].getResult());
    		} else {
    			logger.warn("Zero (non-missing) values for "+m_cols.getIncludeList().get(i));
    			cells[10]= new LongCell(ss.getN());
    		}
    		c.addRowToTable(new DefaultRow("Sample"+i, cells));
    	}
        c.close();
        BufferedDataTable out = c.getTable();
        return new BufferedDataTable[]{out};
    }

    private DataTableSpec make_output_spec(DataTableSpec inSpec) throws IllegalArgumentException {
    	DataColumnSpec[] cols = new DataColumnSpec[13];
    	cols[0] = new DataColumnSpecCreator("Sample Name", StringCell.TYPE).createSpec();
    	cols[1] = new DataColumnSpecCreator("Minimum", DoubleCell.TYPE).createSpec();
    	cols[2] = new DataColumnSpecCreator("Maximum", DoubleCell.TYPE).createSpec();
    	cols[3] = new DataColumnSpecCreator("Mean", DoubleCell.TYPE).createSpec();
    	cols[4] = new DataColumnSpecCreator("Geometric Mean", DoubleCell.TYPE).createSpec();
    	cols[5] = new DataColumnSpecCreator("Sum", DoubleCell.TYPE).createSpec();
    	cols[6] = new DataColumnSpecCreator("Variance", DoubleCell.TYPE).createSpec();
    	cols[7] = new DataColumnSpecCreator("Sum of squares", DoubleCell.TYPE).createSpec();
    	cols[8] = new DataColumnSpecCreator("Standard Deviation", DoubleCell.TYPE).createSpec();
    	cols[9] = new DataColumnSpecCreator("Standard Error (of the mean)", DoubleCell.TYPE).createSpec();
    	cols[10]= new DataColumnSpecCreator("N", LongCell.TYPE).createSpec();
    	cols[11]= new DataColumnSpecCreator("Kurtosis", DoubleCell.TYPE).createSpec();
    	cols[12]= new DataColumnSpecCreator("Skewness", DoubleCell.TYPE).createSpec();
		return new DataTableSpec(cols);
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
    	m_cols.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_cols.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_cols.validateSettings(settings);
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

