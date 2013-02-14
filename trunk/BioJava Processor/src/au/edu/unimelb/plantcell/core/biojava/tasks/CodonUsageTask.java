package au.edu.unimelb.plantcell.core.biojava.tasks;

import java.util.ArrayList;
import java.util.HashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.IntCell;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Report the count of codons in the specified DNA/RNA sequence. T&U are equivalent are
 * far as the column names and counts are concerned.
 * 
 * @author andrew.cassin
 *
 */
public class CodonUsageTask extends BioJavaProcessorTask {
	private final HashMap<String,Integer> codon2idx = new HashMap<String,Integer>();
	
	public CodonUsageTask() {
	}
	
	@Override
	public DataCell[] getCells(DataRow row) {
		SequenceValue sv = getSequenceForRow(row);
		
		int n_cols = this.getColumnSpecs().length;
		if (sv == null || sv.getSequenceType().isProtein()) {
			return missing_cells(n_cols);
		}
		
		DataCell[] cells = missing_cells(n_cols);
		HashMap<String,Integer> counts = new HashMap<String,Integer>();
		try {
			String seq = sv.getStringValue();
			int seq_len = sv.getLength();
			int len = seq_len;
			if (len % 3 != 0) {
				len -= (seq_len % 3);
			}
			for (int i=0; i<len; i+=3) {
				String codon = ""+seq.charAt(i) + 
								seq.charAt(i+1) + 
								seq.charAt(i+2);
				
				Integer idx = codon2idx.get(codon);		// find column 
				if (idx == null) {
					idx = codon2idx.get("Other");
					assert(idx != null);
					codon = "Other";
				}
				
				Integer cnt = counts.get(codon);		// first time codon has been seen?
				if (cnt == null) {
					cnt = new Integer(1);
				} else {
					cnt = new Integer(cnt.intValue()+1);
				}
				counts.put(codon, cnt);
			}
			
			for (String codon : counts.keySet()) {
				cells[codon2idx.get(codon)] = new IntCell(counts.get(codon));
			}
		} catch (Exception e) {
			e.printStackTrace();
			return missing_cells(n_cols);
		} 
		return cells;
	}

	@Override
	public String getCategory() {
		return "Basic";
	}

	@Override
	public String[] getNames() {
		return new String[] { "Codon usage table" };
	}
	
	@Override
	public String getHTMLDescription(String task) {
		return "<html>Computes the codon usage using <b>only</b> the +1 frame for any DNA/RNA sequence";
	}
	
	@Override
	public DataColumnSpec[] getColumnSpecs() {
		// output every possible [ACGT]{3} combination and and other count
		codon2idx.clear();
		int idx = 0;
		ArrayList<DataColumnSpec> ret = new ArrayList<DataColumnSpec>();
		for (char c1 : new char[]{ 'A', 'C', 'G', 'T'}) {
			for (char c2 : new char[]{ 'A', 'C', 'G', 'T'}) {
				for (char c3 : new char[]{ 'A', 'C', 'G', 'T'}) {
					String codon = ""+c1+c2+c3;
					ret.add(new DataColumnSpecCreator(codon, IntCell.TYPE).createSpec());
					codon2idx.put(codon, new Integer(idx++));
				}
			}
		}
		ret.add(new DataColumnSpecCreator("Other", IntCell.TYPE).createSpec());
		return ret.toArray(new DataColumnSpec[0]);
	}
}
