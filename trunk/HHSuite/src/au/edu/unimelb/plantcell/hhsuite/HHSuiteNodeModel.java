package au.edu.unimelb.plantcell.hhsuite;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
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
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.MyDataContainer;


/**
 * This is the model implementation of HHSuite.
 * Provides remote homology detection (more sensitive than BLAST) with most of the performance. 
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class HHSuiteNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("HHSuite");
    
    
	public static final String CFGKEY_ALIGNMENT           = "alignment-column";
	public static final String CFGKEY_SEARCH_DATABASE     = "search-database";
	public static final String CFGKEY_CPU_CORES           = "number-of-cpu-cores";
        
    private final SettingsModelString m_alignment         = new SettingsModelString(CFGKEY_ALIGNMENT, "");
    private final SettingsModelString m_search_db         = new SettingsModelString(CFGKEY_SEARCH_DATABASE, "");
    private final SettingsModelIntegerBounded m_cpu_cores = new SettingsModelIntegerBounded(CFGKEY_CPU_CORES, 2, 1, 16);
    
    /**
     * Constructor for the node model.
     */
    protected HHSuiteNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	DataTableSpec outTable = make_output_spec(inData[0].getSpec());
        MyDataContainer c = new MyDataContainer(exec.createDataContainer(outTable), "Row");
        
        logger.info("Searching alignments from "+m_alignment.getStringValue()+" using HHSearch.");
        
        // once we are done, we close the container and return its table
        return new BufferedDataTable[]{c.close()};
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
 
        return new DataTableSpec[]{make_output_spec(inSpecs[0])};
    }

    private DataTableSpec make_output_spec(DataTableSpec inSpec) {
    	DataColumnSpec[] outCols = new DataColumnSpec[2];
    	
		return new DataTableSpec(inSpec, new DataTableSpec(outCols));
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_cpu_cores.saveSettingsTo(settings);
    	m_alignment.saveSettingsTo(settings);
    	m_search_db.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_cpu_cores.loadSettingsFrom(settings);
    	m_alignment.loadSettingsFrom(settings);
    	m_search_db.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_cpu_cores.validateSettings(settings);
    	m_alignment.validateSettings(settings);
    	m_search_db.validateSettings(settings);    
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

