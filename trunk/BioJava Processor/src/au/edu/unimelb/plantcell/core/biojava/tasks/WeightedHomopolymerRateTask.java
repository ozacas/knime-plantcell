package au.edu.unimelb.plantcell.core.biojava.tasks;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

public class WeightedHomopolymerRateTask extends BioJavaProcessorTask {
	
	public WeightedHomopolymerRateTask() {
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
		return new WeightedHomopolymerRateTask();
	}
	
	/** @InheritDoc */
	@Override
	public String[] getNames() {
		return new String[] { "Weighted Homopolymer Rate (WHR)" };
	}
	
	/**
	 * @InheritDoc 
	 */
	@Override
	public String getHTMLDescription(String task) {
		return "<html>Computes the WHR as described at <a href=\"http://www.broadinstitute.org/crd/wiki/index.php/Homopolymer\">http://www.broadinstitute.org/crd/wiki/index.php/Homopolymer</a>"+
			"<br/><br/>Only for Nucleotide sequences.";
	}
	
	@Override
	public DataCell[] getCells(DataRow row) {
		SequenceValue sv = getSequenceForRow(row);
		if (sv == null || !sv.getSequenceType().isNucleotides()) 
			return missing_cells(getColumnSpecs().length);
		
		String seq = sv.getStringValue().toUpperCase();
		// calculation as per http://www.broadinstitute.org/crd/wiki/index.php/Homopolymer
		int idx = 0;
		int cnt = 0;
		int sum = 0;
		int seq_len = seq.length();
		boolean bad = false;
		while (idx < seq_len) {
			cnt++;
			char residue = seq.charAt(idx);
			if (residue != 'A' && residue != 'C' && residue != 'T' && residue != 'G') {
				bad = true;
			}
			idx++;
			int len = 1;
			while (idx < seq_len && seq.charAt(idx) == residue) {
				idx++;
				len++;
			}
			sum += len * len;
		}
		DataCell[] cells = new DataCell[1];
		if (bad) {	
			cells[0] = DataType.getMissingCell();
			//total_bad++;
		} else {
			double whr = ((double)sum) / cnt;
			cells[0] = new DoubleCell(whr);
		}
		
		//if (total_bad > 0) {
		//	l.warn(""+total_bad+" sequences had ambiguous/unknown base calls, they are ignored (missing WHR).");
		//}
		
		return cells;
	}

	@Override
	public DataColumnSpec[] getColumnSpecs() {
		DataColumnSpec[] cols = new DataColumnSpec[1];
		cols[0] = new DataColumnSpecCreator("Weighted Homopolymer Rate (WHR)", DoubleCell.TYPE).createSpec();
		return cols;
	}

}
