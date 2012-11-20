package au.edu.unimelb.plantcell.misc.biojava;

import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;

public class SequenceCleanerProcessor extends BioJavaProcessorTask {
	private final Set<Character> m_dna = new HashSet<Character>();
	private final Set<Character> m_rna = new HashSet<Character>();
	private final Set<Character> m_aa  = new HashSet<Character>();

	@Override
	public String getCategory() {
		return "Formatting";
	}
	
	@Override
	public void init(BioJavaProcessorNodeModel owner, String task_name) {
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
	public DataTableSpec get_table_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[2];
		cols[0] = new DataColumnSpecCreator("Cleaned Sequence", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Residues rejected", IntCell.TYPE).createSpec();
		return new DataTableSpec(cols);
	}

	@Override
	public boolean isMerged() {
		return true;
	}

	public static BioJavaProcessorTask getInstance() { 
		return new SequenceCleanerProcessor();
	}
	
	public String[] getNames() { 
		return new String[] {"Clean sequences (whitespace removal, non-coding symbols, stop codon->X conversion, all uppercase)"}; 
	}
	
	public String getHTMLDescription(String task) {
		return "<html>Performs a series of simple tasks to the chosen column of sequences:" +
				"<ul>" +
				"<li>Removal of all whitespace (space, tab, other)</li>"+
				"<li>Conversion of all residue symbols to uppercase</li>"+
				"<li>Conversion of all stop codons to X</li>"+
				"<li>Removal of non-coding symbols for the chosen sequence type. A count " +
				" of any residues removed from sequences appears in a separate column."  +
				"</li>"+
				"</ul>";
	}
	
	@Override
	public void execute(ColumnIterator ci, ExecutionContext exec,
			NodeLogger l, BufferedDataTable[] inData, BufferedDataContainer c1)
			throws Exception {
		l.info("Cleaning "+inData[0].getRowCount()+" sequences.");
		
		int n_rows = inData[0].getRowCount();
		int done   = 0;
		while (ci.hasNext()) {
			DataCell c = ci.next();
			if (c == null || c.isMissing() || !(c instanceof SequenceValue))
				continue;
			SequenceValue sv = (SequenceValue) c;
			String seq = sv.getStringValue();
			seq = seq.replaceAll("\\s+", "");
			seq = seq.replaceAll("\\*", "X");
			seq = seq.toUpperCase();
			
			SequenceType st = sv.getSequenceType();
			Set<Character> letters = m_dna;
			if (st.isProtein()) {
				letters = m_dna;
			} else if (st.isRNA()) {
				letters = m_rna;
			}
			StringBuffer sb = new StringBuffer(seq.length());
			int rejected = 0;
			for (int i=0; i<seq.length(); i++) {
				Character c2 = new Character(seq.charAt(i));
				if (Character.isLetter(c2)) {
					if (!letters.contains(c2)) {     // AA
						rejected++;
					} else {
						sb.append(c2);
					}
				} else if (c2 == '-' || c2 == '.') {
					sb.append(c2);
				} else {
					rejected++;
				}
			}
			
			add_row_to_table(c1, sb.toString(), rejected, ci.lastRow());
			done++;
			if (done % 1000 == 0) {
				exec.checkCanceled();
				exec.setProgress(((double)done)/n_rows);
			}
		}
	}

	protected void add_row_to_table(BufferedDataContainer c, String string, 
									int rejected, DataRow r) {
		DataCell[] cells = new DataCell[2];
		cells[0] = new StringCell(string);
		cells[1] = new IntCell(rejected);
		c.addRowToTable(new JoinedRow(r, new DefaultRow(r.getKey().getString(), cells)));
	}

}
