package au.edu.unimelb.plantcell.core.biojava.tasks;

import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Emits metrics to aid in identification of HRGP proteins. These metrics are aimed at computing
 * the %PAST, %P, %PSYK and %PVK within windows and identification of rich "regions" of these metrics.
 * the task provides highlighted regions and numeric metrics for subsequent use.
 * 
 * @author andrew.cassin
 *
 */
public class HRGPScreenTask extends BioJavaProcessorTask {
	private int m_past, m_p, m_pvk, m_psyk;
	private int m_n_regions;
	
	@Override
	public String getCategory() {
		return "Protein Sequence Metrics";
	}
	
	@Override
	public String[] getNames() { 
		return new String[] { "Screen for Proline-rich HRGP-like proteins" };
	}
	
	@Override
	public DataColumnSpec[] getColumnSpecs() {
		DataColumnSpec[] cols = new DataColumnSpec[5];
		cols[0] = new DataColumnSpecCreator("Region of interest (HTML)", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Window lengths satisfying threshold", ListCell.getCollectionType(DoubleCell.TYPE)).createSpec();
		cols[2] = new DataColumnSpecCreator("RoI Coverage (%) of predicted protein", DoubleCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("Number of distinct regions satisfying threshold", IntCell.TYPE).createSpec();
		cols[4] = new DataColumnSpecCreator("Window size (AA)", IntCell.TYPE).createSpec();
		return cols;
	}
	
	@Override
	public String getHTMLDescription(String task) {
		return "<html>Adds metrics to aid in identification of proteins related to HRGP's (incl. Extensins, AGPs etc.). Requires protein sequence.";
	}
	
	
	@Override
	public DataCell[] getCells(DataRow r) {
		SequenceValue sv = getSequenceForRow(r);
		DataCell[] cells = missing_cells(getColumnSpecs().length);
		if (!sv.getSequenceType().isProtein())
			return cells;
		
		// compute windows
		int len = sv.getLength();
		int window_size = 8;
		if (len < window_size)		// not even a single full window available?
			return cells;
		
		DenseBitVector bv = new DenseBitVector(len);
		boolean got_window_start = false;
		int start_pos = -1;
		
		int n_windows = 0;
		String seq = sv.getStringValue();
		for (int i=0; i<len-window_size+1; i++) {
			if (!got_window_start) {
				String window = seq.substring(i, i+window_size);
				compute_window(window);	// updates m_*
				
				if (accept_window(window_size)) {
					got_window_start = true;
					n_windows++;
					start_pos = i;
					bv.set(i,i+window_size);
				}
				// reject current window and keep going...
			} else {
				String window = seq.substring(start_pos, i+window_size);
				compute_window(window);	// updates m_*
				
				if (accept_window(i+window_size-start_pos)) {
					bv.set(i, i+window_size);
				} else {
					got_window_start = false;
					start_pos = -1;
				}
			}
		}

		// to save disk space and speed calculation we just output results only if at least one window found
		if (n_windows > 0) {
			cells[0] = getHTMLCell(seq, bv);
			cells[1] = getCollectionCell(bv);		// side effects m_n_windows
			cells[2] = new DoubleCell(((double)bv.cardinality()) / len * 100.0d);
			cells[3] = new IntCell(m_n_regions);
			cells[4] = new IntCell(window_size);
		} 
		return cells;
	}
	
	private DataCell getCollectionCell(DenseBitVector bv) {
		ArrayList<IntCell> col = new ArrayList<IntCell>();
		long start = 0;
		while ((start = bv.nextSetBit(start)) >= 0) {
			long end = bv.nextClearBit(start+1);
	
			if (end < 0) {
				col.add(new IntCell((int)(bv.length()-start)));
		
				break;
			} else {
				col.add(new IntCell((int)(end-start)));
				start = end;
			}
		}
		if (col.size() < 1)
			return DataType.getMissingCell();
		m_n_regions = col.size();
		return CollectionCellFactory.createListCell(col);
	}

	private DataCell getHTMLCell(String seq, DenseBitVector bv) {
		if (seq == null || bv == null || seq.length() != bv.length())
			return DataType.getMissingCell();
		StringBuilder sb = new StringBuilder(seq.length());
		sb.append("<html>");
		for (int i=0; i<seq.length(); i++) {
			char c = seq.charAt(i);
			if (bv.get(i)) {
				sb.append("<font color=\"blue\">");
				sb.append(c);
				sb.append("</font>");
			} else {
				sb.append(c);
			}
		}
		
		return new StringCell(sb.toString());
	}

	private boolean accept_window(int window_size) {
		double percent_p = ((double)m_p)/window_size * 100;
		if (percent_p < 20.0d)
			return false;
		
		double percent_past = ((double)m_past)/window_size * 100.0d;
		if (percent_past >= 70.0d && percent_p >= 20.0d) {
			return true;
		} 
		double percent_psyk = ((double)m_psyk)/window_size * 100.0d;
		if (percent_psyk >= 70.0d && percent_p >= 20.0d) {
			return true;
		}
		double percent_pvk = ((double)m_pvk)/window_size * 100.0d;
		if (percent_pvk >= 70.0d && percent_p >= 20.0d)
			return true;
		return false;
	}

	private void compute_window(String window) {
		assert(window != null && window.length() > 0);
		m_past = 0;
		m_p    = 0;
		m_pvk  = 0;
		m_psyk = 0;
		for (int j=0; j<window.length(); j++) {
			char c = window.charAt(j);
			if (c == 'P') {
				m_past++;
				m_p++;
				m_pvk++;
				m_psyk++;
				continue;
			}
			if (c == 'A' || c == 'S' || c == 'T')
				m_past++;
			
			if (c == 'V' || c == 'K')
				m_pvk++;
			if (c == 'S' || c == 'Y' || c == 'K')
				m_psyk++;
		}
	}

}
