package au.edu.unimelb.plantcell.core.biojava.tasks;

import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.InvalidSettingsException;

import au.edu.unimelb.plantcell.core.cells.Comment;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Removes extraneous and unwanted features from sequence data which can interfere with some 
 * programs operation. HACK: needs to be more configurable and capable!
 * 
 * @author andrew.cassin
 *
 */
public class SequenceCleanerTask extends BioJavaProcessorTask {
	private final Set<Character> m_dna = new HashSet<Character>();
	private final Set<Character> m_rna = new HashSet<Character>();
	private final Set<Character> m_aa  = new HashSet<Character>();
	
	@Override
	public String getCategory() {
		return "Formatting";
	}
	
	@Override
	public void init(String task_name, int col) throws Exception {
		super.init(task_name, col);
		// DNA
		for (char c : new char[] { 'A', 'C', 'G', 'T' }) {
			m_dna.add(new Character(c));
		}
		// IUPAC letters for unknown bases (non-strict mode)
		for (char c : new char[] { 'N', 'R', 'Y', 'S', 'W', 'K', 'M', 'B', 'D', 'H', 'V'}) {
			m_dna.add(new Character(c));
		}
		
		// RNA
		for (char c : new char[] { 'A', 'C', 'G', 'U', 'N' }) {
			m_rna.add(new Character(c));
		}

		// Protein
		for (char c: new char[] {
					'A', 'R', 'N', 'D', 'C', 'E', 'Q', 'G', 'H', 'I', 'L',
					'K', 'M', 'F', 'P', 'S', 'T', 'W', 'Y', 'V', 'X'
		}) {
				m_aa.add(new Character(c));
		}
	}

	@Override
	public DataColumnSpec[] getColumnSpecs() {
		DataColumnSpec[] cols = new DataColumnSpec[2];
		cols[0] = new DataColumnSpecCreator("Cleaned Sequence (no annotations)", SequenceCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Residues changed/deleted", IntCell.TYPE).createSpec();
		return cols;
	}

	public String[] getNames() { 
		return new String[] {"Clean sequences (whitespace removal, non-coding symbols, stop codon->X conversion, all uppercase)"}; 
	}
	
	public String getHTMLDescription(String task) {
		return "<html>Performs a series of simple tasks to the chosen column of sequences:" +
				"<ul>" +
				"<li>Removal of all whitespace (space, tab, other)</li>"+
				"<li>Conversion of all residue symbols to uppercase</li>"+
				"<li>Removal of trailing stop codon(s). Conversion of remaining stop codons to X</li>"+
				"<li>Removal of non-coding symbols for the chosen sequence type. A count " +
				" of any residues removed from sequences appears in a separate column.</li>"  +
				"<li>Removal of all graphical annotations</li>"+
				"</ul>";
	}
	
	@Override
	public DataCell[] getCells(DataRow row) {
		SequenceValue sv = getSequenceForRow(row);
		if (sv == null)
			return missing_cells(getColumnSpecs().length);
		
		String seq = sv.getStringValue().toUpperCase();
		int before_len = seq.length();
		int rejected = 0;
		seq = seq.replaceAll("\\*+$", "");			// remove trailing stop codons
		if (seq.length() != before_len) {
			rejected += before_len - seq.length();
			before_len = seq.length();
		}
		seq = seq.replaceAll("\\s+", "");
		if (before_len != seq.length()) {
			rejected += before_len - seq.length();
		}
		// BUG: reject is not altered for these two steps
		seq = seq.replaceAll("\\*", "X");
		seq = seq.toUpperCase();
		
		SequenceType st = sv.getSequenceType();
		Set<Character> letters = m_dna;
		if (st.isDNA()) {
			letters = m_dna;
		} else if (st.isRNA()) {
			letters = m_rna;
		} else {		// assume protein
			letters = m_aa;
		}
		StringBuffer sb = new StringBuffer(seq.length());
		for (int i=0; i<seq.length(); i++) {
			Character c2 = new Character(seq.charAt(i));
			if (Character.isLetter(c2)) {
				if (!letters.contains(c2)) {     // AA
					rejected++;
				} else {
					sb.append(c2);
				}
			} else if (c2 == '-' || c2 == '.') {		// pass gap characters through unchanged
				sb.append(c2);
			} else {									// else... reject and suppress
				rejected++;
			}
		}
		
		DataCell[] cells = new DataCell[2];
		try {
			SequenceCell sc = new SequenceCell(st, sv.getID(), sb.toString());
			if (sc.hasDescription()) {
				sc.addComment(new Comment(sc.getDescription()));
			}
			cells[0] = sc;
		} catch (InvalidSettingsException e) {
			cells[0] = DataType.getMissingCell();
			e.printStackTrace();
		}
		cells[1] = new IntCell(rejected);
		return cells;
	}
}
