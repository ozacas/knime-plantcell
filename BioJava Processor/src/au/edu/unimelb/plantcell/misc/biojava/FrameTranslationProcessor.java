package au.edu.unimelb.plantcell.misc.biojava;

import org.knime.core.data.*;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.symbol.*;

import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;


public class FrameTranslationProcessor extends BioJavaProcessorTask {
	private boolean m_incl_na_seqs;		// include NA frames for use by later processing steps
	
	public FrameTranslationProcessor() {
	}
	
	@Override
	public String getCategory() {
		return "Translation";
	}
	
	public static BioJavaProcessorTask getInstance() {
		return new FrameTranslationProcessor();
	}
	
	public void init(BioJavaProcessorNodeModel owner, String task) {
		setOwner(owner);
		
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
	
	public void execute(ColumnIterator ci, ExecutionContext exec, NodeLogger logger,
			BufferedDataTable[] inData, BufferedDataContainer c1)
			throws Exception {
		
		int n_rows = inData[0].getRowCount();
		int done   = 0;
	
		int ncols = 6;
		if (m_incl_na_seqs) {
			ncols += 6;
		}
		
		while (ci.hasNext()) {
			DataCell c = ci.next();
			if (c == null || c.isMissing() || !(c instanceof SequenceValue)) 
				continue;
			SequenceValue sv = (SequenceValue) c;
			boolean is_dna = sv.getSequenceType().equals(SequenceType.DNA);
			SymbolList syms = asBioJava(sv);
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
			
			// add all the cells into the row
			DataRow      row = new DefaultRow(ci.lastRowID(), cells);
			c1.addRowToTable(new JoinedRow(ci.lastRow(), row));
			
			done++;
			if (done % 100 == 0) {
				exec.checkCanceled();
				exec.setProgress(((double)done) / n_rows, "Processed "+done+" sequences.");
			}
		}
	}

	public DataTableSpec get_table_spec() {
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
                 new DataColumnSpecCreator("NA Frame -3", StringCell.TYPE).createSpec();
             allColSpecs[7] = 
                 new DataColumnSpecCreator("NA Frame -2", StringCell.TYPE).createSpec();
             allColSpecs[8] =
             	new DataColumnSpecCreator("NA Frame -1", StringCell.TYPE).createSpec();
             allColSpecs[9] = 
                 new DataColumnSpecCreator("NA Frame +1", StringCell.TYPE).createSpec();
             allColSpecs[10] =
             	new DataColumnSpecCreator("NA Frame +2", StringCell.TYPE).createSpec();
             allColSpecs[11] = 
                 new DataColumnSpecCreator("NA Frame +3", StringCell.TYPE).createSpec();
        }
        
        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
		return outputSpec;
	}

	@Override
	public boolean isMerged() {
		return true;
	}

}
