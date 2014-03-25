package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import java.awt.Color;
import java.util.List;

import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.data.BranchColor;

import au.edu.unimelb.plantcell.io.write.phyloxml.heat.PairwiseHeatMatrix.Pair;

/**
 * Baseclass implementation just picks the maximum pairwise datapoint from the matrix
 * and sets only the branch colour connecting each leaf node to the tree to this colour.
 * 
 * @author acassin
 *
 */
public class HeatModel {
	private PairwiseHeatMatrix phm;
	private final ColourGradient cg = new ColourGradient(Color.RED, Color.GREEN);
	
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
		double val = getValue(all_values);
		applyColourToNodeBranch(val, n);
	}
	
	public ColourGradient getColourGradient() {
		return cg;
	}
	
	public void applyColourToNodeBranch(double percent, final PhylogenyNode n) {
		assert(percent >= 0.0d && percent <= 1.0d && n != null);
		BranchColor bc = new BranchColor();
		bc.setValue(getColourGradient().getColor(percent));
		n.getBranchData().setBranchColor(bc);
	}
	
	/**
	 * Given a list of heat values
	 * @param l
	 * @return
	 */
	public double getValue(List<Double> l) {
		if (l == null || l.size() < 1)
			return 0.0d;
		double max = Double.NEGATIVE_INFINITY;
		for (Double d : l) {
			if (d.doubleValue() > max)
				max = d.doubleValue();
		}
		return max;
	}
	
	public void finish() {
		phm = null;
	}
}
