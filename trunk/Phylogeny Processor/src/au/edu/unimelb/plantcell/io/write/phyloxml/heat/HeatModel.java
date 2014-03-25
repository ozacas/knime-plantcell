package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import java.util.List;

import org.forester.phylogeny.PhylogenyNode;

import au.edu.unimelb.plantcell.io.write.phyloxml.heat.PairwiseHeatMatrix.Pair;

/**
 * Baseclass implementation just picks the maximum pairwise datapoint from the matrix
 * and sets only the branch colour to each leaf to this colour.
 * 
 * @author acassin
 *
 */
public class HeatModel {
	private PairwiseHeatMatrix phm;
	
	public HeatModel(PairwiseHeatMatrix phm) {
		assert(phm != null);
		this.phm = phm;
	}
	
	public void apply(final PhylogenyNode n) {
		assert(n != null);
		
		if (!n.isExternal())
			return;
		
		List<Pair> all_pairs_for_this_leaf = phm.findAll(n.getName());
		List<Double> all_values = phm.findAllValues(all_pairs_for_this_leaf);
		
	}
	
	public void finish() {
		phm = null;
	}
}
