package au.edu.unimelb.plantcell.misc.biojava;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue;

public class AlignmentSequenceExtractorTask extends BioJavaProcessorTask {
	@Override
	public String getCategory() {
		return "Alignment";
	}
	
	@Override
	public void init(BioJavaProcessorNodeModel owner, String task_name) {
	}

	@Override
	public DataTableSpec get_table_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[3];
		cols[0] = new DataColumnSpecCreator("Input RowID", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Accession", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Aligned Sequence", StringCell.TYPE).createSpec();
		return new DataTableSpec(cols);
	}

	@Override
	public boolean isMerged() {
		return false;
	}

	@Override
	public ColumnFilter getColumnFilter() {
		return new ColumnFilter() {

			@Override
			public boolean includeColumn(DataColumnSpec colSpec) {
				return (colSpec.getType().isCompatible(AlignmentValue.class));
			}

			@Override
			public String allFilteredMsg() {
				return "Input table must have an alignment column (eg. Sequence Aligner node)";
			}
			
		};
	}
	
	/** {@inheritDoc} */
	@Override
	public String[] getNames() {
        return new String[] { "Extract aligned sequences" };
	}
	
	public static BioJavaProcessorTask getInstance() {
		return new AlignmentSequenceExtractorTask();
	}
	
	/** {@inheritDoc} */
	@Override
	public String getHTMLDescription(String task_name) {
		return "<html>Separates each sequence in an alignment (in a column) into a separate row";
	}
	
	@Override
	public void execute(ColumnIterator ci, ExecutionContext exec,
			NodeLogger l, BufferedDataTable[] inData, BufferedDataContainer c)
			throws Exception {
		int n_rows = inData[0].getRowCount();
		
		int done = 0;
		int id = 1;
		while (ci.hasNext()) {
			DataCell cell = ci.next();
			if (cell == null || cell.isMissing())
				continue;
			
			if (cell.getType().isCompatible(AlignmentValue.class)) {
				AlignmentValue av = (AlignmentValue) cell;
				for (int i=0; i<av.getSequenceCount(); i++) {
					DataCell[] cells = new DataCell[3];
					cells[0] = new StringCell(ci.lastRowID());
					cells[1] = new StringCell(av.getIdentifier(i).getName());
					cells[2] = new StringCell(av.getAlignedSequenceString(i));
					c.addRowToTable(new DefaultRow("Seq"+id++, cells));
				}
			}
			
			done++;
			if (done % 100 == 0) {
				exec.checkCanceled();
				exec.setProgress(((double)done)/n_rows);
			}
		}
	}

}
