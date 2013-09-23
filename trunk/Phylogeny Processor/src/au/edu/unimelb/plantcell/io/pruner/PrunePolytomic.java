package au.edu.unimelb.plantcell.io.pruner;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Removes taxa which are descended from a non-binary node
 * 
 * @author andrew.cassin
 *
 */
public class PrunePolytomic implements PruningStrategy {
	private HashSet<String> rejected_taxa = new HashSet<String>();
	private boolean m_reduce = false;
	
	public PrunePolytomic() {
		this(false);		// remove all descendants
	}
	
	public PrunePolytomic(boolean reduce_to_binary) {
		m_reduce = reduce_to_binary;
	}

	@Override
	public void execute(Phylogeny input_tree, Map<String, SequenceValue> taxa)
			throws Exception {
		rejected_taxa.clear();
		for (PhylogenyNodeIterator it = input_tree.iteratorPreorder(); it.hasNext(); ) {
			PhylogenyNode n = it.next();
			
			if (!n.isRoot() && n.getNumberOfDescendants() > 2) {
				List<PhylogenyNode> externals = n.getAllExternalDescendants();
				if (!m_reduce) {
					n.getParent().removeChildNode(n);
					//Logger.getAnonymousLogger().info("Pruning node ID: "+n.getId()+" descendants="+n.getNumberOfDescendants());
					for (PhylogenyNode kid : externals) {
						rejected_taxa.add(kid.getName());
					}
				} else {
					// remove all but 2 kids but leave n as part of the tree
					int idx = 0;
					for (PhylogenyNode kid : externals) {
						if (idx++ > 1) {
							rejected_taxa.add(kid.getName());
						}
					}
				}
			}
		}
	}

	@Override
	public boolean acceptTaxa(SequenceValue taxa) {
		if (rejected_taxa.contains(taxa.getID()))
			return false;
		return true;
	}

}
