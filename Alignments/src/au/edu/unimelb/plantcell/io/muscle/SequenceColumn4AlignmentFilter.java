package au.edu.unimelb.plantcell.io.muscle;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Shared by all the alignment nodes to accept columns under one of two conditions:
 * 1) the column contains SequenceValue instance's. All the valid sequences are pooled and then aligned as one.
 * 2) the column contains a collection of SequenceValue instances (list or set). In this case the alignments
 *    are done separately per collection
 *    
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class SequenceColumn4AlignmentFilter implements ColumnFilter {

	@Override
	public String allFilteredMsg() {
		return "No suitable set/list/column of sequences available!";
	}

	@Override
	public boolean includeColumn(DataColumnSpec arg0) {
		if (arg0 == null || arg0.getType() == null)
			return false;
		
		// collection cell?
		if (arg0.getType().isCollectionType() && arg0.getType().getCollectionElementType().isCompatible(SequenceValue.class))
			return true;
		// sequence cell?
		if (arg0.getType().isCompatible(SequenceValue.class)) 
			return true;
		
		return false;
	}

}
