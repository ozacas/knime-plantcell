package au.edu.unimelb.plantcell.io.ws.multialign;

import java.util.List;

/**
 * Represents an abtract portion of the node model. There must be nothing in this interface which is specific
 * to a particular node: the idea is that it enables the {@link AlignmentNodeView} implementation to be generic
 * for the node regardless of its implementation. In this way we can link JalView to the alignment data without
 * worrying about the details of each aligner's output.
 * 
 * @author acassin
 *
 */
public interface AlignmentViewDataModel {
	/**
	 * 
	 * @return a list of alignment row IDs in an order as determined by the node
	 */
	public List<String> getAlignmentRowIDs();
	
	/**
	 * Return the {@link AlignmentValue} for the chosen row ID or null if the row does not have an alignment (eg. missing value).
	 * If the row contains multiple alignment columns, the node may determine which column to return. Although it is recommended
	 * that the rightmost (most recent) column be returned for user sanity. A future revision of this interface may be more flexible.
	 */
	public AlignmentValue getAlignment(final String row_id);
}
