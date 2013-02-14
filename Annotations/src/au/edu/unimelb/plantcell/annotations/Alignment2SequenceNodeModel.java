package au.edu.unimelb.plantcell.annotations;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.biojava.tasks.AlignmentSequenceExtractorTask;
import au.edu.unimelb.plantcell.core.biojava.tasks.BioJavaProcessorTask;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;



/**
 * Convert a sequence to tabular format based on user-chosen items of interest
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class Alignment2SequenceNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Alignment 2 Sequence");
   
    // dialog configuration code
    public final static String CFGKEY_ALIGNMENT_COL = "alignment-column";
    
    private final SettingsModelString    m_alignment = new SettingsModelString(CFGKEY_ALIGNMENT_COL, "");
    private int m_col = -1;
    
    /**
     * Constructor for the node model.
     */
    protected Alignment2SequenceNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

       logger.info("Extracting biological sequences to separate columns");
       if (m_col < 0) 
    	   throw new InvalidSettingsException("Cannot find column: "+m_alignment.getStringValue()+", re-configure?");
       
       // sequence summary data in first output port
       ColumnRearranger outputTable = new ColumnRearranger(inData[0].getDataTableSpec());
       outputTable.append(getTask());			// may throw if duplicate column names
     
       // execute!
       BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], outputTable, exec);
    
       // once we are done, we close the container and return its table     
       return new BufferedDataTable[]{out};
    }

   
	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    public BioJavaProcessorTask getTask() throws Exception {
    	 BioJavaProcessorTask t = new AlignmentSequenceExtractorTask();
         t.init(t.getNames()[0], m_col);	// HACK: we know the task has exactly one name
         return t;		// return fully initialised & constructed task
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        m_col = inSpecs[0].findColumnIndex(m_alignment.getStringValue());
        if (m_col < 0) {
        	if (hasAlignmentColumn(inSpecs[0])) {
        		m_col = useAlignmentColumnIndex(inSpecs[0], logger);
        	}
        }
        
        DataTableSpec dt = new DataTableSpec();
        if (m_col >= 0) {
        	try {
        		dt = new DataTableSpec(getTask().getColumnSpecs());
        	} catch (Exception e) {
        		e.printStackTrace();
        		throw new InvalidSettingsException(e.getMessage());
        	}
        }
       
        return new DataTableSpec[]{ new DataTableSpec(inSpecs[0], dt) };
    }
    
    // GRUBBY HACK FIXME TODO: code copied from AbstractWebServiceNodeModel
    /**
	 * Searches the specified input table spec to find a SequenceValue compatible column
	 */
	protected boolean hasAlignmentColumn(DataTableSpec inSpec) {
		return (useAlignmentColumnIndex(inSpec, null) >= 0);
	}

    // GRUBBY HACK FIXME TODO: code copied from AbstractWebServiceNodeModel
	/**
	 * Returns the index of the right-most column with a suitable
	 * @param inSpec input table spec to search
	 * @param logger may be null
	 * @return negative if not suitable column can be found, otherwise the column index is returned
	 */
	protected int useAlignmentColumnIndex(DataTableSpec inSpec, NodeLogger logger) {
		for (int i=inSpec.getNumColumns()-1; i >= 0; i--) {
			DataColumnSpec cs = inSpec.getColumnSpec(i);
			if (cs.getType().isCompatible(SequenceValue.class)) {
				if (logger != null) {
					logger.warn("Using '"+cs.getName()+"' column for sequence alignments.");
				}
				return i;
			}
		}
		return -1;
	}
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_alignment.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_alignment.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_alignment.validateSettings(settings);
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

