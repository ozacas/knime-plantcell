package au.edu.unimelb.plantcell.io.muscle;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

public class SequenceColumn4AlignmentFilter implements ColumnFilter {

	@Override
	public String allFilteredMsg() {
		return "No suitable set/list/column of sequences available!";
	}

	@Override
	public boolean includeColumn(DataColumnSpec arg0) {
		// collection cell?
		if (arg0.getType().isCollectionType() && arg0.getType().getCollectionElementType().isCompatible(SequenceValue.class))
			return true;
		// sequence cell?
		if (arg0.getType().isCompatible(SequenceValue.class)) 
			return true;
		
		return false;
	}

}
