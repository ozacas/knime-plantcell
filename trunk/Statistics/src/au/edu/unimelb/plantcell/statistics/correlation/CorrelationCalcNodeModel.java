package au.edu.unimelb.plantcell.statistics.correlation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.core.Cache;
import au.edu.unimelb.plantcell.core.MyDataContainer;


/**
 * This is the model implementation of LinearCorrelationCalc.
 * Unlike the builtin knime node, this can perform large scale correlations by not storing the results into a matrix. I use it for AffyMetrix 21k gene correlations
 *
 * @author Andrew Cassin
 */
public class CorrelationCalcNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger("Correlation calculator");
    
    // configuration settings
    public final static String CFGKEY_METHODS = "statistical-correlation-method";
    public final static String CFGKEY_CORRECTION_METHODS = "multiple-comparison-correction-method";
	public static final String   CFGKEY_MAGNITUDE = "minimum-magnitude";

    /* 
     * do NOT change the order of things in this list - code below depends on it
     */
    public final static String[] METHODS = new String[] { "Pearson (linear) correlation", "Spearman rank" /*, "Kendall rank" */ };
    public final static String[] CORRECTION_METHODS = new String[] { "None", "Bonferroni correction" };
    
    private final SettingsModelStringArray m_method = new SettingsModelStringArray(CFGKEY_METHODS, METHODS);
    private final SettingsModelDoubleBounded   m_magnitude = new SettingsModelDoubleBounded(CFGKEY_MAGNITUDE, 0.0, 0.0, 1.0);
    
    // not yet implemented
    //private final SettingsModelString m_correction_method = new SettingsModelString(CFGKEY_CORRECTION_METHODS, CORRECTION_METHODS[0]);
    
    /**
     * Constructor for the node model.
     */
    protected CorrelationCalcNodeModel() {
        super(2, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	StringBuffer sb = new StringBuffer();
        for (String method : m_method.getStringArrayValue()) {
        	sb.append(method);
        	sb.append(' ');
        }
        logger.info("Computing correlations amongst numeric columns using "+sb.toString());

        
        // the data table spec of the single output table, 
        // the table will have three columns:
        DataTableSpec[] outspecs = make_output_spec();
        MyDataContainer c1 = new MyDataContainer(exec.createDataContainer(outspecs[0]), "Corr");
        MyDataContainer c2 = new MyDataContainer(exec.createDataContainer(outspecs[1]), "Outlier");
        
        HashSet<String> methods_to_use = new HashSet<String>();
        for (String s : m_method.getStringArrayValue()) {
        	methods_to_use.add(s);
        }
        
        // variables of interest
        HashSet<String> vars_of_interest = new HashSet<String>();
        boolean add_all = false;
        if (inData[1].getRowCount() > 0) {
            RowIterator it2 = inData[1].iterator();
            logger.info("Reporting target variables in Row ID column in bottom input port");
	        while (it2.hasNext()) {
	        	DataRow r = it2.next();
	        	vars_of_interest.add(r.getKey().getString());
	        }
	        it2 = null;
	        logger.info("Found "+vars_of_interest.size()+" target variables of interest.");
        } else {
        	logger.info("Reporting correlations between all available variables");
        	add_all = true;
        }
        
        // numeric only columns are considered (rows represent treatments, columns variables of interest in the correlation)
        HashMap<String,Integer> ok_columns = new HashMap<String,Integer>();
        for (int i=0; i<inData[0].getDataTableSpec().getNumColumns(); i++) {
        	DataColumnSpec cs = inData[0].getDataTableSpec().getColumnSpec(i);
        	if (cs.getType().isCompatible(DoubleValue.class)) {
        		ok_columns.put(cs.getName(), new Integer(i));
        		if (add_all) {
        			vars_of_interest.add(cs.getName());
        		}
        		//logger.info("Correlating values in: "+cs.getName());
        	}
        }
        logger.info("Found "+ok_columns.size()+" variables to perform correlation against selected target variables.");
        
        double minimum_magnitude = m_magnitude.getDoubleValue();
        boolean do_pearson = methods_to_use.contains(METHODS[0]);
        boolean do_spearman= methods_to_use.contains(METHODS[1]);
        	
        // 1. read table data into internal data structures
        RowIterator it = inData[0].iterator();
        Cache cache = new Cache("CorrelationCalc", new Properties());
        cache.init();
        if (!cache.isEnabled()) {
        	throw new InvalidSettingsException("You must specify a folder to cache data: check the PlantCell preferences (File -> Preferences)");
        }
        
        logger.info("Caching data... please be patient!");
        while (it.hasNext()) {
        	DataRow r = it.next();
        	for (String name : ok_columns.keySet()) {
        		DataCell c = r.getCell(ok_columns.get(name));
        		if (c == null || c.isMissing()) {
        			throw new Exception("Missing values are not currently supported.");
        		}
        		String vals = cache.get(name);
        		if (vals == null)
        			vals = "";
        		vals += " " + (DoubleValue) c;
        		cache.put(name, vals);
        	}
        	exec.checkCanceled();
        }
        
        // 2. construct data pairwise (ie. columnwise) and then correlate and output if not already done
        long n = ok_columns.size();        
        DenseBitVector reported_outliers = new DenseBitVector(n + 1);
               
        logger.info("Performing correlations... ");
        long i = 0;
        for (String iname : vars_of_interest) {
			double[] x = get_data(cache, iname);
			if (x != null) {
	        	long j=0;
	        	for (String jname : ok_columns.keySet()) {
	        		if (!iname.equals(jname)) {
	        			// do a correlation
	        			double[] y = get_data(cache, jname);
	        			if (x.length != y.length) {
	        				throw new Exception("Different number of values being compared");
	        			}
	        			if (y != null) {
		        			DataCell[] cells = new DataCell[c1.getTableSpec().getNumColumns()];
		        			cells[0] = new StringCell(iname);
		        			cells[1] = new StringCell(jname);
		        			for (int m=2; m<cells.length; m++) {
		        				cells[m] = DataType.getMissingCell();
		        			}
		        			
		        			boolean reported = false;
		        			//logger.info("Correlating "+iname+" with "+jname);
		        			//logger.info(iname+": "+x[0]+" "+x[1]+" "+x[2]+" "+x[3]);
		        			//logger.info(jname+": "+y[0]+" "+y[1]+" "+y[2]+" "+y[3]);
		        			if (do_pearson) {
		        				PearsonsCorrelation pc = new PearsonsCorrelation();
		        				double r = pc.correlation(x, y);
		        				if (Math.abs(r) >= minimum_magnitude) {
		        					cells[2] = new DoubleCell(r);
		        					cells[6] = new StringCell(METHODS[0]);
			        				c1.addRow(cells);
			        				reported = true;
		        				}
			        			pc = null;
		        			} 
		        			if (do_spearman) {
		        				SpearmansCorrelation sc = new SpearmansCorrelation();
		        				double r = sc.correlation(x, y);
		        				if (Math.abs(r) >= minimum_magnitude) {
		        					cells[2] = new DoubleCell(r);
		        					cells[6] = new StringCell(METHODS[1]);
		        					c1.addRow(cells);
		        					reported = true;
		        				}
			        			sc = null;
		        			} 
		        			
		        			if (reported && !reported_outliers.get(j)) {
		        				reported_outliers.set(j);
		        				// grubb test is not suitable for really small datasets
		        				if (y.length >= 6) {
		        					int outlier_idx = GrubbOutlierTest.findOutlier(y, GrubbOutlierTest.T_990);
			        				if (outlier_idx >= 0) {
			        					DataCell[] c2_cells = new DataCell[3];
			        					c2_cells[0] = new StringCell(jname);
			        					List<DoubleCell> value_cells = new ArrayList<DoubleCell>();
			        					for (int m=0; m<y.length; m++) {
			        						value_cells.add(new DoubleCell(y[m]));
			        					}
			        					c2_cells[1] = CollectionCellFactory.createListCell(value_cells);
			        					c2_cells[2] = new DoubleCell(y[outlier_idx]);
			        					c2.addRow(c2_cells);
			        				}
		        				}
		        			}
		        			
		        			//logger.info("correlated "+cells[0].toString()+ " - " + cells[1].toString());
		        			cells = null;
	        			}
	        		}
	        		j++;
	        		
					exec.checkCanceled();
					exec.setProgress(((double)i) / vars_of_interest.size());
	        	}
			}
        	i++;
        }
        
        // once we are done, we close the container and return its table
        c2.close();
        BufferedDataTable out = c1.close();
        BufferedDataTable out2= c2.close();
        return new BufferedDataTable[]{out, out2};
    }

    /** 
     * Retrieves the datapoints (as a double array) associated with the specified variable.
     * Returns <code>null</code> if the specified variable has no data (eg. not present in input)
     * @param cache
     * @param var_of_interest
     * @return
     * @throws Exception
     */
	private double[] get_data(Cache cache, String var_of_interest) throws Exception {
		String vals = cache.get(var_of_interest);
		if (vals == null)
			return null;
		String[] val= vals.trim().split("\\s+");
		double[] ret = new double[val.length];
		for (int i=0; i<val.length; i++) {
			ret[i] = Double.valueOf(val[i]);
		}
		return ret;
	}

	private DataTableSpec[] make_output_spec() {
		   DataColumnSpec[] allColSpecs = new DataColumnSpec[7];
	        allColSpecs[0] = 
	            new DataColumnSpecCreator("A", StringCell.TYPE).createSpec();
	        allColSpecs[1] = 
	            new DataColumnSpecCreator("B", StringCell.TYPE).createSpec();
	        allColSpecs[2] = 
	            new DataColumnSpecCreator("Correlation Coefficient (r)", DoubleCell.TYPE).createSpec();
	        allColSpecs[3] = 
	        	new DataColumnSpecCreator("95% CI Lower Limit", DoubleCell.TYPE).createSpec();
	        allColSpecs[4] =
	        	new DataColumnSpecCreator("95% CI Upper Limit", DoubleCell.TYPE).createSpec();
	        allColSpecs[5] =
	        	new DataColumnSpecCreator("Significance Probability", DoubleCell.TYPE).createSpec();
	        allColSpecs[6] =
	        	new DataColumnSpecCreator("Statistical Correlation Method", StringCell.TYPE).createSpec();
	        
	        DataColumnSpec[] allColSpecs2 = new DataColumnSpec[3];
	        allColSpecs2[0] = 
	        	new DataColumnSpecCreator("Variable with outlier (Grubb test)", StringCell.TYPE).createSpec();
	        allColSpecs2[1] = 
	        	new DataColumnSpecCreator("Values (incl. outliers)", ListCell.getCollectionType(DoubleCell.TYPE)).createSpec();
	        allColSpecs2[2]  =
	        	new DataColumnSpecCreator("A known outlier", DoubleCell.TYPE).createSpec();
	        
	        return new DataTableSpec[] { new DataTableSpec(allColSpecs), new DataTableSpec(allColSpecs2) };
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
        return make_output_spec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_method.saveSettingsTo(settings);
    	m_magnitude.saveSettingsTo(settings);
    	//m_correction_method.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_method.loadSettingsFrom(settings);
    	m_magnitude.loadSettingsFrom(settings);
    	//m_correction_method.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_method.validateSettings(settings);
    	m_magnitude.validateSettings(settings);
    	//m_correction_method.validateSettings(settings);
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

