package au.edu.unimelb.plantcell.io.pruner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.data.Confidence;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 * Uses forester library to remove tips from the tree which meet the following conditions:
 * 1) contain descendants which have 2 or more tips which belong to the same species
 * 2) have a support value in the parent >= support_threshold (100% by default)
 * 2) groups of same-specie tips contain sequences which meet the identity threshold (100% by default)
 * 
 * @author andrew.cassin
 *
 */
public class PruneShortestIdentity implements PruningStrategy {
	private int m_support;
	private int m_identity;
	private final HashSet<String> m_accepted = new HashSet<String>();		// taxa names of accepted nodes
	
	public PruneShortestIdentity() {
		this(100, 100);
	}
	
	public PruneShortestIdentity(int support_threshold, int identity_threshold) {
		m_support = support_threshold;
		m_identity= identity_threshold;		
	}
	
	@Override
	public void execute(final TreePruneNodeModel mdl, final Phylogeny input_tree) throws Exception {
		assert(input_tree != null && mdl != null);
		
		m_accepted.clear();
		for (final PhylogenyNodeIterator it = input_tree.iteratorPostorder(); it.hasNext(); ) {
			PhylogenyNode n = it.next();
			List<PhylogenyNode> externals = getExternalKids(n);
			if (n.isInternal() && externals != null && externals.size() >= 2) {
				for (String specie : getSuitableSpecies(externals)) {
					validateExternals(mdl, n, externals, specie);
				}
				
			}
		}
	}

	/**
	 * Look at the list of external taxa which are of the specified specie and decide if some of them should be pruned
	 * @param externals
	 * @param specie
	 * @param taxa2species
	 */
	private void validateExternals(final TreePruneNodeModel mdl, final PhylogenyNode parent, final List<PhylogenyNode> externals, String specie) 
			throws InvalidSettingsException {
		double c = getSupportValue(parent);
		// parent node have sufficient support? 
		if (!Double.isNaN(c)) {
			if (c < ((double)m_support/100.0))
					return;
			// else fall thru...
		} else {
			// no confidence? no pruning...
			return;
		}
		
		// what external nodes to consider for the specified parent?
		List<PhylogenyNode> candidates = getExternalCandidates(mdl, externals, specie);
		
		// remove candidates which are redundant according to chosen filter
		List<PhylogenyNode> accepted = new ArrayList<PhylogenyNode>();
		accepted.addAll(candidates);
		HashSet<PhylogenyNode> protected_candidates = new HashSet<PhylogenyNode>();
		for (PhylogenyNode candidate : candidates) {
			if (prune1candidate(mdl, candidate, accepted, protected_candidates)) {
				accepted.remove(candidate);
			} else {
				protected_candidates.add(candidate);
			}
		}
		
		// add them to accept list
		for (PhylogenyNode n : accepted) {
			m_accepted.add(mdl.getTaxa(n));
		}
	}

	/**
	 * Can we remove the candidate from the result list according to the filtering rules?
	 * @param c
	 * @param result
	 * @param taxa2seq
	 * @return
	 */
	private boolean prune1candidate(final TreePruneNodeModel mdl, final PhylogenyNode c, 
							final List<PhylogenyNode> result, final Set<PhylogenyNode> protected_nodes) {
		// cannot remove a protected node (required for another analysis to be true)
		if (protected_nodes.contains(c))
			return false;
		
		return false;
	}
	
	/**
	 * If the tree is properly curated the phyloxml will have a <code>&lt;confidence&gt;</code> tag of type bootstrap
	 * and we use that for the support value. If not present and its an internal node, we assume that the internal
	 * node name is the support value. If not either of these cases, <code>Double.NaN</code> is returned.
	 * 
	 * @param n
	 * @return
	 */
	private double getSupportValue(final PhylogenyNode n) {
		if (n.getBranchData().isHasConfidences()) {
			List<Confidence> clist = n.getBranchData().getConfidences();
			for (Confidence c : clist) {
				if (c.getType().equals("bootstrap"))
					return c.getValue();
			}
			// fall thru
		}
		
		if (n.isInternal()) {
			try {
				double d = Double.valueOf(n.getName());
				return d;
			} catch (Exception e) {
				return Double.NaN;
			}
		} 
		
		return Double.NaN;
	}

	private List<PhylogenyNode> getExternalCandidates(final TreePruneNodeModel mdl, List<PhylogenyNode> in, String wanted_specie) {
		List<PhylogenyNode> ret = new ArrayList<PhylogenyNode>();
		for (PhylogenyNode n : in) {
			String species = mdl.getSpecies(n);
			if (species != null && species.equals(wanted_specie)) {
				ret.add(n);
			}
		}
		return ret;
	}

	/**
	 * Returns a list of all external nodes which ARE DIRECT children of the specified node
	 */
	public List<PhylogenyNode> getExternalKids(PhylogenyNode parent) {
		assert(parent != null);
		List<PhylogenyNode> ret = new ArrayList<PhylogenyNode>();
		for (PhylogenyNode kid : parent.getDescendants()) {
			if (kid.isExternal())
				ret.add(kid);
		}
		
		return ret;
	}
	
	/**
	 * Returns a list of only those species represented at least twice in the given list of phylogenetic nodes
	 * @param externals
	 * @param smap map from taxa name to species name
	 * @return list of suitable species names identified from supplied external nodes
	 */
	private List<String> getSuitableSpecies(final List<PhylogenyNode> externals) {
		ArrayList<String> ret = new ArrayList<String>();
		
		return ret;
	}

	@Override
	public boolean accept(final TreePruneNodeModel mdl, final PhylogenyNode n) {
		String t = mdl.getTaxa(n);
		return m_accepted.contains(t);
	}

	@Override
	public void summary(final NodeLogger l, final Phylogeny p) {
		l.info("Accepted "+m_accepted.size()+" tip nodes from tree.");
		l.info("Tree now has "+p.getNodeCount()+ " nodes remaining.");
	}
}
