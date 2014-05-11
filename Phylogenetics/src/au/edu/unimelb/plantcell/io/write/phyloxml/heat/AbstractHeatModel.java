package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;

/**
 * 
 * @author acassin
 *
 */
public abstract class AbstractHeatModel {

	/**
	 * Called before start(), this method processes the available input data and creates the internal distance matrix
	 * or whatever for the model to use.
	 */
	public abstract void makePairwise(final BufferedDataTable in, int a_idx, int b_idx, int heat_idx) throws InvalidSettingsException; 
	
	/**
	 * Called before the start of each Phylogeny to indicate to the model that analysis of a new tree is starting.
	 * Typically models will respond by initialising their state.
	 */
	public abstract void start(final Phylogeny p);
	
	/**
	 * Compute the heat for the specified node in the tree. The caller is free to do nothing, or to alter
	 * parent nodes (eg. propagate heat to the root of the tree) as it sees fit. 
	 * 
	 * @param n never null
	 */
	public abstract void apply(final PhylogenyNode n);
	
	/**
	 * Called after traversal of the tree (ie. <code>apply()</code>) is complete, this method may
	 * finalise the heat values as computed during apply. Or nothing.
	 * 
	 * @param p the phylogeny which has just been traversed (maybe one of many trees in the given file)
	 */
	public abstract void finish(final Phylogeny p);
}
