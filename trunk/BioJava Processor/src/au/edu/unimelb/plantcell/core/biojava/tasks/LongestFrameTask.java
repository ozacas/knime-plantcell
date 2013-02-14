package au.edu.unimelb.plantcell.core.biojava.tasks;

import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.symbol.IllegalAlphabetException;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.bio.symbol.SymbolList;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/** 
 * Gross hack to identify the "longest ORF" - but really this code is only a partial
 * solution. Available for now, until someone writes a better version...
 * TODO: support for non-standard genetic codes
 * 
 * @author andrew.cassin
 *
 */
public class LongestFrameTask extends BioJavaProcessorTask {
	private boolean m_start_codon, m_stop_codon;
	private boolean m_convert_to_protein;
	private boolean m_forward, m_reverse;
	
	public LongestFrameTask() {
	}
	
	@Override
	public String getCategory() {
		return "Reading Frames";
	}
	
	public static BioJavaProcessorTask getInstance() {
		return new LongestFrameTask();
	}
	
	@Override
	public void init(String task, int col) throws Exception {
		super.init(task, col);
		m_convert_to_protein = (task != null && task.trim().toLowerCase().endsWith("aa)")) ? true : false;
		m_forward = (task != null && (task.indexOf("(all") > 0 || task.indexOf("(3 forward") > 0)) ? true : false;
		m_reverse = (task != null && (task.indexOf("(all") > 0 || task.indexOf("(3 reverse") > 0)) ? true : false;
	}
	
	/** {@inheritDoc} */
	@Override
	public String[] getNames() {
		 return new String[] {   
				 "Longest reading frame (all 6 frames, DNA)", 
			        "Longest reading frame (3 forward frames, DNA)",
			        "Longest reading frame (3 reverse frames, DNA)",
			        "Longest reading frame (all 6 frames, AA)",
			        "Longest reading frame (3 forward frames, AA)",
			        "Longest reading frame (3 reverse frames, AA)"
		 };
	}
	
	/** {@inheritDoc} */
	@Override
	public String getHTMLDescription(String task) {
		return "<html><b>WARNING: experimental! use at own risk!</b><br/>Identifies the longest reading frame (complete) from the input DNA sequence. "+
		"You should scrutinize the results and advanced settings to get it to perform what is desired.";
	}
	
	@Override
	public DataCell[] getCells(DataRow row) {
		SymbolList rev_syms = null;
		String rev_seq = null;
		
		SequenceValue sv = getSequenceForRow(row);
		if (sv == null || !sv.getSequenceType().isDNA()) 
			return missing_cells(getColumnSpecs().length);
		
		// compute distance from methionine AA to stop codon
		String seq= sv.getStringValue();
		try {
			if (m_reverse) {
				rev_syms = DNATools.complement(DNATools.createDNA(seq)); // rev_syms is lowercase...
				rev_seq  = new StringBuffer(rev_syms.seqString()).reverse().toString().toUpperCase();
			}
			String seq_best = "";
			int    seq_dist = -1;
			int    seq_frame= 0;
			int found_start = 0;
			int found_stop  = 0;
			
			for (int offset=0; offset < 3; offset++) {
				// try the forward frames first 
				if (m_forward) {
					String     rf = seq.substring(offset, seq.length() - (seq.length() - offset) % 3);
					int best_dist = find_best_dist(rf);
					if (best_dist > seq_dist) {
						seq_dist     = best_dist;
						seq_best     = rf;
						seq_frame    = offset+1;
						found_start += m_start_codon ? 1 : 0;
						found_stop  += m_stop_codon  ? 1 : 0;
					}
				}
				
				// check reverse sequences?
				if (m_reverse) {
					String rf = rev_seq.substring(offset, seq.length() - (seq.length() - offset) % 3);
					//l.info(rf.substring(0,100));
					int best_dist = find_best_dist(rf);
					if (best_dist > seq_dist) {
						seq_dist     = best_dist;
						seq_best     = rf;
						seq_frame    = -(offset + 1);
						found_start += m_start_codon ? 1 : 0;
						found_stop  += m_stop_codon  ? 1 : 0;
					}
				}
			}
			
			DataCell[] cells = new DataCell[4];
			if (seq_dist >= 0) {
				cells[0] = dna2cell(seq_best);
				cells[1] = new IntCell(seq_frame);
				cells[2] = new IntCell(found_start);
				cells[3] = new IntCell(found_stop);
			} else {
				cells[0] = DataType.getMissingCell();
				cells[1] = new IntCell(1);
				cells[2] = new IntCell(0);
				cells[3] = new IntCell(0);
			}
			return cells;
		} catch (Exception e) {
			e.printStackTrace();
			return missing_cells(getColumnSpecs().length);
		}
	}

	protected StringCell dna2cell(String rf) throws Exception {
		StringCell ret;
		if (m_convert_to_protein) {
			SymbolList syms = DNATools.createDNA(rf);
			syms = DNATools.toRNA(syms);
			// ensure multiple of 3 (trim excess)
			if (syms.length() % 3 != 0) {
				syms = syms.subList(1, syms.length() - (syms.length() % 3));
			}
			SymbolList prot = RNATools.translate(syms);
			ret = new StringCell(prot.seqString());
		} else {
			ret = new StringCell(rf);
		}
		return ret;
	}
	
	protected int find_best_dist(String rf) throws IllegalAlphabetException, IllegalSymbolException {
		int best_dist   = -1;
		
		// convert to protein (stop codons are marked as '*' by biojava)
		SymbolList syms = DNATools.createDNA(rf);
		syms = DNATools.toRNA(syms);
		String aa = RNATools.translate(syms).seqString();
	
		for (String subseq : aa.split("\\*")) {
			if (subseq.length() > best_dist) {
				// TODO... require a start codon in subseq?
				
				best_dist = subseq.length();
			}
		}
		return best_dist;
	}
	
	@Override
	public DataColumnSpec[] getColumnSpecs() {
		DataColumnSpec[] allColSpecs = new DataColumnSpec[4];
		
        allColSpecs[0] = 
            new DataColumnSpecCreator("Longest Reading Frame Sequence", StringCell.TYPE).createSpec();
        allColSpecs[1] = 
        	new DataColumnSpecCreator("Chosen Frame", IntCell.TYPE).createSpec();
        allColSpecs[2] = 
        	new DataColumnSpecCreator("Start codons (total across reading frames)", IntCell.TYPE).createSpec();
        allColSpecs[3] =
        	new DataColumnSpecCreator("Stop codons (total across reading frames)", IntCell.TYPE).createSpec();
        return allColSpecs;
    }

}
