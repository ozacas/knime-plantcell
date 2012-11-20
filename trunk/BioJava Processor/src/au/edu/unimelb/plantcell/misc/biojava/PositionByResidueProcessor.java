package au.edu.unimelb.plantcell.misc.biojava;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Adds columns for each position (up to the specified maximum length in the constructor) 
 * with a cell for each residue in each column. Each cells contains the number of residues at that
 * position. Positions are numbered from 1 to correspond to what biologists expect ;-)
 * 
 * @author andrew.cassin
 *
 */
public class PositionByResidueProcessor extends BioJavaProcessorTask {
	private int m_maxlen;
	
	public PositionByResidueProcessor() {
	}
	
	@Override
	public String getCategory() {
		return "Statistics";
	}
	
	public static BioJavaProcessorTask getInstance() {
		return new PositionByResidueProcessor();
	}
	
	public void init(BioJavaProcessorNodeModel m, String task) {
		setOwner(m);
		m_maxlen = 75;
	}
	
	/** {@inheritDoc} */
	@Override
	public String[] getNames() {
		return new String[] { "Residue Frequency by Position" };
	}

	/** {@inheritDoc} */
	@Override
	public String getHTMLDescription(String task) {
		return "<html>Outputs a table of data: residue (either DNA+IUPAC codes or AA) by position up to " +
				"the specified maximum length of sequence data. If not all sequences are the "+
				"same length, you'll need to be careful to interpret the raw counts. " +
				"By default, only the first <b>75</b> residues are counted, but you can change that with the advanced settings tab in the configure dialog.";
	}
	
	@Override
	public void execute(ColumnIterator ci, ExecutionContext exec,
			NodeLogger l, BufferedDataTable[] inData, BufferedDataContainer c1)
			throws Exception {
		TaskParameter maxlen_tp = getParameter("Maximum Sequence Length", "75");
		Integer i_maxlen = new Integer(maxlen_tp.getValue());
		if (i_maxlen.intValue() < 1) {
			throw new InvalidSettingsException("Invalid maximum sequence length (must be at least one!)");
		}
		m_maxlen = i_maxlen.intValue();
		ResidueByPosition[] pos = new ResidueByPosition[m_maxlen];
		for (int i=0; i<m_maxlen; i++) {
			pos[i] = new ResidueByPosition(i+1);
		}
		
		// scan the sequences -- speed is key here
		int done = 0;
		int n_rows = inData[0].getRowCount();
		while (ci.hasNext()) {
			DataCell c = ci.next();
			if (c == null || c.isMissing() || !(c instanceof SequenceValue)) 
				continue;
			SequenceValue sv = (SequenceValue) c;
			
			int len = m_maxlen;
			if (sv.getLength() < m_maxlen)
				len = sv.getLength();
			String seq = sv.getStringValue().toUpperCase();
			for (int i=0; i<len; i++ ) {
				pos[i].bump(seq.charAt(i));
			}
			if (done % 1000 == 0) {
				exec.setProgress((double) done / n_rows);
				exec.checkCanceled();
			}
			done++;
		}
		
		// build the output table...
		char[] letters = pos[0].getResidueLetters();
		for (int i=0; i<letters.length; i++) {
			DataCell[] row = new DataCell[m_maxlen];
			for (int j=0; j<m_maxlen; j++) {
				row[j] = new IntCell(pos[j].count(letters[i]));
			}
			c1.addRowToTable(new DefaultRow(new RowKey(new Character(letters[i]).toString()), row));
		}
		
		// all done
		c1.close(); 
	}

	@Override
	public DataTableSpec get_table_spec() {
		assert(m_maxlen > 0);
		DataColumnSpec[] cols = new DataColumnSpec[m_maxlen];
		for (int i=0; i<m_maxlen; i++) {
			cols[i] = new DataColumnSpecCreator("Position "+new Integer(i+1).toString(), IntCell.TYPE).createSpec();
		}
		return new DataTableSpec(cols);
	}

	public class ResidueByPosition {
		private final char[] letters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',  
				'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };
		private int[]  count;
		
		public ResidueByPosition(int i) {
			count = new int[] {0, 0, 0, 0, 0, 0,
					 0, 0, 0, 0, 0, 0,
					 0, 0, 0, 0, 0, 0,
					 0, 0, 0, 0, 0, 0, 0, 0};	
		}
		
		public char[] getResidueLetters() {
			return letters;
		}
	
		public void bump(char c) {
			assert(c >= 'A' && c <= 'Z');
			int offset = c - 'A';
			count[offset]++;
		}

		public int count(char c) {
			assert(c >= 'A' && c <= 'Z');
			int offset = c - 'A';
			return count[offset];
		}
	}

	@Override
	public boolean isMerged() {
		return false;
	}

}
