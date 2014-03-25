package au.edu.unimelb.plantcell.statistics.venn;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.def.IntCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;


/**
 * This is the model implementation of VennAnalyzer.
 * Performs an n-way (n<=4 recommended) venn analysis based over the values in chosen columns based on a group-by column.
 *
 * @author Andrew Cassin
 */
public class VennAnalyzerNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Venn Diagram");
        
  
    static final String CFGKEY_GROUPBY = "groupby-column";
    static final String CFGKEY_VALUE_COLUMNS = "value-columns";
    static final String CFGKEY_N = "max-n";
    
    /**
     * persistent user configured state
     */
    private final SettingsModelString m_groupby = new SettingsModelString(CFGKEY_GROUPBY, "");
    private final SettingsModelStringArray m_value_columns = new SettingsModelStringArray(CFGKEY_VALUE_COLUMNS, new String[] {""} );
    private final SettingsModelIntegerBounded m_n = new SettingsModelIntegerBounded(CFGKEY_N, 4, 1, 100000);
    

    /**
     * results of last analysis (TODO: not currently persisted)
     */
    private VennModel m_venn;
    
    /**
     * Constructor for the node model.
     */
    protected VennAnalyzerNodeModel() {
        super(1, 2);
        m_venn = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	logger.info("Calculating up to "+ m_n.getIntValue()+"-way venn diagram for input table");
    	HashSet<String> categories = new HashSet<String>();
    	DataTableSpec[] specs = make_output_spec();
    	
    	BufferedDataContainer container = exec.createDataContainer(specs[0]);
    	BufferedDataContainer c2        = exec.createDataContainer(specs[1]);
    	
    	reset();	// discard old model to save memory when computing new one...
    	
    	// 1. search group-by column for required number of up-to N categories
    	RowIterator it = inData[0].iterator();
    	int        idx = inData[0].getDataTableSpec().findColumnIndex(m_groupby.getStringValue());
    	if (idx < 0) {
    		throw new Exception("Cannot locate group-by column: "+m_groupby.getStringValue());
    	}
    	while (it.hasNext()) {
    		if (categories.size() >= m_n.getIntValue())
    			break;
    		DataRow r = it.next();
    		DataCell c = r.getCell(idx);
    		if (c == null || c.isMissing())
    			continue;
    		String category = c.toString();
    		if (category != null && category.length() > 0)
    			categories.add(category);
    	}
    	exec.checkCanceled();
    	
    	// NB: cant use m_n.getIntValue() as this may not correspond to the number of distinct values in the group-by column
    	VennModel venn = new VennModel(categories.size(), categories);

    	// 2. process values to compute venn data
    	it = inData[0].iterator();
    	ArrayList<Integer> vec = new ArrayList<Integer>();
    	for (String col : m_value_columns.getStringArrayValue()) {
    		int tmp = inData[0].getDataTableSpec().findColumnIndex(col);
    		if (tmp < 0) {
    			throw new Exception("Cannot locate value column: "+col);
    		}
    		vec.add(tmp);
    	}
    	Integer[] cell_idx = vec.toArray(new Integer[0]);
    	int done = 0;
    	while (it.hasNext()) {
    		DataRow r = it.next();
    		String category = r.getCell(idx).toString();
    		if (categories.contains(category)) {
    			HashSet<String> values = new HashSet<String>();
    			for (Integer i : cell_idx) {
    				DataCell c = r.getCell(i.intValue());
    				if (c == null || c.isMissing()) {
    					continue;
    				}
    				if (c.getType().isCollectionType()) {
    					CollectionDataValue collection = (CollectionDataValue) c;
    					for (DataCell tmp : collection) {
    						String s = tmp.toString();
    						if (s != null)
    							values.add(s);
    					}
    				} else {
    					String s = c.toString();
    					if (s != null)
    						values.add(s);
    				}
    			}
    			
    			venn.add(category, values);
    			// fallthru
    		}
    		
    		if (done % 1000 == 0) {
    			exec.setProgress(((double)done) / inData[0].getRowCount());
    			exec.checkCanceled();
    		}
    		done++;
    	}
    	
    	// 3. save results
    	VennDataContainerAdapter adapter = new VennDataContainerAdapter(container);
    	venn.outputToAdapter(adapter);
    	venn.outputValuesToContainer(c2, adapter.getDoneCategories());
    	m_venn = venn;
    	
    	// 4. done!
        container.close();
        c2.close();
        BufferedDataTable out = container.getTable();
        BufferedDataTable out2= c2.getTable();
        return new BufferedDataTable[]{out, out2};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    	m_venn = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
        return make_output_spec();
    }

    private DataTableSpec[] make_output_spec() {
    	DataColumnSpec[] cols = new DataColumnSpec[2];
    	cols[0] = new DataColumnSpecCreator("Category", StringCell.TYPE).createSpec();
    	cols[1] = new DataColumnSpecCreator("Count", IntCell.TYPE).createSpec();
    	
    	DataColumnSpec[] cols2= new DataColumnSpec[2];
    	cols2[0] = new DataColumnSpecCreator("Category", StringCell.TYPE).createSpec();
    	cols2[1] = new DataColumnSpecCreator("Value", StringCell.TYPE).createSpec();
    	
		return new DataTableSpec[] { new DataTableSpec(cols), new DataTableSpec(cols2)};
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_groupby.saveSettingsTo(settings);
    	m_n.saveSettingsTo(settings);
    	m_value_columns.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_groupby.loadSettingsFrom(settings);
    	m_n.loadSettingsFrom(settings);
    	m_value_columns.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_groupby.validateSettings(settings);
    	m_n.validateSettings(settings);
    	m_value_columns.validateSettings(settings);
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
     * Returns the model computed by last successful execute()
     * @return
     */
    public VennModel getVennModel() {
    	return m_venn;
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

