package au.edu.unimelb.plantcell.io.pruner;

import java.util.HashMap;
import java.util.HashSet;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 * Removes duplicate named nodes (or if the node name is empty, the falls back to taxonomy scientific name, if still empty taxonomy common name)
 * 
 * @author andrew.cassin
 *
 */
public class RemoveDuplicateTaxa implements PruningStrategy {
	private final HashSet<PhylogenyNode> bad_nodes = new HashSet<PhylogenyNode>();
	
	@Override
	public void execute(TreePruneNodeModel mdl, Phylogeny input_tree) throws Exception {
		HashMap<String,Integer> name2count = new HashMap<String,Integer>((int) (input_tree.getNodeCount() * 1.25));
		
		// 1. identify species with more than one node in the tree
		for (PhylogenyNodeIterator it = input_tree.iteratorPreorder(); it.hasNext(); ) {
			PhylogenyNode n = it.next();
			if (!n.isExternal()) 
				continue;
			String name = mdl.getSpecies(n);
			if (name == null || name.length() < 1)
				continue;
			
			if (!name2count.containsKey(name)) {
				name2count.put(name, new Integer(1));
			} else {
				Integer i = name2count.get(name);
				name2count.put(name, new Integer(i.intValue()+1));
			}
		}
		
		// 2. go thru the tree nodes until the corresponding species has a count of one
		for (PhylogenyNodeIterator it = input_tree.iteratorPreorder(); it.hasNext(); ) {
			PhylogenyNode n = it.next();
			if (!n.isExternal())
				continue;
			String species = mdl.getSpecies(n);
			if (species == null)
				continue;
			Integer count = name2count.get(species);
			if (count == null)
				throw new InvalidSettingsException("Programmer error! Species not found! "+species);
			if (count == 1)
				continue;
			name2count.put(species, new Integer(count-1));
			bad_nodes.add(n);
			n.getParent().removeChildNode(n);
		}
	}

	@Override
	public boolean accept(final TreePruneNodeModel mdl, final PhylogenyNode n) {
		return !bad_nodes.contains(n);
	}

	@Override
	public void summary(final NodeLogger l, final Phylogeny p) {
		l.info("Removed "+bad_nodes.size()+" tip nodes from tree.");
		l.info("Tree now has "+p.getNodeCount()+ " nodes remaining.");
	}
}
