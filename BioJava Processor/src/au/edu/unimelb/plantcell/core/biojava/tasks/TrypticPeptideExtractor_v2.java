package au.edu.unimelb.plantcell.core.biojava.tasks;

import java.util.HashSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;

/**
 * DISABLED UNTIL FURTHER NOTICE -- pending a refactoring of the biojava node
 * 
 * This class differs from the superclass in terms of processing at the DNA/RNA level where it
 * is more persistent to try to find suitable peptides and supports alternate initiation codons,
 * unlike the superclass.
 * 
 * @author andrew.cassin
 *
 */
public class TrypticPeptideExtractor_v2 extends TrypticPeptideExtractor {
	
	public TrypticPeptideExtractor_v2() {
		super();
	}
	
	@Override
	public String getCategory() {
		return "Reading Frames";
	}
	
	/**
	 * Contains all the unique RNA codons which can code for the tryptic terminii (K and/or R amino acids)
	 */
	private final HashSet<String> terminii_codons = new HashSet<String>();
	/**
	 * Since trypsin almost always does not cleave after a [KR] immediately followed by Proline (P) we
	 * store the proline codons in a hash set in order to model this behaviour 
	 */
	private final HashSet<String> proline_codons  = new HashSet<String>();
	
	@Override
	public void init(String task, int col) throws Exception {
		super.init(task, col);
		terminii_codons.add("AAG"); 	// K
		terminii_codons.add("AAA");
		terminii_codons.add("CGA");		// R
		terminii_codons.add("CGC");
		terminii_codons.add("CGG");
		terminii_codons.add("CGU");
		terminii_codons.add("AGA");
		terminii_codons.add("AGG");
		
		proline_codons.add("CCC");		// proline (p)
		proline_codons.add("CCG");
		proline_codons.add("CCA");
		proline_codons.add("CCU");
	}
	
	/** @InheritDoc */
	@Override
	public String[] getNames() {
        return new String[] { "Tryptic Peptide Extraction (all 6 frames iff DNA/RNA, supports IUPAC code conversion)" };
	}
	
	/** @InheritDoc */
	@Override
	public String getHTMLDescription(String task) {
		return "<html>An <b>experimental algorithm</b> to extract tryptic peptides from sequences (AA, DNA/RNA supported).\n"+
		"The algorithm can handle IUPAC codes for DNA/RNA by exhausting all alternatives and reporting valid\n"+
		"tryptic peptides. The algorithm distinguishes between several classes of tryptics: \n"+
		"<ul><li>Fully tryptic and unambiguous: no ambiguous base calls</li><li>Single missed cleavage</li>\n"+
		"<li>Weak tryptics (those with ambiguity at either end)</li></ul>";
	}
	
	/**
	 * Replaces the superclass implementation with an implementation that works directly at the
	 * RNA level and handles IUPAC ambiguity with codons essential to tryptic peptides eg. K/R. In this
	 * case the algorithm emits all possible peptides which are fully tryptic. 
	 */
	@Override
	public DataCell[] getCells(DataRow row) {
		return null;
		/*double done = 0.0;
		int n_rows = inData[0].getRowCount();
		while (ci.hasNext()) {
			DataCell c = ci.next();
			if (c == null || c.isMissing() || !(c instanceof SequenceValue))
				continue;
			SequenceValue sv = (SequenceValue) c;
			if (sv.getSequenceType().isProtein()) {
				super.grok_sequence(sv, l, c1);
				continue;
			}
			String[] na_seqs = new String[6];		// RNA reading frames if !is_prot (MUST be in the same frame order as seqs)
			if (sv.getLength() < 1)
				continue;
			
			SymbolList syms = asBioJava(sv);
			boolean is_dna  = sv.getSequenceType().isDNA();

			for (int i=0; i<3; i++) {
				// take the reading frame
				SymbolList rf = syms.subList(i+1, syms.length()-(syms.length() - i) % 3);
				
				// if it is DNA transcribe it to RNA first
				if (is_dna) {
					rf = DNATools.toRNA(rf);
				}
				na_seqs[i+3] = rf.seqString().toUpperCase();
				
				// reverse frame translation
				rf       = RNATools.reverseComplement(rf);
				na_seqs[i] = rf.seqString().toUpperCase();
			}
			
			// process the available protein sequences looking for tryptic peptides
			DataCell[] cells = new DataCell[5];
			cells[0] = new StringCell(sv.getStringValue());
			cells[1] = DataType.getMissingCell();
			cells[2] = DataType.getMissingCell();
			cells[3] = DataType.getMissingCell();
			cells[4] = DataType.getMissingCell();
			
			// unambiguous tryptics
			HashSet<String> unambg_tryptics = new HashSet<String>();
			for (String seq : na_seqs) {
				for (int i=0; i<seq.length(); i+= 3) {
					if (can_code_for(seq.substring(i, i+3), terminii_codons)) {
							int len = next_tryptic_terminii(seq.substring(i+3), terminii_codons);
							if (len > 0) {
								// HACK: superclass uses DNA Thiamine (which is wrong) in codon table 
								// rather than RNA Uracil, so we make sure this wont throw
								String codon_seq = seq.substring(i, i+(6+len*3)).replaceAll("U", "T");
								int codon_len = codon_seq.length();
								recurse(unambg_tryptics, new StringBuffer(codon_seq), 0, codon_len);
								
							}
					}
				}
			}
			if (unambg_tryptics.size() > 0) {
				cells[1] = populate_cell(unambg_tryptics, true);		// true == omit peptides with stop codons in results
			}
			
			c1.addRowToTable(new DefaultRow(ci.lastRowID(), cells));
			
			done++;
			if (((int)done) % 100 == 0) {
				exec.checkCanceled();
				exec.setProgress(done / n_rows);
			}
		}
	*/
	}

	private int next_tryptic_terminii(String codons, HashSet<String> terminii_codons) {
		for (int n_codons=6; n_codons<(TrypticPeptideExtractor.MAX_MEASURABLE_PEPTIDE_LENGTH_IN_AA-2); n_codons++) {
			int offset = n_codons*3;
			if (offset+3 > codons.length())
				return -1;
			if (can_code_for(codons.substring(offset, offset+3), terminii_codons)) {
				// check for proline after K/R and reject if we find one, otherwise accept it
				offset += 3;
				if (offset+3 > codons.length()) {
					// accept an end-of-sequence as not having a proline
					return n_codons;
				}
				if (can_code_for(codons.substring(offset, offset+3), proline_codons)) {
					return -1;
				}
				return n_codons;
			}
		}
		
		return -1;		// not found
	}

	/**
	 * Determines if the specified codon (eg. AAC) can code for any of the <code>wanted_codons</code>
	 * taking into account any IUPAC codes in the specified codon - this routine supports any base
	 * being called as ambiguous (or all of them) and will test all permutations
	 * 
	 * @param codon
	 * @param wanted_codons
	 * @return returns true if the specified codon satisfies the wanted codons, false otherwise
	 */
	private boolean can_code_for(String codon, HashSet<String> wanted_codons) {
		// ambiguous base call(s) in codon, but *could potentially* code for one of the wanted codons?
		assert(codon != null && codon.length() == 3);
		
		// if the codon has no ambiguity and codes for a wanted codon then we are done
		if (wanted_codons.contains(codon.toUpperCase())) {
			return true;
		}
		
		for (int i=0; i<3; i++) {
			char c = codon.charAt(i);
			if (c != 'A' && c != 'C' && c != 'G' && c != 'U') {
				return ok_recursive(0, new StringBuffer(codon), wanted_codons);
			}
		}
		
		// all failed...
		return false;
	}

	/**
	 * A helper function to <code>can_code_for()</code> this resolves the ambiguity and once
	 * resolved, tests the resulting codons against the desired <code>wanted_codons</code>
	 * 
	 * @param idx
	 * @param na_codon
	 * @param wanted_codons
	 * @return true if the specified codon matches any of the wanted codons, false otherwise
	 */
	private boolean ok_recursive(int idx, StringBuffer na_codon, HashSet<String> wanted_codons) {
		if (idx >= 3) {
			return (wanted_codons.contains(na_codon.toString()));
		}
		
		char c = na_codon.charAt(idx);
		if (c == 'R') {
			na_codon.setCharAt(idx, 'A');
			if (ok_recursive(idx+1, na_codon, wanted_codons))
				return true;
			na_codon.setCharAt(idx, 'G');
			return ok_recursive(idx, na_codon, wanted_codons);
		} else if (c == 'Y') {
			na_codon.setCharAt(idx, 'C');
			if (ok_recursive(idx, na_codon, wanted_codons)) 
				return true;
			na_codon.setCharAt(idx, 'U');
			return ok_recursive(idx, na_codon, wanted_codons);
			// no need to worry about T
		} else if (c == 'M') {
			na_codon.setCharAt(idx, 'C');
			if (ok_recursive(idx, na_codon, wanted_codons))
				return true;
			na_codon.setCharAt(idx, 'A');
			return ok_recursive(idx, na_codon, wanted_codons);
		} else if (c == 'K') {
			na_codon.setCharAt(idx, 'U');
			if (ok_recursive(idx, na_codon, wanted_codons)) {
				return true;
			}
			na_codon.setCharAt(idx, 'G');
			return ok_recursive(idx, na_codon, wanted_codons);
			// no need to worry about U
		} else if (c == 'W') {
			na_codon.setCharAt(idx, 'U');
			if (ok_recursive(idx, na_codon, wanted_codons)) {
				return true;
			}
			na_codon.setCharAt(idx, 'A');
			return ok_recursive(idx, na_codon, wanted_codons);
			// no need to worry about T
		} else if (c == 'S') {
			na_codon.setCharAt(idx, 'C');
			if (ok_recursive(idx, na_codon, wanted_codons)) {
				return true;
			}
			na_codon.setCharAt(idx, 'G');
			return ok_recursive(idx, na_codon, wanted_codons);
		} else if (c == 'B') {
			na_codon.setCharAt(idx, 'U');
			if (ok_recursive(idx, na_codon, wanted_codons)) {
				return true;
			}
			na_codon.setCharAt(idx, 'C');
			if (ok_recursive(idx, na_codon, wanted_codons)) {
				return true;
			}
			na_codon.setCharAt(idx, 'G');
			return ok_recursive(idx, na_codon, wanted_codons);
			// no need to worry about T
		} else if (c == 'D') {
			na_codon.setCharAt(idx, 'A');
			if (ok_recursive(idx, na_codon, wanted_codons)) {
				return true;
			}
			na_codon.setCharAt(idx, 'T');
			if (ok_recursive(idx, na_codon, wanted_codons)) {
				return true;
			}
			na_codon.setCharAt(idx, 'G');
			return ok_recursive(idx, na_codon, wanted_codons);
			// no need to worry about U
		} else if (c == 'H') {
			na_codon.setCharAt(idx, 'A');
			if (ok_recursive(idx, na_codon, wanted_codons)) {
				return true;
			}
			na_codon.setCharAt(idx, 'U');
			if (ok_recursive(idx, na_codon, wanted_codons)) {
				return true;
			}
			na_codon.setCharAt(idx, 'C');
			return ok_recursive(idx, na_codon, wanted_codons);
			// no need to worry about T
		} else if (c == 'V') {
			na_codon.setCharAt(idx, 'A');
			if (ok_recursive(idx, na_codon, wanted_codons)) {
				return true;
			}
			na_codon.setCharAt(idx, 'C');
			if (ok_recursive(idx, na_codon, wanted_codons)) {
				return true;
			}
			na_codon.setCharAt(idx, 'G');
			return ok_recursive(idx, na_codon, wanted_codons);
			// no need to worry about U
		} else if (c == 'N') {
			for (char c2 : new char[] { 'A', 'C', 'G', 'U'} ) {
				na_codon.setCharAt(idx, c2);
				if (ok_recursive(idx, na_codon, wanted_codons))
					return true;
			}
			return false;
		} 
		// else	
		return ok_recursive(idx+1, na_codon, wanted_codons);
	}
	
}
