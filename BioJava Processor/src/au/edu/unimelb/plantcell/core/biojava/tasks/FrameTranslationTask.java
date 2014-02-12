package au.edu.unimelb.plantcell.core.biojava.tasks;

import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.symbol.SymbolList;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.StringCell;

import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;


/**
 * Convert from DNA->RNA->Protein as requested by the user in several frames using BioJava
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class FrameTranslationTask extends BioJavaProcessorTask {
	private boolean m_incl_na_seqs;		// include NA frames for use by later processing steps
	
	public FrameTranslationTask() {
	}
	
	@Override
	public String getCategory() {
		return "Conversion";
	}
	
	public static BioJavaProcessorTask getInstance() {
		return new FrameTranslationTask();
	}
	
	@Override
	public void init(String task, int col) throws Exception {
		super.init(task, col);
		
		m_incl_na_seqs = false;
		if (task.toLowerCase().endsWith("(incl. dna frames)")) {
			m_incl_na_seqs = true;
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public String[] getNames() {
		 return new String[] {   
				 "Six-Frame nucleotide translation (excl. DNA frames)",
			     "Six-Frame nucleotide translation (incl. DNA frames)"
		 };
	}
	
	/** {@inheritDoc} */
	@Override
	public String getHTMLDescription(String task) {
		return "<html>Perform a six frame translation, reporting the results with"+
		" nucleotide frames chosen (incl. DNA frames) or without (excl. DNA frames)" +
		" as AA sequence";
	}

	@Override
	public DataColumnSpec[] getColumnSpecs() {
		int add_na = 0;
		if (m_incl_na_seqs)
			add_na = 6;
		DataColumnSpec[] allColSpecs = new DataColumnSpec[6+add_na];
		
        allColSpecs[0] = 
            new DataColumnSpecCreator("Translation Frame -3", StringCell.TYPE).createSpec();
        allColSpecs[1] = 
            new DataColumnSpecCreator("Translation Frame -2", StringCell.TYPE).createSpec();
        allColSpecs[2] =
        	new DataColumnSpecCreator("Translation Frame -1", StringCell.TYPE).createSpec();
        allColSpecs[3] = 
            new DataColumnSpecCreator("Translation Frame +1", StringCell.TYPE).createSpec();
        allColSpecs[4] =
        	new DataColumnSpecCreator("Translation Frame +2", StringCell.TYPE).createSpec();
        allColSpecs[5] = 
            new DataColumnSpecCreator("Translation Frame +3", StringCell.TYPE).createSpec();
        if (m_incl_na_seqs) {
        	 allColSpecs[6] = 
                 new DataColumnSpecCreator("Nucleic Acid Frame -3", StringCell.TYPE).createSpec();
             allColSpecs[7] = 
                 new DataColumnSpecCreator("Nucleic Acid Frame -2", StringCell.TYPE).createSpec();
             allColSpecs[8] =
             	new DataColumnSpecCreator("Nucleic Acid Frame -1", StringCell.TYPE).createSpec();
             allColSpecs[9] = 
                 new DataColumnSpecCreator("Nucleic Acid Frame +1", StringCell.TYPE).createSpec();
             allColSpecs[10] =
             	new DataColumnSpecCreator("Nucleic Acid Frame +2", StringCell.TYPE).createSpec();
             allColSpecs[11] = 
                 new DataColumnSpecCreator("Nucleic Acid Frame +3", StringCell.TYPE).createSpec();
        }
        
        return allColSpecs;
	}

	@Override
	public DataCell[] getCells(DataRow row) {
		int ncols = 6;
		if (m_incl_na_seqs) {
			ncols += 6;
		}
		
		try {
			SequenceValue sv = getSequenceForRow(row);
			if (sv == null) 
				return missing_cells(ncols);
			
			boolean is_dna   = sv.getSequenceType().isDNA();
			SymbolList syms  = asBioJava(sv, SequenceType.DNA);		// if ambiguous sequence input then assume DNA...
			DataCell[] cells = new DataCell[ncols];
		
			for (int i=0; i<3; i++) {
				// take the reading frame
				SymbolList rf = syms.subList(i+1, syms.length()-(syms.length() - i) % 3);
				
				// if it is DNA transcribe it to RNA first
				if (is_dna) {
					rf = DNATools.toRNA(rf);
				}
				
				SymbolList prot = RNATools.translate(rf);
				cells[i+3] = new StringCell(prot.seqString());
				if (m_incl_na_seqs) {
					cells[i+9] = new StringCell(rf.seqString().toUpperCase());
				}
				
				// reverse frame translation
				rf       = RNATools.reverseComplement(rf);
				prot     = RNATools.translate(rf);
				cells[i] = new StringCell(prot.seqString());
				if (m_incl_na_seqs) {
					cells[i+6] = new StringCell(rf.seqString().toUpperCase());
				}
			}
			return cells;
		} catch (Exception e) {
			e.printStackTrace();
			return missing_cells(ncols);
		}
	}

}
