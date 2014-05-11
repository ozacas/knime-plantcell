package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import java.util.ArrayList;
import java.util.List;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 * 
 * @author acassin
 *
 */
public abstract class AbstractHeatModel {
	@SuppressWarnings("unused")
	private NodeLogger logger;

	public AbstractHeatModel(final NodeLogger l) {
		logger = l;
	}

	protected String double_list_summary(List<Double> kid_heat) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		int idx = 0;
		for (Double d : kid_heat) {
			sb.append(String.valueOf(d));
			if (idx < kid_heat.size()-1) {
				sb.append(',');
			}
			idx++;
		}
		sb.append(']');
		return sb.toString();
	}
	
	protected String kid_summary(List<PhylogenyNode> kids) {
		if (kids == null || kids.size() < 1)
			return "No kids!";
		
		if (kids.size() <= 5) {
			StringBuilder sb = new StringBuilder();
			int idx = 0;
			for (PhylogenyNode n : kids) {
				sb.append(n.getName());
				if (sb.length() > 0 && idx < kids.size()-1) {
					sb.append(", ");
				}
				idx++;
			}
			if (sb.length() < 1) {
				sb.append("" + kids.size() + " nodes.");
			}
			return sb.toString();
		} else {
			int n = kids.size() - 3;
			ArrayList<PhylogenyNode> new_list = new ArrayList<PhylogenyNode>();
			new_list.add(kids.get(0));
			new_list.add(kids.get(1));
			new_list.add(kids.get(2));
			String summary = kid_summary(new_list);
			return summary + n + " other nodes.";
		}
	}
	
	protected Double average(final List<Double> kid_heat) {
		if (kid_heat == null || kid_heat.size() < 1)
			return 0.0d;
		double sum = 0.0d;
		for (Double d : kid_heat) {
			sum += d;
		}
		return sum / kid_heat.size();
	}
	
	protected Double maximum(final List<Double> vec) {
		Double max = -Double.NEGATIVE_INFINITY;
		for (Double d : vec) {
			if (d > max)
				max =d;
		}
		return max;
	}
	
	public String asTaxaName(final DataCell c) {
		if (c == null)
			return "";
		return c.toString();
	}
	
	public Double asNumber(final DataCell c) throws InvalidSettingsException {
		if (c instanceof DoubleValue)
			return ((DoubleValue)c).getDoubleValue();
		else if (c instanceof IntValue)
			return Double.valueOf(((IntValue)c).getIntValue());
		return Double.NaN;
	}
	
	protected void propagateToRoot(PhylogenyNode n,
			PropagationFunction propagationFunction) {
		propagationFunction.propagate(n, this);
		if (!n.isRoot()) {
			propagateToRoot(n.getParent(), propagationFunction);
		}
	}
	
	/**
	 * Called before the start of each Phylogeny to indicate to the model that analysis of a new tree is starting.
	 * Typically models will respond by initialising their state.
	 */
	public void start(final Phylogeny p, final BufferedDataTable in, int a_idx, int heat_idx) throws InvalidSettingsException {
		// no op
	}
	
	/**
	 * Compute the heat for the specified node in the tree. The caller is free to do nothing, or to alter
	 * parent nodes (eg. propagate heat to the root of the tree) as it sees fit. Best advice is not to override this method
	 * unless other methods are unsuitable.
	 * 
	 * @param n never null
	 */
	public abstract void apply(final PhylogenyNode n);
	
	/**
	 * Return the amount of heat associated with the specified node (or perhaps more accurately the branch which connects
	 * it to the rest of the tree). Tip nodes as well as internal nodes of the tree are called by the default implementation.
	 * 
	 * @param n 
	 * @return
	 */
	public abstract Double getHeat(final PhylogenyNode n);
	
	public List<Double> getHeat(List<PhylogenyNode> l) {
		ArrayList<Double> ret = new ArrayList<Double>();
		for (PhylogenyNode n : l) {
			Double d = getHeat(n);
			if (d != null && !d.isNaN()) {
				ret.add(d);
			}
		}
		return ret;
	}
	
	/**
	 * Called after traversal of the tree (ie. <code>apply()</code>) is complete, this method may
	 * finalise the heat values as computed during apply. Or nothing.
	 * 
	 * @param p the phylogeny which has just been traversed (maybe one of many trees in the given file)
	 */
	public void finish(final Phylogeny p) {
		// no op
	}
}
