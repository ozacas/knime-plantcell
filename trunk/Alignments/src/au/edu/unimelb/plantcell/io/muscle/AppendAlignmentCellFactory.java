package au.edu.unimelb.plantcell.io.muscle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.container.SingleCellFactory;

import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Responsible for appending the computed alignment to the last column of the output table specification
 * as specified in the constructor.
 * 
 * @author acassin
 *
 */
public class AppendAlignmentCellFactory extends SingleCellFactory {
	private int seqs_idx = -1;
	private AbstractAlignerNodeModel mdl;
	
	public AppendAlignmentCellFactory(final DataTableSpec outSpec, int seq_column, final AbstractAlignerNodeModel mdl) {
    	// last column in the table is the newly added alignment column, so we give that column spec to the factory constructor...
		super(outSpec.getColumnSpec(outSpec.getNumColumns()-1));
		assert(seq_column >= 0);
		seqs_idx = seq_column;
		assert(mdl != null);
		this.mdl = mdl;
	}
	
	@Override
	public DataCell getCell(DataRow r) {
		DataCell seqs_cell = r.getCell(seqs_idx);
		if (!mdl.isValidCollectionForAlignment(seqs_cell)) 
			return DataType.getMissingCell();
		
		Iterator<DataCell> it = null;
		if (seqs_cell instanceof ListCell) {
			it = ((ListCell)seqs_cell).iterator();
		} else { // must be set cell
			it = ((SetCell)seqs_cell).iterator();
		}
		
		// validate input sequences and create set of sequences to align...
		SequenceType st = SequenceType.UNKNOWN;
		final Map<UniqueID,SequenceValue> seq_map = new HashMap<UniqueID,SequenceValue>();
		while (it.hasNext()) {
			DataCell c = it.next();
			if (c instanceof SequenceValue) {
				SequenceValue sv = (SequenceValue)c;
				if (st != SequenceType.UNKNOWN && st != sv.getSequenceType()) {
					mdl.error("Cannot mix sequence types (eg. AA versus NA) in alignment for row: "+r.getKey().getString());
					return DataType.getMissingCell();
				} else {
					st = sv.getSequenceType();
				}
				seq_map.put(new UniqueID(), sv);
			}
		}
		
		String rowid = r.getKey().getString();
		
		try {
			mdl.validateSequencesToBeAligned(seq_map);
			return mdl.runAlignmentProgram(seq_map, rowid, st);
		} catch (Exception e) {
			e.printStackTrace();
			return DataType.getMissingCell();
		}
	}

}
