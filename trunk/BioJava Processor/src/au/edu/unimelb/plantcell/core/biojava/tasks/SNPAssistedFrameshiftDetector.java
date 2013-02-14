package au.edu.unimelb.plantcell.core.biojava.tasks;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.IntCell;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.misc.biojava.ModeSummary;

/**
 * SNPs are frequently, but not always, associated with the minor base in a codon. 
 * We use this to identify when a frameshift occurs. This task relies on the sequence
 * data (usually from next gen sequencing) using an IUPAC code to denote a SNP. Once
 * this is done, this task performs simple statistics to estimate the number of frameshifts
 * present in the sequence. There is no indication (yet) of where in the sequence the frameshift occurs.
 * 
 * @author andrew.cassin
 *
 */
public class SNPAssistedFrameshiftDetector extends BioJavaProcessorTask {
	
	public SNPAssistedFrameshiftDetector() {
	}
	
	@Override
	public String getCategory() {
		return "Frame shifts";
	}
	
	public static BioJavaProcessorTask getInstance() {
		return new SNPAssistedFrameshiftDetector();
	}
	
	/** @InheritDoc */
	@Override
	public String[] getNames() {
		return new String[] { "SNP-assisted frameshift detection" };
	}
	
	/** @InheritDoc */
	@Override
	public String getHTMLDescription(String task) {
		return "<html>An <b>experimental algorithm</b> to identify frameshifts resulting from indels based "+
		"on IUPAC code presence in the least-significant base of codons. The output is a column with "+
		"the number of detected frameshifts present in the input sequence. Only for DNA sequences.";
	}
	
	@Override
	public DataCell[] getCells(DataRow row) {
		SequenceValue sv = getSequenceForRow(row);
		if (sv == null || sv.getLength() < 1 || sv.getSequenceType().isDNA())
			return missing_cells(getColumnSpecs().length);
	
		DataCell[] cells = new DataCell[1];
			
		int[] codon_pos = new int[sv.getLength()];
		int codon_idx = 0;
		String seq = sv.getStringValue().toUpperCase();
		for (int i=0; i<sv.getLength(); i++) {
			char ch = seq.charAt(i);
			if (ch != 'A' && ch != 'T' && ch !='G' && ch != 'C' ) {
				// TODO
				//if (!Character.isLetter(ch))
				//	throw new Exception("Bad char: "+ (int) ch+" (encountered in sequence" + sv.getID() +")");
				codon_pos[codon_idx++] = (i+1) % 3;
			} 
		}
		// according to Andreas, SNPs are mostly in the 3rd nucleotide per codon. If this
		// holds true then a windowed-mode should yield the region of a frame shift.
		// Obviously, this doesn't help much when no SNP's are available or are too sparsely
		// distributed amongst the sequence
		cells[0]         = new IntCell(0);
		StringBuffer codon_str = new StringBuffer();
		for (int i=0; i<codon_idx; i++) {
			codon_str.append(codon_pos[i]);
		}
		// now compute the mode with a window size of 3
		StringBuffer mode_str = new StringBuffer();
		int n_modes = 0;
		for (int i=0; i<codon_str.length()-2; i++) {
			ModeSummary ms = new ModeSummary(codon_str.charAt(i),
						codon_str.charAt(i+1),
						codon_str.charAt(i+2)
					);
			mode_str.append(ms.toString());
			mode_str.append(", ");
			n_modes++;
		}
		if (n_modes < 3) {
		}
		
		return missing_cells(getColumnSpecs().length);
	}

	@Override
	public DataColumnSpec[] getColumnSpecs() {
		DataColumnSpec[] cols = new DataColumnSpec[2];
		cols[0] = new DataColumnSpecCreator("Number of detected frameshifts", IntCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Number of unknown/illegal residues encountered", IntCell.TYPE).createSpec();
		
		//cols[1] = new DataColumnSpecCreator("Debug", StringCell.TYPE).createSpec();
		return cols;
	}
}
