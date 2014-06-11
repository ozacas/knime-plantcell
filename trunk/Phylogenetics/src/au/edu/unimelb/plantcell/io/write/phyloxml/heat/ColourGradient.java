package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.data.BranchColor;
import org.knime.core.node.NodeLogger;

/**
 * Only supports one Phylogeny, cannot reset internal state (one shot)
 * @author acassin
 *
 */
public class ColourGradient implements ColourManager {
	private final Color[] gradient;
	private final NodeLogger logger;
	private final Map<PhylogenyNode,Double> heat_map = new HashMap<PhylogenyNode,Double>();
	private Double min, max;
	
	public ColourGradient(final NodeLogger logger, final Color start, final Color end) {
		int n = 100;
		this.logger = logger;
		gradient = new Color[n];
		min = Double.MAX_VALUE;
		max = -Double.MAX_VALUE;
		for (int i=1; i<=n; i++) {
			double pc = (i / 100.0d);
			
			double red = start.getRed() * pc + end.getRed() * (1-pc);
			double green=start.getGreen() * pc + end.getGreen() * (1-pc);
			double blue =start.getBlue() * pc + end.getBlue() * (1-pc);
			
			gradient[i-1] = new Color((int)red, (int)green, (int)blue);;
		}
	}
	
	/**
	 * Linear colour gradient which requires heat to be in the range [min,max] or Double.NaN will result
	 * @param min
	 * @param max
	 * @param heat
	 * @return
	 */
	private double mapToRange(double min, double max, double heat) {
		if (heat < min || heat > max)
			return Double.NaN;
		double pc = (heat - min) / Math.abs(max-min);
		if (pc < 0.0 || pc > 1.0)
			return Double.NaN;		// calculation error!
		return pc;
	}
	
	private double getPercent(double heat) {
		return mapToRange(min, max, heat) * 100.0d;
	}
	
	private void apply(final PhylogenyNode n, double percent) {
		int idx = (int) percent;
		if (idx >= gradient.length)
			idx = gradient.length - 1;
		n.getBranchData().setBranchColor(new BranchColor(gradient[idx]));
	}

	@Override
	public boolean addHeat(PhylogenyNode n, Color row_colour, Double heat_value) {
		heat_map.put(n, heat_value);
		if (heat_value > max)
			max = heat_value;
		if (heat_value < min)
			min = heat_value;
		return true;
	}

	private Double fetchHeat(final PhylogenyNode n) throws Exception {
		if (heat_map.containsKey(n)) {
			return heat_map.get(n);
		} else {
			return 0.0d;
		}
	}

	@Override
	public void decorate(PhylogenyNode n) throws Exception {
		if (heat_map.containsKey(n)) { 
			apply(n, getPercent(fetchHeat(n)));
		}
		if (!n.isRoot()) {
			decorate(n.getParent());
		}
	}

	@Override
	public void propagate(ModerationSelector ms, HeatModerator hm) {
		final HashSet<PhylogenyNode> done = new HashSet<PhylogenyNode>();		// reserved solely for use by propagate();
		logger.info("Propagating heat throughout tree");
		// need to do this to avoid concurrent modification exception on heat_map
		List<PhylogenyNode> nodes_to_visit = new ArrayList<PhylogenyNode>(heat_map.keySet().size());
		nodes_to_visit.addAll(heat_map.keySet());
		for (PhylogenyNode n : nodes_to_visit) {
			PhylogenyNode root = n;
			while (!root.isRoot()) {
				root = root.getParent();
			}
			propagateUpTo(n, root, done, ms, hm);
		}
		logger.info("Propagated heat to "+done.size()+" additional nodes in the tree during heat propagation.");
	}


	private void propagateUpTo(final PhylogenyNode n, final PhylogenyNode root, Set<PhylogenyNode> done, final ModerationSelector ms, final HeatModerator hm) {
		// decorate() will apply the heat for us, so we dont do it again here
		if (!done.contains(n) && !heat_map.containsKey(n)) {
			List<PhylogenyNode> relevant_nodes = ms.select(n);
			List<Double> relevant_heat = new ArrayList<Double>(relevant_nodes.size());
			for (PhylogenyNode p : relevant_nodes) {
				if (!heat_map.containsKey(p)) {
					for (PhylogenyNode kid_of_p : p.getAllExternalDescendants()) {
						propagateUpTo(kid_of_p, p, done, ms, hm);
					}
				}
				try {
					double heat = fetchHeat(p);
					if (!Double.isNaN(heat)) {
						relevant_heat.add(heat);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (relevant_heat.size() != relevant_nodes.size()) {
				logger.warn("Node id: "+n.getId()+ " has inconsistent heat versus nodes: "+relevant_nodes.size()+" "+relevant_heat.size());
			} else if (relevant_heat.size() == 0) {
				logger.warn("Node id: "+n.getId()+" has no relevant heat from user-configured nodes!");
			}
			double moderated_heat = hm.moderate(relevant_heat);
			heat_map.put(n, moderated_heat);
			logger.info("Moderated heat for "+n.getId()+" is "+moderated_heat);
			done.add(n);
		}
		
		if (!n.equals(root)) {
			propagateUpTo(n.getParent(), root, done, ms, hm);
		}
	}
}
