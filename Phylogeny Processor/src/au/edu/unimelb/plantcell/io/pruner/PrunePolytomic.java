package au.edu.unimelb.plantcell.io.pruner;

import java.util.HashSet;
import java.util.List;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;
import org.knime.core.node.NodeLogger;

/**
 * Removes taxa which are descended from a non-binary node or alternately prunes all but the first 2 kids to make the node binary
 * depending on construction parameters
 * 
 * @author andrew.cassin
 *
 */
public class PrunePolytomic implements PruningStrategy {
	private HashSet<PhylogenyNode> rejected_taxa = new HashSet<PhylogenyNode>();
	private boolean m_reduce = false;
	
	public PrunePolytomic() {
		this(false);		// remove all descendants
	}
	
	public PrunePolytomic(boolean reduce_to_binary) {
		m_reduce = reduce_to_binary;
	}

	@Override
	public void execute(final TreePruneNodeModel mdl, final Phylogeny input_tree) throws Exception {
		rejected_taxa.clear();
		
		for (PhylogenyNodeIterator it = input_tree.iteratorPreorder(); it.hasNext(); ) {
			PhylogenyNode n = it.next();
			
			if (!n.isRoot() && n.getNumberOfDescendants() > 2) {
				List<PhylogenyNode> externals = n.getAllExternalDescendants();
				if (!m_reduce) {
					n.getParent().removeChildNode(n);
					//Logger.getAnonymousLogger().info("Pruning node ID: "+n.getId()+" descendants="+n.getNumberOfDescendants());
					for (PhylogenyNode kid : externals) {
						rejected_taxa.add(kid);
						// NB: we dont delete the kids here since the parent has already gone...
					}
				} else {
					// remove all but 2 kids but leave n as part of the tree
					int idx = 0;
					for (PhylogenyNode kid : externals) {
						if (idx++ > 1) {
							rejected_taxa.add(kid);
							n.removeChildNode(kid);
						}
					}
				}
			}
		}
	}

	@Override
	public boolean accept(final TreePruneNodeModel mdl, PhylogenyNode n) {
		if (rejected_taxa.contains(n))
			return false;
		return true;
	}

	@Override
	public void summary(final NodeLogger l, final Phylogeny p) {
		l.info("Removed "+rejected_taxa.size()+" tip nodes from tree.");
		l.info("Tree now has "+p.getNodeCount()+ " nodes remaining.");
	}
}
