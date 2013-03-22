package au.edu.unimelb.plantcell.core.biojava.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 *  Speed is somewhat important here, for large sequence databases (eg. short reads from NextGenSeq etc.)
 *  
 * @author acassin
 *
 */
public class ResidueFrequencyTask extends BioJavaProcessorTask {
	private boolean m_single_residue, m_tri_mers;
	private final HashMap<String, Integer> m_colmap = new HashMap<String,Integer>(); // maps column name (ie. symbol name) to a corresponding column id
	private String m_task;
	
	public ResidueFrequencyTask() {
	}
	
	@Override
	public String getCategory() {
		return "Common";
	}
	
	@Override
	public boolean canWindow() {
		return true;
	}
	
	public static BioJavaProcessorTask getInstance() {
		return new ResidueFrequencyTask();
	}
	
	@Override
	public void init(String task, int col) throws Exception {
		super.init(task, col);
		m_single_residue = task.startsWith("Count Residues");
		m_tri_mers       = (task.toLowerCase().indexOf("tri-mers") >= 0);
		m_colmap.clear();
		m_task = task;
	}
	
	/** {@inheritDoc} */	
	@Override
	public String[] getNames() {
		 return new String[] { 
		    "Count Residues", 
		    "Count Residues (% of total length)",
	        "Count Di-mers (overlapping)",
	        "Count Tri-mers (overlapping, Protein)"
		 };
	}
	
	/** {@inheritDoc} */
	@Override
	public String getHTMLDescription(String task) {
		return "<html>Totals the occurrence of each residue (DNA+IUPAC or AA) for the " +
				"set of sequences. Ambiguous calls (N etc.) are included. It is also possible "+
				"to compute the overlapping di-mer frequencies for a set of sequences and tri-mer " +
				"frequencies for protein sequences";
	}
	
	@Override
	public DataCell[] getCells(DataRow row) {
		int[] vec = new int[m_colmap.size()];
		DataCell[] cells = missing_cells(vec.length);
		
		// process rows for user's dataset
		SequenceValue sv = getSequenceForRow(row);
		if (sv == null) {
			return cells;
		}
		String seq = sv.getStringValue().toUpperCase();
		int len = seq.length();		// NB: NOT equivalent to sv.getLength() when using the window analysis node
		
		if (m_tri_mers && len >= 3) {
			for (int i=0; i<len-2; i++) {
				String trimer = seq.substring(i,i+3);
				Integer column_idx = m_colmap.get(trimer);
				assert(column_idx != null);
				if (column_idx == null)
					continue;
				vec[column_idx.intValue()]++;
			}
			for (String s : m_colmap.keySet()) {
				int idx = m_colmap.get(s);
				cells[idx] = new IntCell(vec[idx]);
			}
			return cells;
		} else if (m_single_residue) {			
			String[] id = new String[m_colmap.size()];
			
			// populate id array
			Iterator<String> iid = m_colmap.keySet().iterator();
			int j = 0;
			while (iid.hasNext()) {
				String col_id = iid.next();
				id[j++] = col_id;
			}
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
					if (m_task.indexOf("%") >= 0) {
						cells[column_idx.intValue()] = new DoubleCell(((double)cnt) * 100.0 / len);
					} else {
						cells[column_idx.intValue()] = new IntCell(cnt);
					}
				}
			}
			return cells;
		} else {
			// di-mer/di-peptide composition?
			int[] values = new int[vec.length];
			for (int k=0; k<cells.length; k++) {
				values[k] = 0;
			}
			for (int k=0; k<seq.length()-1; k++) {
				StringBuffer sb = new StringBuffer();
				sb.append(seq.charAt(k));
				sb.append(seq.charAt(k+1));
				String dimer = sb.toString();
				
				if (m_colmap.containsKey(dimer)) {
					Integer column_idx = m_colmap.get(dimer);
					values[column_idx.intValue()]++;
				}
			}
			
			for (int k=0; k<cells.length; k++) {
				cells[k] = new IntCell(new Integer(values[k]));
			}
			return cells;
		}
	}

	@Override
	public DataColumnSpec[] getColumnSpecs() {
		
		// tri-mer AA table?
		if (m_task.toLowerCase().indexOf("tri-mers") >= 0) {
			char[] aa = new char[] { 'A', 'R', 'N', 'D', 'C', 'E', 'Q', 'G', 'H', 'I', 'L', 'K', 'M', 'F', 'P', 'S', 'T', 'W', 'Y', 'V' };
			ArrayList<DataColumnSpec> ret = new ArrayList<DataColumnSpec>();
			int idx=0;
			for (char a : aa) {
				for (char b : aa) {
					for (char c : aa) {
						String name = ""+a+b+c;
						m_colmap.put(name, new Integer(idx++));
						ret.add(new DataColumnSpecCreator(name, IntCell.TYPE).createSpec());
					}
				}
			}
			return ret.toArray(new DataColumnSpec[0]);
		}
		
		// report any strange characters in whatever sequence type for QA purposes
		DataColumnSpec[] cols;
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
			if (m_task.indexOf("%") >= 0) {
				cols[k] = new DataColumnSpecCreator(colname+"%", DoubleCell.TYPE).createSpec();
			} else {
				cols[k] = new DataColumnSpecCreator(colname, IntCell.TYPE).createSpec();
			}
		}
		return  cols;
	}
}
