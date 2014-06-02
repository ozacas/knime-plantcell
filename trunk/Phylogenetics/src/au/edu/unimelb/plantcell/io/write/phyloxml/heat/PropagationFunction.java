package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import org.forester.phylogeny.PhylogenyNode;

/**
 * How to propagate heat from the leaf branches to the root?
 * @author acassin
 *
 */
public interface PropagationFunction {
	/**
	 * Called for every node during traversal to propagate the heat from the leaves to the tree.
	 * Implementations must not rely on the order of node traversal.
	 * 
	 * @param n never null
	 * @param mdl never null
	 */
	public void propagate(final PhylogenyNode n, final AbstractHeatModel mdl);
}
