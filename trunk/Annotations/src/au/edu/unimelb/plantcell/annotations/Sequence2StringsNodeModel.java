package au.edu.unimelb.plantcell.annotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.cells.SequenceAnnotation;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcore.core.regions.RegionInterface;



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
    public final static String SEQUENCE_ID            = "Sequence ID";		// NB: must match dialog code
    public final static String SEQUENCE_SINGLE_LETTER = "Sequence (single-letter)"; // ditto
    public final static String DESCRIPTION_PRIMARY    = "Description (primary)";
    public final static String INPUT_SEQUENCE         = "Annotated Sequence";
    
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
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

       logger.info("Extracting biological sequences to separate columns");
       boolean made_table_spec = false;
       if (m_seq_idx < 0) 
    	   throw new InvalidSettingsException("Cannot find column: "+m_sequence.getStringValue()+", re-configure?");
       BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(inData[0].iterator(), m_seq_idx, 100, 0, new SequenceProcessor() {

			@Override
			public SequenceValue process(SequenceValue sv) {
				// for now, we dont validate the input sequences
				return sv;
			}
    	   
       });
       HashSet<String> want = new HashSet<String>();
       ArrayList<String> wanted_tracks = new ArrayList<String>();
       for (String s : m_wanted.getStringArrayValue()) {
    	   logger.info("Extracting \'"+s+"\' into columns");
    	   if (s.startsWith("Track - ")) {
    		   wanted_tracks.add(s.substring("Track - ".length()));
    	   } else {
    		   want.add(s);
    	   }
       }
       MyDataContainer c = null;
       
       HashMap<String,Integer> cols = new HashMap<String,Integer>();
       int done = 0;
       
       while (bsi.hasNext()) {
    	   List<SequenceValue>      batch = bsi.next();
    	   
    	   for (SequenceValue sv : batch) {
    		   if (!made_table_spec) {
    		       DataTableSpec outSpec = make_output_spec(inData[0].getSpec(), sv, m_wanted.getStringArrayValue());
    		       c = new MyDataContainer(exec.createDataContainer(outSpec), "Row");
    		       for (int col_idx = 0; col_idx < outSpec.getNumColumns(); col_idx++) {
    		    	   DataColumnSpec colSpec = outSpec.getColumnSpec(col_idx);
    		    	   cols.put(colSpec.getName(), new Integer(col_idx));
    		       }
    		       made_table_spec = true;
    		   }
    		   
    		   // NB: these strings must match the dialog code
    		   Map<String,DataCell> ocells = new HashMap<String,DataCell>();
    		   
    		   if (want.contains(SEQUENCE_ID)) {
    			   ocells.put(SEQUENCE_ID, new StringCell(sv.getID()));
    		   } 
    		   if (want.contains(SEQUENCE_SINGLE_LETTER)) {
    			   ocells.put(SEQUENCE_SINGLE_LETTER, new StringCell(sv.getStringValue()));
    		   }
    		   if (want.contains(DESCRIPTION_PRIMARY) && sv.hasDescription()) {
    			   ocells.put(DESCRIPTION_PRIMARY, new StringCell(sv.getDescription()));
    		   }
    		   if (want.contains(INPUT_SEQUENCE)) {
    			   ocells.put(INPUT_SEQUENCE, new SequenceCell(sv));
    		   }
    		   // find track data (if requested by the user)
    		   if (wanted_tracks.size() > 0) {
	    		   for (String key : wanted_tracks) {
    				   Track t = sv.getTrackByName(key, null);
    				   if (t != null) {
    					   SequenceAnnotation      sa = t.getAnnotation();
    					   boolean is_first = true;
    					   Set<String> track_attrs = new HashSet<String>();
    					   for (RegionInterface ri : sa.getRegions()) {
    			    			Map<String,DataCell> cells = ri.asCells(t.getName());
    			    			if (is_first) {
    			    				is_first = false;
    			    				for (String s : cells.keySet()) {
    			    					track_attrs.add(s);
    			    				}
    			    			}
    			    			for (String k : cells.keySet()) {
    			    				ocells.put(k, cells.get(k));
    			    			}
    			    			report_row(c, ocells, cols);
    			    	   }
    					   
    					   for (String s : track_attrs) {
    						   ocells.remove(s);
    					   }
    				   }
	    		   }
    		   } else {
    			   // no tracks so just output the row
    			   report_row(c, ocells, cols);
    		   }
    		 
    	   }
    	   exec.checkCanceled();
    	   done += batch.size();
    	   exec.setProgress(((double)done) / inData[0].getRowCount());
       }
       return new BufferedDataTable[]{c.close()};
    }

    private void report_row(final MyDataContainer c, final Map<String, DataCell> ocells, final Map<String,Integer> colmap) {
    	  DataCell[] cells = new DataCell[c.getTableSpec().getNumColumns()];
		   for (int i=0; i<cells.length; i++) {
			   cells[i] = DataType.getMissingCell();
		   }
		   for (String key : ocells.keySet()) {
			   if (colmap.containsKey(key)) {
				   Integer i = colmap.get(key);
				   cells[i.intValue()]  = ocells.get(key);
			   }
		   }
		   c.addRow(cells);
	}

	/**
     * KNIME requires at least one column, so we guarantee that despite user-configuration
     * @param spec
     * @param wanted 
     * @return
     * @throws InvalidSettingsException 
     */
	private DataTableSpec make_output_spec(DataTableSpec spec, SequenceValue sv, String[] wanted) throws InvalidSettingsException {
		if (m_wanted.getStringArrayValue().length < 1) {
	    	DataColumnSpec[] cols = new DataColumnSpec[1];
	    	cols[0] = new DataColumnSpecCreator(SEQUENCE_ID, StringCell.TYPE).createSpec();
			return new DataTableSpec(cols);
		}
		
		// else...
		ArrayList<DataColumnSpec> cols = new ArrayList<DataColumnSpec>();
		Arrays.sort(wanted);
		for (String w : wanted) {
			DataType type = StringCell.TYPE;
			if (w.equals(INPUT_SEQUENCE)) {
				DataColumnSpecCreator my_annot_spec = new DataColumnSpecCreator("Annotated Sequence", SequenceCell.TYPE);
				
				// we must keep the list of tracks as a property associated with the column. We must do this
				// so that annotation dialogs can correctly display the tracks for the user
				int seq_idx = spec.findColumnIndex(m_sequence.getStringValue());
				if (seq_idx >= 0) {
					my_annot_spec.setProperties(spec.getColumnSpec(seq_idx).getProperties());
				}
				cols.add(my_annot_spec.createSpec());
			} else if (w.startsWith("Track - ") && sv != null) {
				Track t = sv.getTrackByName(w.substring("Track - ".length()), null);
				if (t != null) {
					SequenceAnnotation sa = t.getAnnotation();
					if (sa != null)
						cols.addAll(sa.asColumnSpec(t.getName()));
				}
			} else {
				cols.add(new DataColumnSpecCreator(w, type).createSpec());
			}
		}
    	
		return new DataTableSpec(cols.toArray(new DataColumnSpec[0]));
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
        return new DataTableSpec[]{ null };
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

