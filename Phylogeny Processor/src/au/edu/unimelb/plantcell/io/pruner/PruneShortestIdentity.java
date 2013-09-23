package au.edu.unimelb.plantcell.io.pruner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;
import org.knime.core.node.InvalidSettingsException;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

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
	private final Map<String,String> m_species_map = new HashMap<String,String>();	// maps for taxa id -> species
	private final HashSet<String> m_accepted = new HashSet<String>();		// taxa names of accepted nodes
	
	public PruneShortestIdentity() {
		this(100, 100, null);
	}
	
	public PruneShortestIdentity(int support_threshold, int identity_threshold, final Map<String,String> species_map) {
		m_support = support_threshold;
		m_identity= identity_threshold;		
		if (species_map != null)
			m_species_map.putAll(species_map);
	}
	
	@Override
	public void execute(final Phylogeny input_tree, final Map<String, SequenceValue> taxa) throws Exception {
		assert(input_tree != null && taxa != null && taxa.size() > 0);
		
		m_accepted.clear();
		m_species_map.clear();
		for (final PhylogenyNodeIterator it = input_tree.iteratorPostorder(); it.hasNext(); ) {
			PhylogenyNode n = it.next();
			List<PhylogenyNode> externals = getExternalKids(n);
			if (n.isInternal() && externals != null && externals.size() >= 2) {
				for (String specie : getSuitableSpecies(externals, m_species_map)) {
					validateExternals(n, externals, specie, m_species_map, taxa);
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
	private void validateExternals(final PhylogenyNode parent, final List<PhylogenyNode> externals, String specie, 
			final Map<String,String> taxa2species, final Map<String,SequenceValue> taxa2sequence) 
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
		List<PhylogenyNode> candidates = getExternalCandidates(externals, specie, taxa2species);
		
		// remove candidates which are redundant according to chosen filter
		List<PhylogenyNode> accepted = new ArrayList<PhylogenyNode>();
		accepted.addAll(candidates);
		HashSet<PhylogenyNode> protected_candidates = new HashSet<PhylogenyNode>();
		for (PhylogenyNode candidate : candidates) {
			if (prune1candidate(candidate, accepted, protected_candidates, taxa2sequence)) {
				accepted.remove(candidate);
			} else {
				protected_candidates.add(candidate);
			}
		}
		
		// add them to accept list
		for (PhylogenyNode n : accepted) {
			m_accepted.add(getTaxa(n.getName()));
		}
	}

	/**
	 * Can we remove the candidate from the result list according to the filtering rules?
	 * @param c
	 * @param result
	 * @param taxa2seq
	 * @return
	 */
	private boolean prune1candidate(final PhylogenyNode c, final List<PhylogenyNode> result, 
			final Set<PhylogenyNode> protected_nodes, final Map<String,SequenceValue> taxa2seq) {
		// cannot remove a protected node (required for another analysis to be true)
		if (protected_nodes.contains(c))
			return false;
		
		return false;
	}
	
	/**
	 * TODO BUG FIXME: Internal node names are assumed to have the support value...
	 * @param parent
	 * @return
	 */
	private double getSupportValue(PhylogenyNode parent) {
		try {
			double d = Double.valueOf(parent.getName());
			return d;
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	private List<PhylogenyNode> getExternalCandidates(List<PhylogenyNode> in, String wanted_specie, final Map<String,String> taxa2species) {
		List<PhylogenyNode> ret = new ArrayList<PhylogenyNode>();
		for (PhylogenyNode n : in) {
			String species = taxa2species.get(getTaxa(n.getName()));
			if (species != null && species.equals(wanted_specie)) {
				ret.add(n);
			}
		}
		return ret;
	}
	
	private String getTaxa(final String name) {
		return name;
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
	private List<String> getSuitableSpecies(List<PhylogenyNode> externals,
			Map<String, String> smap) {
		ArrayList<String> ret = new ArrayList<String>();
		
		return ret;
	}

	@Override
	public boolean acceptTaxa(final SequenceValue taxa) {
		String t = getTaxa(taxa.getID());
		return m_accepted.contains(t);
	}

}
