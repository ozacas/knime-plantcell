package au.edu.unimelb.plantcell.io.pruner;

import java.util.Map;

import org.forester.phylogeny.Phylogeny;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * 
 * @author andrew.cassin
 *
 */
public interface PruningStrategy {
	
	/**
	 * Evaluate the taxa against the input tree and compute internal state as required to return 
	 * results for the <code>accept*</code> methods
	 * @param input_tree
	 * @param taxa
	 * @throws Exception is thrown if the input tree or taxa are considered invalid according to the strategy
	 */
	public void execute(final Phylogeny input_tree, final Map<String,SequenceValue> taxa) throws Exception;

	/**
	 * Called after <code>execute()</code>, should the specified sequence be pruned?
	 */
	public boolean acceptTaxa(final SequenceValue taxa);
}
