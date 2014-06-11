package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import java.awt.Color;

import org.forester.phylogeny.PhylogenyNode;

/**
 * Responsible for maintaining the heat state of each node and application of the final computed
 * colour for each node
 * 
 * @author acassin
 *
 */
public interface ColourManager {
	/**
	 * Called once for each node before any apply() calls are made, this is responsible for
	 * updating the internal state to reflect the appropriate colour to display for the given node. It is free to ignore
	 * data as it sees fit
	 * 
	 * @param n guaranteed non-null
	 * @param row_colour a Color instance to use for n
	 * @param heat_value a numeric value for the heat (used for propagation to the root)
	 * @param if heat was accepted (true) or rejected (false)
	 */
	public boolean addHeat(final PhylogenyNode n, final Color row_colour, final Double heat_value);
	
	/**
	 * 
	 * @param n
	 * @throws Exception 
	 */
	public void decorate(final PhylogenyNode n) throws Exception;
	
	/**
	 * Usually not all nodes have heat specified. This method is called immediately after all known heat is applied to nodes (using addHeat())
	 * and can be used to propagate heat to other nodes as required. For example, internal nodes may have a colour reflecting the average
	 * heat of child nodes below it (or at least those with known heat).
	 * 
	 * @param ms used to tailor which nodes are included in a heat re-calculation for a given node in the tree
	 * @param hm used to customise how the heat is recalculated eg. average/maximum/min/...
	 */
	public void propagate(final ModerationSelector ms, final HeatModerator hm);
	
}
