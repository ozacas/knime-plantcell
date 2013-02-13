package au.edu.unimelb.plantcell.core.biojava.tasks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.IntCell;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.misc.biojava.BioJavaProcessorNodeModel;

/**
 *  Speed is important here, for large sequence databases (eg. short reads from NextGenSeq etc.)
 *  
 * @author acassin
 *
 */
public class ResidueFrequencyTask extends BioJavaProcessorTask {
	private boolean m_single_residue;
	private final HashMap<String, Integer> m_colmap = new HashMap<String,Integer>(); // maps column name (ie. symbol name) to a corresponding column id
	private int m_col = -1;
	
	public ResidueFrequencyTask() {
	}
	
	@Override
	public String getCategory() {
		return "Statistics";
	}
	
	public static BioJavaProcessorTask getInstance() {
		return new ResidueFrequencyTask();
	}
	
	public void init(BioJavaProcessorNodeModel owner, String task, int col) {
		m_single_residue = task.equals("Count Residues");
		m_colmap.clear();
		m_col = col;
	}
	
	/** {@inheritDoc} */	
	@Override
	public String[] getNames() {
		 return new String[] { 
		    "Count Residues", 
	        "Count Di-mers (overlapping)" 
		 };
	}
	
	/** {@inheritDoc} */
	@Override
	public String getHTMLDescription(String task) {
		return "<html>Totals the occurrence of each residue (DNA+IUPAC or AA) for the " +
				"set of sequences. Ambiguous calls (N etc.) are included. It is also possible "+
				"to compute the overlapping di-mer frequencies for a set of sequences";
	}
	
	@Override
	public DataCell[] getCells(DataRow row) {
		int[] vec = new int[m_colmap.size()];
		String[] id = new String[m_colmap.size()];
		
		// populate id array
		Iterator<String> iid = m_colmap.keySet().iterator();
		int j = 0;
		while (iid.hasNext()) {
			String col_id = iid.next();
			id[j++] = col_id;
		}
		
		// process rows for user's dataset
		DataCell c = row.getCell(m_col);
		if (c == null || c.isMissing() || !(c instanceof SequenceValue))
			return missing_cells(m_colmap.size());
		SequenceValue sv = (SequenceValue) c;
		String seq = sv.getStringValue().toUpperCase();
		
		if (m_single_residue) {			
			DataCell[] cells = new DataCell[vec.length];
			for (int k=0; k<vec.length; k++) {
				int cnt = 0;
				String colname = id[k];
				assert(colname.length() == 1);
				char ch = colname.charAt(0);
				for (int m2=0; m2<seq.length(); m2++) {
					if (seq.charAt(m2) == ch) 
						cnt++;
				}
				
				if (m_colmap.containsKey(colname)) {
					Integer column_idx = m_colmap.get(colname);
					cells[column_idx.intValue()] = new IntCell(cnt);
				}
			}
			return cells;
		} else {
			// di-mer/di-peptide composition?
			int[] cells = new int[vec.length];
			for (int k=0; k<cells.length; k++) {
				cells[k] = 0;
			}
			for (int k=0; k<seq.length()-1; k++) {
				StringBuffer sb = new StringBuffer();
				sb.append(seq.charAt(k));
				sb.append(seq.charAt(k+1));
				String dimer = sb.toString();
				
				if (m_colmap.containsKey(dimer)) {
					Integer column_idx = m_colmap.get(dimer);
					cells[column_idx.intValue()]++;
				}
			}
			
			DataCell[] knime_cells = new DataCell[cells.length];
			for (int k=0; k<cells.length; k++) {
				knime_cells[k] = new IntCell(new Integer(cells[k]));
			}
			return knime_cells;
		}
	}

	@Override
	public DataColumnSpec[] getColumnSpecs() {
		DataColumnSpec[] cols;
		
		// report any strange characters in whatever sequence type for QA purposes
	
		char[] vec = new char[26];
		int idx = 0;
		for (char c= 'A'; c<= 'Z'; c++) {
			vec[idx++] = c;
		}
	
		int k = 0;
		for (char i : vec) {
			for (char j : vec) {
				StringBuffer tmp = new StringBuffer();
				tmp.append(i);
				if (!m_single_residue) {
					tmp.append(j);
				}
				String as_str = tmp.toString();
				if (!m_single_residue || (m_single_residue && i==j)) {
					// handle symettry eg. AA
					if (!m_colmap.containsKey(as_str)) {
						//System.err.println(as_str+ " "+k);
						m_colmap.put(as_str, new Integer(k));
						k++;
					}
				}
			}
		}
		
		// columns are built from the final map to avoid duplicates
		int n_cols = m_colmap.size();
		cols = new DataColumnSpec[n_cols];
		Set<String> colnames = m_colmap.keySet();
		
		for (String colname : colnames) {
			k = m_colmap.get(colname).intValue();
			cols[k] = new DataColumnSpecCreator(colname, IntCell.TYPE).createSpec();
		}
		return  cols;
	}
}
