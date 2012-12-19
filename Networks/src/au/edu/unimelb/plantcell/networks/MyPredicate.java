package au.edu.unimelb.plantcell.networks;

import org.apache.commons.collections15.Predicate;

public interface MyPredicate<T> extends Predicate<T> {
	
	/**
	 * is the predicate an edge or vertex filter? (true = vertex, false = edge)
	 */
	public boolean isVertexPredicate();
	
	/**
	 * Returns a representation of the predicate as a string suitable for displaying to the user
	 */
	@Override
	public String toString();
}
