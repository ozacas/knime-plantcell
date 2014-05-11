package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.data.BranchColor;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;
import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;

/**
 * Baseclass implementation just picks the maximum pairwise datapoint from the matrix
 * and sets only the branch colour connecting each leaf node to the tree to this colour.
 * 
 * @author acassin
 *
 */
public class DefaultHeatModel extends AbstractHeatModel {
	private final Map<PhylogenyNode,Double> heat = new HashMap<PhylogenyNode,Double>();
	private Double max, min;
	private final ColourGradient cg = new ColourGradient(Color.RED, Color.BLUE);
	private NodeLogger logger;
	
	public DefaultHeatModel(final NodeLogger l) {
		super(l);
		assert(l != null);
		logger = l;
	}

	@Override
	public void apply(final PhylogenyNode n) {
		Double val = heat.get(n);
		// since the actual calculation is done during start(), we can just apply it...
		if (val == null || val.isNaN()) {
			return;
		}
		applyColourToNodeBranch(val, n);
	}
	
	/**
	 * 
	 * @param n
	 * @return always in the range [0..1] unless node is not in model, in which case Double.NaN is returned
	 */
	@Override
	public Double getHeat(final PhylogenyNode n) {
		Double ret = mapToRange(min, max, heat.get(n));
		return ret;
	}
	
	/**
	 * Linear colour gradient which requires heat to be in the range [min,max] or Double.NaN will result
	 * @param min
	 * @param max
	 * @param heat
	 * @return
	 */
	public double mapToRange(double min, double max, double heat) {
		if (heat < min || heat > max)
			return Double.NaN;
		double pc = (heat - min) / Math.abs(max-min);
		if (pc < 0.0 || pc > 1.0)
			return Double.NaN;		// calculation error!
		return pc;
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
	
	@Override
	public void start(final Phylogeny p, final BufferedDataTable in, int a_col, int heat_col) throws InvalidSettingsException {
		heat.clear();
		
		max = -Double.MAX_VALUE;
		min = Double.MAX_VALUE;
		for (DataRow r : in) {
			String taxa_name = asTaxaName(r.getCell(a_col));
			Double heat_value= asNumber(r.getCell(heat_col));
			
			PhylogenyNode n = p.getNode(taxa_name);
			if (n == null)
				continue;
			if (heat.containsKey(taxa_name)) {
				throw new InvalidSettingsException("Duplicate heat values are not permitted for "+taxa_name);
			}
			heat.put(n, Double.valueOf(heat_value));
			logger.debug("Heat for "+taxa_name+" is "+heat_value);
			if (heat_value > max)
				max = heat_value;
			if (heat_value < min)
				min = heat_value;
		}
		
		PhylogenyNodeIterator it = p.iteratorExternalForward();
		while (it.hasNext()) {
			PhylogenyNode n = it.next();
			assert(n.isExternal());
			
			propagateToRoot(n.getParent(), new PropagationFunction() {
				final Set<PhylogenyNode> done = new HashSet<PhylogenyNode>();

				@Override
				public void propagate(PhylogenyNode n, AbstractHeatModel mdl) {
					if (!done.contains(n)) {
						List<PhylogenyNode> kids = n.getAllExternalDescendants();
						List<Double> kid_heat = mdl.getHeat(kids);
						assert(!heat.containsKey(n));
						Double av = average(kid_heat);
						if (logger.getLevel().compareTo(LEVEL.DEBUG) >= 0) {		// expensive to log so dont if we dont have to...
							logger.debug("Average for "+kid_summary(kids)+ " is "+av);
							logger.debug("Found "+kid_heat.size()+" values to average for "+kid_summary(kids));
							logger.debug("Heat values are: "+double_list_summary(kid_heat));
						}
						heat.put(n, av);
						done.add(n);
					}
				}
			});
		}
	}
}
