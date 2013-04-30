package au.edu.unimelb.plantcell.statistics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * This is the model implementation of Ranker.
 * Basic descriptive and inferential statistics support built using the apache commons math v3 library.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ANOVA1WayNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("One way ANOVA");
        
    private static final int MAX_PERMUTATION_SIZE = 6;	// report all anova results for up to 6 samples
    
    public static final String CFGKEY_ALPHA = "significance-level";
    public static final String CFGKEY_FROM  = "data-from";
    public static final String CFGKEY_COLUMN_CLASSES = "column-class-names";
    

    private SettingsModelDouble m_alpha         = new SettingsModelDouble(CFGKEY_ALPHA, 0.05);
    private SettingsModelString m_from          = new SettingsModelString(CFGKEY_FROM, "columns");
    private SettingsModelFilterString m_columns = new SettingsModelFilterString(CFGKEY_COLUMN_CLASSES);
    
    
    // not persisted, for sole use by do_anova()
    private int m_id;
    
    /**
     * Constructor for the node model.
     */
    protected ANOVA1WayNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
	@Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	Map<Integer, String> wanted_set = new HashMap<Integer,String>();
    	for (String colName : m_columns.getIncludeList()) {
    		int idx = inData[0].getSpec().findColumnIndex(colName);
    		if (idx < 0) {
    			throw new InvalidSettingsException("Unable to locate: "+colName+" - re-configure the node?");
    		}
    		wanted_set.put(new Integer(idx), colName);
    	}
    	
    	BufferedDataContainer c = exec.createDataContainer(make_output_spec(inData[0].getSpec()));
    	Map<Integer,ArrayList<Double>> samples = new HashMap<Integer,ArrayList<Double>>();
    	RowIterator it = inData[0].iterator();
    	Integer[] wanted = wanted_set.keySet().toArray(new Integer[0]);
    	
    	while (it.hasNext()) {
    		DataRow r = it.next();
    		for (Integer w : wanted) {
    			DataCell cell = r.getCell(w.intValue());
    			if (cell == null || cell.isMissing()) {
    				continue;
    			} 
    			DoubleValue dv = (DoubleValue) cell;
    			if (Double.isNaN(dv.getDoubleValue()))
    				continue;
    			ArrayList<Double> values = samples.get(w);
    			if (values == null) {
    				values = new ArrayList<Double>();
    				samples.put(w, values);
    			}
    			values.add(new Double(dv.getDoubleValue()));
    		}
    	}
    	
    	m_id = 1;	// for use only by do_anova() which adds to the output port
    	for (Integer idx : wanted_set.keySet()) {
    		List<Double> l = samples.get(idx);
			logger.info("Sample "+wanted_set.get(idx)+" has "+l.size()+" valid data values.");
		}
    	if (wanted_set.size() < MAX_PERMUTATION_SIZE) {
    		logger.info("Computing ANOVA of all permutations of samples in output table");
    		recurse_wanted_set(wanted_set, samples, c, exec);
    	} else {
    		do_anova(wanted_set, samples, c, exec);
    	}
	
        c.close();
        BufferedDataTable out = c.getTable();
        return new BufferedDataTable[]{out};
    }

	private void recurse_wanted_set(Map<Integer, String> wanted_set,
			Map<Integer, ArrayList<Double>> samples, BufferedDataContainer c,
			ExecutionContext exec) {
    	// termination condition
    	if (wanted_set.keySet().size() < 2)
    		return;
   
    	do_anova(wanted_set, samples, c, exec);
    	
    	List<Integer> vec = new ArrayList<Integer>();
    	vec.addAll(wanted_set.keySet());
		
    	// recursive step
    	for (int i=0; i<vec.size(); i++) {
    		Integer old = vec.remove(i);
    		Map<Integer,String> subset = new HashMap<Integer,String>();
    		for (Integer tmp : vec) {
    			subset.put(tmp, wanted_set.get(tmp));
    		}
    		recurse_wanted_set(subset, samples, c, exec);
    		vec.add(i, old);
    	}
	}

    @SuppressWarnings("unchecked")
	public void do_anova(Map<Integer, String> wanted_set,
			Map<Integer, ArrayList<Double>> samples, BufferedDataContainer c,
			ExecutionContext exec) {
    	@SuppressWarnings("rawtypes")
		ArrayList al_samples = new ArrayList();
		ArrayList<StringCell> descr = new ArrayList<StringCell>();
    	for (Integer idx : wanted_set.keySet()) {
    		List<Double> l = samples.get(idx);
        	descr.add(new StringCell(wanted_set.get(idx)));
        	
    		if (l.size() < 1) 
    			continue;
			double[] v = ArrayUtils.toPrimitive(l.toArray(new Double[0]));
    		al_samples.add(v);
    	}
    	
    	double f_value = TestUtils.oneWayAnovaFValue(al_samples);
    	double p_value = TestUtils.oneWayAnovaPValue(al_samples);
    	boolean reject_nh = TestUtils.oneWayAnovaTest(al_samples, m_alpha.getDoubleValue());
    	
    	DataCell[] cells = new DataCell[4];
    	cells[0] = CollectionCellFactory.createListCell(descr);
    	cells[1] = new DoubleCell(f_value);
    	cells[2] = new DoubleCell(p_value);
    	cells[3] = reject_nh ? BooleanCell.TRUE : BooleanCell.FALSE;
    	c.addRowToTable(new DefaultRow("AnovaResult"+m_id++, cells));
    }
    
	private DataTableSpec make_output_spec(DataTableSpec inSpec) {
    	DataColumnSpec[] cols = new DataColumnSpec[4];
    	
    	cols[0] = new DataColumnSpecCreator("ANOVA: Samples", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
    	cols[1] = new DataColumnSpecCreator("ANOVA: F-Value", DoubleCell.TYPE).createSpec();
    	cols[2] = new DataColumnSpecCreator("ANOVA: P-Value", DoubleCell.TYPE).createSpec();
    	cols[3] = new DataColumnSpecCreator("ANOVA: Null hypothesis rejected at "+m_alpha.getDoubleValue()+"?", BooleanCell.TYPE).createSpec();
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
    	m_alpha.saveSettingsTo(settings);
    	m_from.saveSettingsTo(settings);
    	m_columns.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_alpha.loadSettingsFrom(settings);
    	m_from.loadSettingsFrom(settings);
    	m_columns.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_alpha.loadSettingsFrom(settings);
    	m_from.loadSettingsFrom(settings);
    	m_columns.loadSettingsFrom(settings);
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

