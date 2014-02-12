package au.edu.unimelb.plantcell.core.biojava.tasks;

import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.symbol.SymbolList;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;

import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Computes a reverse complement using the most specific BioJava conversion routine for the input sequence as per
 * http://biojava.org/wiki/BioJava:Cookbook:Sequence:Reverse
 * 
 * @author andrew.cassin
 *
 */
public class ReverseComplementTask extends BioJavaProcessorTask {
	@Override
	public String getCategory() {
		return "Conversion";
	}
	
	public static BioJavaProcessorTask getInstance() {
		return new ReverseComplementTask();
	}
	
	@Override
	public void init(String task, int col) throws Exception {
		super.init(task, col);
	}
	
	/** {@inheritDoc} */
	@Override
	public String[] getNames() {
		 return new String[] {   
				 "Reverse Complement (nucleotide/DNA/RNA sequences only)"
		 };
	}
	
	/** {@inheritDoc} */
	@Override
	public String getHTMLDescription(String task) {
		return "<html>Using BioJava, this routine computes the reverse complement of the input nucleotide (ie. RNA or DNA) sequence "+
				"using a procedure similar to http://biojava.org/wiki/BioJava:Cookbook:Sequence:Reverse. This routine will assume " +
				"DNA sequence if no sequence type information is available (eg. from the Fasta Reader)";
	}

	@Override
	public DataColumnSpec[] getColumnSpecs() {
		DataColumnSpec[] cols = new DataColumnSpec[1];
		cols[0] = new DataColumnSpecCreator("Reverse complement of input sequence", SequenceCell.TYPE).createSpec();
		return cols;
	}
	
	@Override
	public DataCell[] getCells(DataRow r) {
		SequenceValue sv = getSequenceForRow(r);
		if (sv == null || sv.getSequenceType().isProtein())
			return new DataCell[] { DataType.getMissingCell() };
		try {
			SymbolList syms = asBioJava(sv, SequenceType.DNA);		// automagically assume DNA if user doesnt specify it
			SymbolList rev  = null;
			if (sv.getSequenceType().isRNA()) {
				rev = RNATools.reverseComplement(syms);
			} else {
				rev = DNATools.reverseComplement(syms);
			}
			
			SequenceCell sc = new SequenceCell(sv.getSequenceType(), "REV_"+sv.getID(), rev.seqString().toUpperCase());
			return new DataCell[] { sc };
		} catch (Exception e) {
			e.printStackTrace();
			return new DataCell[] { DataType.getMissingCell() };
		}
	}

}
