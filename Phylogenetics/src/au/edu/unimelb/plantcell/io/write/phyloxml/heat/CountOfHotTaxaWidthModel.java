package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import java.util.HashMap;
import java.util.List;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.data.BranchWidth;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;
import org.knime.core.node.NodeLogger;

/**
 * Keeps track of how many taxa (external nodes) in the tree have heat. Since the width model
 * is called after the heat model has computed, this code is being called at the right time. However,
 * we need more flexibility in the interactions between the models. But for now it is sufficient.
 * 
 * @author acassin
 *
 */
public class CountOfHotTaxaWidthModel extends DefaultWidthModel {
	private final HashMap<PhylogenyNode,Integer> widths_by_nodes = new HashMap<PhylogenyNode,Integer>();
	private final NodeLogger logger;
	private int     altered;
	private boolean rescale;
	
	public CountOfHotTaxaWidthModel(final NodeLogger l) {
		super(l);
		this.logger = l;
	}
	
	public CountOfHotTaxaWidthModel(NodeLogger logger2, boolean rescale_to_percentage) {
		this(logger2);
		setRescaleToPercentage(rescale_to_percentage);
	}

	public boolean rescale() {
		return rescale;
	}
	
	private void setRescaleToPercentage(boolean rescale_to_percentage) {
		rescale = rescale_to_percentage;
	}

	@Override
	public void start(Phylogeny p, AbstractHeatModel hm) {
		PhylogenyNodeIterator it = p.iteratorPreorder();
		widths_by_nodes.clear();
		altered = 0;
		int max = 0;
		while (it.hasNext()) {
			PhylogenyNode n = it.next();
			int count = getCount(n.getAllExternalDescendants(), hm);
			widths_by_nodes.put(n, count);
			if (count > max) {
				max = count;
			}
		}
		it = p.iteratorPreorder();
		while (it.hasNext()) {
			PhylogenyNode n = it.next();
			if (widths_by_nodes.containsKey(n)) {
				double val = ((double) widths_by_nodes.get(n));
				if (rescale()) {
					val = (val / max) * 100.0;		// branch widths in the range 1..100?
					if (val < 1.0d) {
						val = 1.0d;
					} 
					if (val > 100.0d) {
						val = 100.0d;
					}
				}
				setWidth(n, val);
				altered++;
			}
		}
	}

	private Integer getCount(final List<PhylogenyNode> taxa, final AbstractHeatModel hm) {
		int cnt = 0;
		for (PhylogenyNode n : taxa) {
			if (hm.hasHeat(n)) {
				cnt++;
			}
		}
		return cnt;
	}
	
	private void setWidth(PhylogenyNode n, double new_width) {
		n.getBranchData().setBranchWidth(new BranchWidth(new_width));
	}
	
	@Override
	public void finish(final Phylogeny p) {
		logger.info("Set branch widths for "+altered+" nodes in tree.");
	}
}
