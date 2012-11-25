package au.edu.unimelb.plantcell.misc.biojava;

import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue.AlignmentType;

public class AlignmentSequenceExtractorTask extends BioJavaProcessorTask {
	private int m_alignment_col = -1;
	
	public AlignmentSequenceExtractorTask() {
			super();
	}
	
	public DataColumnSpec[] getColumnSpecs() {
		DataColumnSpec[] cols = new DataColumnSpec[1];
		cols[0] = new DataColumnSpecCreator("Aligned Sequences (list)", ListCell.getCollectionType(SequenceCell.TYPE)).createSpec();
		return cols;
	}
	
	@Override
	public String getCategory() {
		return "Alignment";
	}
	
	@Override
	public void init(BioJavaProcessorNodeModel owner, String task_name, int idx) {
		m_alignment_col = idx;
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
	public DataCell[] getCells(DataRow row) {
		DataCell cell = row.getCell(m_alignment_col);
		if (cell == null || cell.isMissing() || 
				!cell.getType().isCompatible(AlignmentValue.class)) {
			return new DataCell[] { DataType.getMissingCell() };
		}
		
		AlignmentValue av = (AlignmentValue) cell;
		ArrayList<SequenceCell> out = new ArrayList<SequenceCell>();
		for (int i=0; i<av.getSequenceCount(); i++) {
			boolean is_na = av.getAlignmentType().equals(AlignmentType.AL_NA);
			try {
				SequenceCell sc = new SequenceCell(
						is_na ? SequenceType.Nucleotide : SequenceType.AA,
					av.getIdentifier(i).getName(), av.getAlignedSequenceString(i));
				out.add(sc);
			} catch (InvalidSettingsException ise) {
				ise.printStackTrace();
			}
		}
		
		return new DataCell[] { CollectionCellFactory.createListCell(out) };
	}

}
