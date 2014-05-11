package au.edu.unimelb.plantcell.io.pruner;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.knime.core.node.NodeLogger;

/**
 * Classes must implement this interface to be used with the {@link TreePruneNodeModel}
 * 
 * @author andrew.cassin
 *
 */
public interface PruningStrategy {
	
	/**
	 * Evaluate the taxa against the input tree and compute internal state as required to return 
	 * results for the <code>accept*</code> methods
	 * @param mdl
	 * @param input_tree
	 * @throws Exception is thrown if the input tree or taxa are considered invalid according to the strategy
	 */
	public void execute(final TreePruneNodeModel mdl, final Phylogeny input_tree) throws Exception;

	/**
	 * Called after <code>execute()</code>, should the specified node be pruned from the tree? the caller has access
	 * to the full model state, after execution of the strategy, for final determination
	 */
	public boolean accept(final TreePruneNodeModel mdl, final PhylogenyNode taxa);
	
	/**
	 * Report a summary of actions taken to the specified logger
	 */
	public void summary(final NodeLogger l, final Phylogeny p);
}
