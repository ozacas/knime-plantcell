package au.edu.unimelb.plantcell.misc.biojava;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 *  Speed is important here, for large sequence databases (eg. short reads from Solexa etc.)
 *  
 * @author acassin
 *
 */
public class ResidueFrequencyProcessor extends BioJavaProcessorTask {
	private boolean m_single_residue;
	private final HashMap<String, Integer> m_colmap = new HashMap<String,Integer>();		    // maps column name (ie. symbol name) to a corresponding column id

	public ResidueFrequencyProcessor() {
	}
	
	@Override
	public String getCategory() {
		return "Statistics";
	}
	
	public static BioJavaProcessorTask getInstance() {
		return new ResidueFrequencyProcessor();
	}
	
	public void init(BioJavaProcessorNodeModel owner, String task) {
		setOwner(owner);
		m_single_residue = task.equals("Count Residues");
		m_colmap.clear();
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
	public void execute(ColumnIterator ci, ExecutionContext exec,
			NodeLogger l, BufferedDataTable[] inData, BufferedDataContainer c1)
			throws Exception {
		
		int n = inData[0].getRowCount();
		int i = 0;
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
		if (m_single_residue) {
			while (ci.hasNext()) {
				DataCell c = ci.next();
				if (c == null || c.isMissing())
					continue;
				i++;
				
				String seq = c.toString();
				
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
				
				c1.addRowToTable(new JoinedRow(ci.lastRow(), new DefaultRow(ci.lastRowID(), cells)));

				if (i % 1000 == 0) {
					exec.checkCanceled();
					exec.setProgress(((double) i)/n, "Processed "+i+" sequences");
				}
			}	
		} else {
			// di-mer/di-peptide composition?
			while (ci.hasNext()) {
				DataCell c = ci.next();
				if (c == null || c.isMissing() || !(c instanceof SequenceValue))
					continue;
				
				i++;
				SequenceValue sv = (SequenceValue) c;
				String seq = sv.getStringValue().toUpperCase();
				
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
				c1.addRowToTable(new JoinedRow(ci.lastRow(), new DefaultRow(ci.lastRowID(), knime_cells)));

				if (i % 1000 == 0) {
					exec.checkCanceled();
					exec.setProgress(((double) i)/n, "Processed "+i+" sequences");
				}
			}	
		}
	}

	@Override
	public DataTableSpec get_table_spec() {
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
		return new DataTableSpec(cols);
	}

	@Override
	public boolean isMerged() {
		return true;
	}

}
