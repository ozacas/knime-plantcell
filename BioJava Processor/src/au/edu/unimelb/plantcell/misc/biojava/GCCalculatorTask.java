package au.edu.unimelb.plantcell.misc.biojava;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Adds a column to the input table with the %GC added. Method is as described at:
 * http://en.wikipedia.org/wiki/GC-content
 * 
 * @author andrew.cassin
 *
 */
public class GCCalculatorTask extends BioJavaProcessorTask {

	public GCCalculatorTask() {
	}
	
	@Override
	public String getCategory() {
		return "Statistics";
	}
	
	public static BioJavaProcessorTask getInstance() {
		return new GCCalculatorTask();
	}
	
	@Override
	public void init(BioJavaProcessorNodeModel owner, String task_name) {
	}
	
	/** {@inheritDoc} */
	@Override
	public String[] getNames() {
		 return new String[] { "GC Content and AT/GT ratio (%)" }; 
	}
	
	/** {@inheritDoc} */
	@Override
	public String getHTMLDescription(String task) {
		return "<html>Computes GC content and AT/GC ratio as a percentage as\n" +
				"described at: http://en.wikipedia.org/wiki/GC-content<br/>\n"+
				"Note that non-ACGT residues are <b>excluded</b> from the calculation.";
	}

	@Override
	public DataTableSpec get_table_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[3];
		cols[0] = new DataColumnSpecCreator("GC content (%)", DoubleCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("AT/GC ratio (%)", DoubleCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Non-base letters (ie. not ACGT, %)", DoubleCell.TYPE).createSpec();
		return new DataTableSpec(cols);
	}

	@Override
	public boolean isMerged() {
		return true;
	}

	@Override
	public void execute(ColumnIterator ci, ExecutionContext exec,
			NodeLogger l, BufferedDataTable[] inData, BufferedDataContainer c1)
			throws Exception {
				
		int done = 0;
		while (ci.hasNext()) {
			DataCell c = ci.next();
			if (c == null || c.isMissing() || !(c instanceof SequenceValue))
				continue;
			SequenceValue sv = (SequenceValue) c;
			if (!sv.getSequenceType().isDNA() || sv.getLength() < 1) {
				l.warn("Ignoring non-DNA sequence: "+sv.getID());
				continue;
			}
			
			int gc = 0;
			int at = 0;
			int n  = 0;
			int other = 0;
			String seq = sv.getStringValue();
			for (int i=0; i<seq.length(); i++) {
				char ch = seq.charAt(i);
				if (Character.isWhitespace(ch)) 
					continue;
				if (Character.isLetter(ch)) {
					n++;
				}
				if (ch == 'G' || ch == 'C') {
					gc++;
				} else if (ch == 'A' || ch == 'T') {
					at++;
				} else {
					other++;
				}
			}
			DataCell[] cells = new DataCell[3];
			cells[0] = new DoubleCell(100.0 * ((double)gc) / (gc+at));
			cells[1] = new DoubleCell(100.0 * ((double)at) / gc);
			cells[2] = new DoubleCell(100.0 * ((double)other) / n);
			c1.addRowToTable(new JoinedRow(ci.lastRow(), new DefaultRow(ci.lastRowID(), cells)));
			exec.checkCanceled();
			exec.setProgress(((double) done++) / inData[0].getRowCount());
		}
	}

}
