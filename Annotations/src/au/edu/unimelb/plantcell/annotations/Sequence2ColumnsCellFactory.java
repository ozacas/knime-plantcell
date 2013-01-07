package au.edu.unimelb.plantcell.annotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.cells.SequenceAnnotation;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcore.core.regions.RegionInterface;

public class Sequence2ColumnsCellFactory extends AbstractCellFactory {
	private int m_col;
	private String[] m_wanted;
	private HashSet<String>            want = new HashSet<String>();
	private ArrayList<String> wanted_tracks = new ArrayList<String>();
	private NodeLogger             m_logger;
	private HashMap<String,Integer>  m_cols = new HashMap<String,Integer>();
    private DataTableSpec m_inspec;
    private MyDataContainer   m_track_table = null;
    
	public Sequence2ColumnsCellFactory(int col, String[] wanted, NodeLogger logger, DataTableSpec inSpec) {
		m_col = col;
		m_wanted = wanted;
		m_logger = logger;
		m_inspec = inSpec;
		
	    for (String s : wanted) {
    	   m_logger.info("Extracting \'"+s+"\' into columns");
    	   if (s.startsWith("Track - ")) {
    		   wanted_tracks.add(s.substring("Track - ".length()));
    	   } else {
    		   want.add(s);
    	   }
	    }
	}
	

	/**
	 * Returns a <code>DataTableSpec</code> suitable for making an output table for storing the features
	 * associated with the desired tracks for extraction. The sequence ID is included so you know which sequence
	 * gave rise to a feature.
	 * @return
	 */
	public DataTableSpec getTrackColumnSpec() {
		boolean added_id = false;
		ArrayList<DataColumnSpec> cols = new ArrayList<DataColumnSpec>();
		for (String w : wanted_tracks) {
			DataColumnProperties tcp = m_inspec.getColumnSpec(m_col).getProperties();
			if (tcp != null) {
				SequenceAnnotation sa = SequenceAnnotation.make(tcp, w);
				if (sa != null) {
					if (!added_id) {
						added_id = true;
						cols.add(new DataColumnSpecCreator(Sequence2StringsNodeModel.SEQUENCE_ID, StringCell.TYPE).createSpec());
					}
					cols.addAll(sa.asColumnSpec(w));
				}
			}
		} 
		return new DataTableSpec(cols.toArray(new DataColumnSpec[0]));
	}

	/**
     * KNIME requires at least one column, so we guarantee that despite user-configuration
     * @param spec
     * @param wanted 
     * @return
     * @throws InvalidSettingsException 
     */
	@Override
	public DataColumnSpec[] getColumnSpecs() {
		if (m_wanted == null || m_wanted.length < 1) {
	    	DataColumnSpec[] cols = new DataColumnSpec[1];
	    	cols[0] = new DataColumnSpecCreator(Sequence2StringsNodeModel.SEQUENCE_ID, StringCell.TYPE).createSpec();
			return cols;
		}
		// else...
		
		ArrayList<DataColumnSpec> cols = new ArrayList<DataColumnSpec>();
		for (String w : m_wanted) {
			DataType type = StringCell.TYPE;
			if (w.equals(Sequence2StringsNodeModel.INPUT_SEQUENCE)) {
				DataColumnSpecCreator my_annot_spec = new DataColumnSpecCreator("Annotated Sequence", SequenceCell.TYPE);
				
				// we must keep the list of tracks as a property associated with the column. We must do this
				// so that annotation dialogs can correctly display the tracks for the user
				if (m_col >= 0) {
					my_annot_spec.setProperties(m_inspec.getColumnSpec(m_col).getProperties());
				}
				cols.add(my_annot_spec.createSpec());
			} else if (w.equals(Sequence2StringsNodeModel.SEQUENCE_LENGTH)) {
				cols.add(new DataColumnSpecCreator(w, IntCell.TYPE).createSpec());
			} else if (w.startsWith("Track - ")) {
				// do nothing, as it appears in the second output port which is handled separately
			} else {
				cols.add(new DataColumnSpecCreator(w, type).createSpec());
			}
		}
    	
		// build up m_cols member from cols array (don't do it if called more than once)
		if (m_cols.size() == 0) {
			int col_idx = 0;
			for (DataColumnSpec colSpec : cols) {
		    	   m_cols.put(colSpec.getName(), new Integer(col_idx++));
		    }
		}
		 
		return cols.toArray(new DataColumnSpec[0]);
	}
	
	protected DataCell[] missing_cells(int n) {
		assert(n > 0);
		DataCell[] ret = new DataCell[n];
		for (int i=0; i<ret.length; i++) {
			ret[i] = DataType.getMissingCell();
		}
		return ret;
	}
	
	private MyDataContainer getTrackContainer() {
		return m_track_table;
	}
	
	public void setTrackContainer(MyDataContainer track_table) {
		m_track_table = track_table;
	}
	
	private void add_track_row(Map<String,DataCell> cells) {
		MyDataContainer track_table = getTrackContainer();
		if (track_table == null)
			return;
		
		DataTableSpec spec = track_table.getTableSpec();
		DataCell[] ocells = missing_cells(spec.getNumColumns());
		for (String colName : cells.keySet()) {
			int idx = spec.findColumnIndex(colName);
			if (idx >= 0) {
				ocells[idx] = cells.get(colName);
			}
		}
		track_table.addRow(ocells);
	}
	
	/**
	 * Append the necessary columns to the input table as specified by the internal state of the factory
	 */
	@Override
	public DataCell[] getCells(DataRow row) {
		DataCell c = row.getCell(m_col);
		if (c == null || c.isMissing() || !(c instanceof SequenceValue))
			return missing_cells(getColumnSpecs().length);
		SequenceValue sv = (SequenceValue) c;
		
		Map<String,DataCell> ocells = new HashMap<String,DataCell>();
		
		try { 
		    if (want.contains(Sequence2StringsNodeModel.SEQUENCE_ID)) {
			   ocells.put(Sequence2StringsNodeModel.SEQUENCE_ID, new StringCell(sv.getID()));
		    } 
		    if (want.contains(Sequence2StringsNodeModel.SEQUENCE_SINGLE_LETTER)) {
			   ocells.put(Sequence2StringsNodeModel.SEQUENCE_SINGLE_LETTER, new StringCell(sv.getStringValue()));
		    }
		    if (want.contains(Sequence2StringsNodeModel.DESCRIPTION_PRIMARY) && sv.hasDescription()) {
			   ocells.put(Sequence2StringsNodeModel.DESCRIPTION_PRIMARY, new StringCell(sv.getDescription()));
		    }
		    if (want.contains(Sequence2StringsNodeModel.INPUT_SEQUENCE)) {
			   ocells.put(Sequence2StringsNodeModel.INPUT_SEQUENCE, new SequenceCell(sv));
		    }
		    if (want.contains(Sequence2StringsNodeModel.SEQUENCE_LENGTH)) {
			   ocells.put(Sequence2StringsNodeModel.SEQUENCE_LENGTH, new IntCell(sv.getLength()));
		    }
		    
		    // find track data (if requested by the user)
		    if (wanted_tracks.size() > 0) {
			    for (String key : wanted_tracks) {
					   Track t = sv.getTrackByName(key, null);
					   if (t != null) {
						   SequenceAnnotation      sa = t.getAnnotation();
						   for (RegionInterface ri : sa.getRegions()) {
				    			Map<String,DataCell> cells = ri.asCells(t.getName());
				    			cells.put(Sequence2StringsNodeModel.SEQUENCE_ID, new StringCell(sv.getID()));
				    			add_track_row(cells);
				    	   }
					   }
			   }
			   // FALL-THRU
		    } 
		    
		    // no tracks so just output the row
		    return row(ocells, m_cols);
		} catch (InvalidSettingsException ise) {
			ise.printStackTrace();
			return missing_cells(getColumnSpecs().length);
		}
	}

	/**
	 * Return a row for inclusion in the first output port by mapping each cell instance to the corresponding column
	 * @param ocells
	 * @param colmap
	 * @return
	 */
	private DataCell[] row(final Map<String, DataCell> ocells, final Map<String,Integer> colmap) {
   	       DataCell[] cells = new DataCell[getColumnSpecs().length];
		   for (int i=0; i<cells.length; i++) {
			   cells[i] = DataType.getMissingCell();
		   }
		   for (String key : ocells.keySet()) {
			   if (colmap.containsKey(key)) {
				   Integer i = colmap.get(key);
				   cells[i.intValue()]  = ocells.get(key);
			   }
		   }
		   return cells;
	}

}
