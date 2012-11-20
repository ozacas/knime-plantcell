package au.edu.unimelb.plantcell.misc.biojava;

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

public class SequenceFormattedCleanerProcessor extends SequenceCleanerProcessor {

	@Override
	public String getCategory() {
		return "Formatting";
	}
	
	@Override 
	public DataTableSpec get_table_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[2];
		cols[0] = new DataColumnSpecCreator("Formatted & Cleaned Sequence", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Residues rejected", IntCell.TYPE).createSpec();
		return new DataTableSpec(cols);
	}
	
	public static BioJavaProcessorTask getInstance() { 
		return new SequenceFormattedCleanerProcessor();
	}
	
	@Override
	public String[] getNames() { 
		return new String[] {"Clean & format sequences (whitespace removal, non-coding symbols, stop codon->X conversion, all uppercase)"}; 
	}
	
	@Override 
	public String getHTMLDescription(String task) {
		return "<html>Similar to cleaning sequences, but formats each sequence " +
				"to have no more than 75 residues per line. When used with the <b>FASTA Writer</b> " +
				"node this gives a consistent FASTA formatted file.";
	}
	
	@Override
	public void add_row_to_table(BufferedDataContainer c, String seq, int rejected, DataRow in) {
		DataCell[] cells = new DataCell[2];
		StringBuffer formatted_seq = new StringBuffer(seq.length());
		int line_len = 75;
		int seq_len  = seq.length();
		for (int i=0; i<seq_len; i += line_len) {
			int end = i+line_len;
			if (end >= seq_len) {
				end = seq.length();
			}
			formatted_seq.append(seq.substring(i, end));
			formatted_seq.append('\n');
		}
		cells[0] = new StringCell(formatted_seq.toString());
		cells[1] = new IntCell(rejected);
		c.addRowToTable(new JoinedRow(in, new DefaultRow(in.getKey().getString(), cells)));
	}
}
