package au.edu.unimelb.plantcell.annotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.AbstractCellFactory;
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
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.biojava.tasks.BioJavaProcessorTask;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.misc.biojava.BioJavaProcessorNodeModel;



/**
 * Convert a sequence to tabular format based on user-chosen items of interest
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class Sequence2StringsNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Sequence 2 Columns");
    
    // all tracks are prefixed with 
	public final static String PLANTCELL_TRACK_PREFIX = "PlantCell:Tracks:";

    // literals for use during execute()
    public final static String SEQUENCE_ID            = "Sequence ID";		        // NB: must match dialog code
    public final static String SEQUENCE_SINGLE_LETTER = "Sequence (single-letter)"; // ditto
    public final static String DESCRIPTION_PRIMARY    = "Description (primary)";    // ditto
    public final static String INPUT_SEQUENCE         = "Annotated Sequence";       // ditto
    public final static String SEQUENCE_LENGTH        = "Sequence Length (residue count)"; // ditto
    
    // dialog configuration code
    public final static String CFGKEY_SEQUENCE_COL = "sequence-column";
	public static final String CFGKEY_WANTED       = "wanted-items";
    
    private final SettingsModelString    m_sequence = new SettingsModelString(CFGKEY_SEQUENCE_COL, "");
    private final SettingsModelStringArray m_wanted = new SettingsModelStringArray(CFGKEY_WANTED, new String[] { SEQUENCE_ID });
    private int m_seq_idx = -1;
    
    /**
     * Constructor for the node model.
     */
    protected Sequence2StringsNodeModel() {
        super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

       logger.info("Extracting biological sequences to separate columns");
       if (m_seq_idx < 0) 
    	   throw new InvalidSettingsException("Cannot find column: "+m_sequence.getStringValue()+", re-configure?");
       
       // sequence summary data in first output port
       ColumnRearranger outputTable = new ColumnRearranger(inData[0].getDataTableSpec());

       // for "small" stuff (attributes of the sequence) we just use the node's cell factory
       // otherwise we use the cell factories from the BioJava node to do the "heavy lifting" of processing
       // the sequences into columns.
       
       // 1. which selected items should be handled by biojava?
       HashSet<String> left = new HashSet<String>();
       HashSet<String> total = new HashSet<String>();
       logger.info("Wanted tasks: ");
       for (String s : m_wanted.getStringArrayValue()) {
    	   left.add(s);
    	   logger.info(s);
    	   total.add(s);
       }
       List<AbstractCellFactory> factories = new ArrayList<AbstractCellFactory>();
       for (BioJavaProcessorTask t : BioJavaProcessorNodeModel.getTasks()) {
    	   ArrayList<String> names = new ArrayList<String>();
    	   for (String s : t.getNames()) {
    		   names.add(s);
    	   }
    	   // remove biojava tasks from t in the wanted list (or whats left of it)
    	   if (left.removeAll(names)) {
    		   // we must instantiate a SEPARATE task for each name chosen and add to the factory list
    		   for (String name : names) {
    			   if (total.contains(name)) {
    				   BioJavaProcessorTask t2 = t.getClass().newInstance();
    				   t2.init(name, m_seq_idx);
    				   factories.add(t2);
    			   }
    		   }
    	   }
       }
       
       // 2. and the remainder of desired columns can be handled by the node's cell factory...
       MyDataContainer c2 = null;
       if (left.size() > 0) {		// something selected which is not handled by biojava task(s)?
    	   Sequence2ColumnsCellFactory s2ccf = new Sequence2ColumnsCellFactory(m_seq_idx, 
    		   											left.toArray(new String[0]), logger, inData[0].getSpec());
    	   factories.add(s2ccf);
    	   // c2 contains extracted features from user-specified tracks (if any were configured)
           c2 = new MyDataContainer(exec.createDataContainer(s2ccf.getTrackColumnSpec()), "r");
           s2ccf.setTrackContainer(c2);
       } else {
    	   // must initialise c2 for below...
    	   c2 = new MyDataContainer(exec.createDataContainer(new DataTableSpec()), "r");
       }
       
       // add the needed cell factories to the output table
       logger.info("Got "+factories.size()+" cell factories to run.");
       try {
    	   for (AbstractCellFactory acf : factories) {		// TODO: order of factories important?
    		   outputTable.append(acf);
    	   }
       } catch (Exception e) {
    	   logger.warn("Two configured items produce the same columns - re-configure!");
    	   throw e;
       }
        
       // execute!
       BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], outputTable, exec);
    
       // once we are done, we close the container and return its table     
       return new BufferedDataTable[]{out, c2.close()};
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
        m_seq_idx = inSpecs[0].findColumnIndex(m_sequence.getStringValue());
        if (m_seq_idx < 0) {
        	if (hasSequenceColumn(inSpecs[0])) {
        		m_seq_idx = useSequenceColumnIndex(inSpecs[0], logger);
        	}
        }
        return new DataTableSpec[]{ null, null };
    }
    
    // GRUBBY HACK FIXME TODO: code copied from AbstractWebServiceNodeModel
    /**
	 * Searches the specified input table spec to find a SequenceValue compatible column
	 */
	protected boolean hasSequenceColumn(DataTableSpec inSpec) {
		return (useSequenceColumnIndex(inSpec, null) >= 0);
	}

    // GRUBBY HACK FIXME TODO: code copied from AbstractWebServiceNodeModel
	/**
	 * Returns the index of the right-most column with a suitable
	 * @param inSpec input table spec to search
	 * @param logger may be null
	 * @return negative if not suitable column can be found, otherwise the column index is returned
	 */
	protected int useSequenceColumnIndex(DataTableSpec inSpec, NodeLogger logger) {
		for (int i=inSpec.getNumColumns()-1; i >= 0; i--) {
			DataColumnSpec cs = inSpec.getColumnSpec(i);
			if (cs.getType().isCompatible(SequenceValue.class)) {
				if (logger != null) {
					logger.warn("Using '"+cs.getName()+"' column for biological sequences.");
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
    	m_sequence.saveSettingsTo(settings);
    	m_wanted.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_sequence.loadSettingsFrom(settings);
    	m_wanted.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_sequence.validateSettings(settings);
    	m_wanted.validateSettings(settings);
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

