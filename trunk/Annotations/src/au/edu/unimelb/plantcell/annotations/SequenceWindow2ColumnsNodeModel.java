package au.edu.unimelb.plantcell.annotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
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

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.biojava.tasks.BioJavaProcessorTask;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.misc.biojava.BioJavaProcessorNodeModel;



/**
 * Convert a sequence to tabular format based on windows of the user-chosen calculation
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class SequenceWindow2ColumnsNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Sequence Window 2 Columns");
 
    // constants
    public final static String[] METHODS = new String[] { "overlapping", "discontiguous" };
    
    // dialog configuration code
    public final static String CFGKEY_SEQUENCE_COL = "sequence-column";
	public final static String CFGKEY_WANTED       = "wanted-items";
	public final static String CFGKEY_NMER		   = "window-size";		// how many residues
    public final static String CFGKEY_STEP         = "step-size";		// bump window size after each window by this amount
    
    private final SettingsModelString    m_sequence = new SettingsModelString(CFGKEY_SEQUENCE_COL, "");
    private final SettingsModelStringArray m_wanted = new SettingsModelStringArray(CFGKEY_WANTED, new String[] { });
    private final SettingsModelIntegerBounded m_n   = new SettingsModelIntegerBounded(CFGKEY_NMER, 3, 1, 1000000);
    private final SettingsModelIntegerBounded m_step= new SettingsModelIntegerBounded(CFGKEY_STEP, 1, 1, 1000);
    
    private int m_seq_idx = -1;
    
    /**
     * Constructor for the node model.
     */
    protected SequenceWindow2ColumnsNodeModel() {
        super(1, 1);
    }

    /**
     * Constructs the {@link BioJavaProcessorTask} of the specified name or returns <code>null</code> if there is no such task.
     * It is the caller's responsibility to call the task's <code>init()</code> method.
     * 
     * @param wanted
     * @return
     * @throws Exception
     */
    protected BioJavaProcessorTask getTask(String wanted) throws Exception {
         for (BioJavaProcessorTask bjt : BioJavaProcessorNodeModel.getTasks()) { 
			for (String name : bjt.getNames()) {
				if (wanted.contains(name)) {
					BioJavaProcessorTask t = bjt.getClass().newInstance();
					return t;
				}
			}
         }
         
         return null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

       logger.info("Creating and analysing windows of input sequences to a table...");
       
       // prepare task
       String[] wanted = m_wanted.getStringArrayValue();
       if (wanted == null || wanted.length < 1)
    	   throw new InvalidSettingsException("No task chosen!");
       
       BioJavaProcessorTask t = getTask(wanted[0]);
       if (t == null) {
    	   throw new InvalidSettingsException("No such task: "+wanted[0]);
       }
       t.init(wanted[0], m_seq_idx);
       
       // CAREFUL: the same instance (t) must be used for configuring the output columns AND execute()
       DataTableSpec outSpec = make_output_spec(inData[0].getSpec(), t);
       
       MyDataContainer out = new MyDataContainer(exec.createDataContainer(outSpec), "w");
      
       
       // iterate over the rows compute the windows over each sequence and dumping the results
       RowIterator it = inData[0].iterator();
       int done = 0;
       while (it.hasNext()) {
    	   DataRow  r = it.next();
    	   DataCell c = r.getCell(m_seq_idx);
    	   if (c == null || c.isMissing() || !(c instanceof SequenceValue)) {
    		   continue;
    	   }
    	   
    	   SequenceValue       sv = (SequenceValue) c;
    	   DataCell[]       cells = new DataCell[r.getNumCells()];
    	   WindowSequenceCell wsc = new WindowSequenceCell(sv, m_n.getIntValue(), m_step.getIntValue());
    	   for (int i=0; i<cells.length; i++) {
    		   cells[i] = r.getCell(i);
    		   if (i == m_seq_idx) {
    			   cells[i] = wsc;
    		   }
    	   }
    	   
    	   // HACK TODO FIXME: construct a sequence cell to replace cells[m_seq_idx] with so that 
    	   // each call to getStringValue() returns the current window and *not* the entire sequence
    	   DataRow new_row = new DefaultRow(r.getKey(), cells);
    	  
    	   while (wsc.hasNextWindow()) {
    		   ArrayList<DataCell> out_cells = new ArrayList<DataCell>(cells.length+10);
    		   out_cells.add(new StringCell(sv.getID()));
    		   out_cells.add(new IntCell(wsc.getStart()));
    		   out_cells.add(new IntCell(wsc.getEnd()));
    		   out_cells.add(new StringCell(wsc.peekStringWindow()));		// must call wsc.peekStringWindow() before position is updated in wsc
    		   
    		   // compute desired (windowed) metric
    		   cells = t.getCells(new_row);
    		   
    		   for (DataCell dc: cells) {
    			   out_cells.add(dc);
    		   }
    		   out.addRow(out_cells.toArray(new DataCell[0]));
    	   }
    	   
    	   if (done % 20 == 0) {
    		   exec.checkCanceled();
    		   exec.setProgress(((double)done)/inData[0].getRowCount());
    	   }
       }
       
       // once we are done, we close the container and return its table     
       return new BufferedDataTable[]{out.close()};
    }

   
	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    protected DataTableSpec make_output_spec(DataTableSpec inSpec, BioJavaProcessorTask t) throws InvalidSettingsException {
	   m_seq_idx = inSpec.findColumnIndex(m_sequence.getStringValue());
       if (m_seq_idx < 0) {
       		if (hasSequenceColumn(inSpec)) {
       			m_seq_idx = useSequenceColumnIndex(inSpec, logger);
       		}
       }
       
       try {
    	   // if the task is null then we assume it needs to be found and initialised, otherwise we assume thats all done
    	   String[] wanted = m_wanted.getStringArrayValue();
    	   if (wanted.length > 0 && t == null) {
    		   t = getTask(wanted[0]);
    		   t.init(wanted[0], m_seq_idx);
    	   } 
    	   ArrayList<DataColumnSpec> cols = new ArrayList<DataColumnSpec>();
    	   cols.add(new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec());
    	   cols.add(new DataColumnSpecCreator("Window Start (from 1)", IntCell.TYPE).createSpec());
    	   cols.add(new DataColumnSpecCreator("Window End", IntCell.TYPE).createSpec());
    	   cols.add(new DataColumnSpecCreator("Window (sequence)", StringCell.TYPE).createSpec());
    	   
    	   if (t != null) {
    		   for (DataColumnSpec colSpec : t.getColumnSpecs()) {
    			   cols.add(colSpec);
    		   }
    	   }
           
           return new DataTableSpec(cols.toArray(new DataColumnSpec[0]));
       } catch (Exception e) {
    	   // TODO Auto-generated catch block
    	   e.printStackTrace();
    	   return new DataTableSpec();
       }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
     
        return new DataTableSpec[]{ make_output_spec(inSpecs[0], null) };
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
    	m_n.saveSettingsTo(settings);
    	m_step.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_sequence.loadSettingsFrom(settings);
    	m_wanted.loadSettingsFrom(settings);
    	m_n.loadSettingsFrom(settings);
    	m_step.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_sequence.validateSettings(settings);
    	m_wanted.validateSettings(settings);
    	m_n.validateSettings(settings);
    	m_step.validateSettings(settings);
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

