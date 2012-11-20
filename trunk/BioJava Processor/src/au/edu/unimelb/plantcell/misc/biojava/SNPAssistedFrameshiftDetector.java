package au.edu.unimelb.plantcell.misc.biojava;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

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
	
	public void init(BioJavaProcessorNodeModel m, String task) {
		setOwner(m);
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
	public void execute(ColumnIterator ci, ExecutionContext exec,
			NodeLogger l, BufferedDataTable[] inData, BufferedDataContainer cont)
			throws Exception {
		int done = 0;
		int n_rows = inData[0].getRowCount();
		while (ci.hasNext()) {
			DataCell   c = ci.next();
			if (c == null || c.isMissing() || !(c instanceof SequenceValue))
				continue;
			SequenceValue sv = (SequenceValue) c;
			if (!sv.getSequenceType().isDNA() || sv.getLength() < 1) {
				l.warn("Only DNA sequences can be processed with the frameshift detector!");
				continue;
			}
			DataCell[] cells = new DataCell[1];
				
			int[] codon_pos = new int[sv.getLength()];
			int codon_idx = 0;
			String seq = sv.getStringValue().toUpperCase();
			for (int i=0; i<sv.getLength(); i++) {
				char ch = seq.charAt(i);
				if (ch != 'A' && ch != 'T' && ch !='G' && ch != 'C' ) {
					if (!Character.isLetter(ch))
						throw new Exception("Bad char: "+ (int) ch+" (encountered in sequence" + sv.getID() +")");
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
			//cells[1] = new StringCell(mode_str.toString());
			
			cont.addRowToTable(new JoinedRow(ci.lastRow(), new DefaultRow(ci.lastRowID(), cells)));
			done++;
			if (done % 100 == 0) {
				exec.checkCanceled();
				exec.setProgress(((double)done)/n_rows, "Processed sequence "+sv.getID());
			}
		}
	}

	@Override
	public DataTableSpec get_table_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[1];
		cols[0] = new DataColumnSpecCreator("Number of detected frameshifts", IntCell.TYPE).createSpec();
		//cols[1] = new DataColumnSpecCreator("Debug", StringCell.TYPE).createSpec();
		return new DataTableSpec(cols);
	}

	@Override
	public boolean isMerged() {
		return true;
	}

}
