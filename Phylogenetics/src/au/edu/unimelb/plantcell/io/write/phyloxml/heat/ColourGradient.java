package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private final Map<PhylogenyNode,Double> moderated_heat = new HashMap<PhylogenyNode,Double>();
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
		if (n.isExternal()) {
			heat_map.put(n, heat_value);
			if (heat_value > max)
				max = heat_value;
			if (heat_value < min)
				min = heat_value;
			return true;
		} else if (canPropagate()) {
			moderated_heat.put(n, heat_value);
			if (heat_value > max)
				max = heat_value;
			if (heat_value < min)
				min = heat_value;
		}
		return false;
	}

	private Double fetchHeat(final PhylogenyNode n) throws Exception {
		if (heat_map.containsKey(n)) {
			return heat_map.get(n);
		} else if (moderated_heat.containsKey(n)) {
			return moderated_heat.get(n);
		} else {
			throw new Exception("No data for node: "+n.getId()+" "+n.getName()+" "+n.isExternal());
		}
	}
	
	private List<Double> fetchHeat(final List<PhylogenyNode> collection, final ModerationSelector ms, final HeatModerator hm) throws Exception {
		int n = 0;
		if (collection != null) {
			n = collection.size();
		}
		ArrayList<Double> ret = new ArrayList<Double>(n);
		if (collection == null) {
			return ret;
		}
		resolveAll(collection, ms, hm);
		for (PhylogenyNode p : collection) {
			ret.add(fetchHeat(p));
		}
		return ret;
	}

	@Override
	public boolean canPropagate() {
		return true;
	}

	@Override
	public void decorate(PhylogenyNode n, ModerationSelector ms, HeatModerator hm) throws Exception {
		if (n.isExternal()) { 
			double heat = heat_map.get(n);
			apply(n, getPercent(heat));
		} 
		if (canPropagate()) {
			if (!moderated_heat.containsKey(n)) {
				List<PhylogenyNode> relevant_nodes = ms.select(n);
				List<Double> relevant_heat = fetchHeat(relevant_nodes, ms, hm);
				if (relevant_nodes != null && relevant_heat.size() == relevant_nodes.size()) {
					double heat = hm.moderate(relevant_heat);
					logger.info("Moderated heat for "+n.getId()+ " is "+heat+" ("+relevant_nodes.size()+" relevant nodes)");
					moderated_heat.put(n, heat);
					apply(n, getPercent(heat));
				}
			}
			if (!n.isRoot()) {
				decorate(n.getParent(), ms, hm);
			}
		}
	}

	
	private void resolveAll(List<PhylogenyNode> relevant_nodes, ModerationSelector ms, HeatModerator hm) throws Exception {
		if (relevant_nodes == null)
			return;
		
		for (PhylogenyNode p : relevant_nodes) {
			resolve(p, ms, hm);
		}
	}


	private List<PhylogenyNode> resolve(final PhylogenyNode n, final ModerationSelector ms, final HeatModerator hm) throws Exception {
		logger.info("Resolving node id: "+n.getId());
		List<PhylogenyNode> kids = ms.select(n);
		if (kids == null) {
			logger.info("No kids for "+n.getId());
			return null;
		}
	
		for (PhylogenyNode kid : kids) {
			List<PhylogenyNode> new_kids = resolve(kid, ms, hm);
			double heat = 0.0d;
			
			if (new_kids != null) {
				heat = hm.moderate(fetchHeat(new_kids, ms, hm));
			} else {
				heat = fetchHeat(kid);
			}
			logger.info("Added resolved moderated heat for "+kid.getId()+" to "+heat);
			addHeat(kid, Color.BLACK, heat);
		}
		return kids;
	}
}
