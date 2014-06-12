package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import java.awt.Color;
import java.util.HashSet;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

public class BootstrapHeatModel extends DefaultHeatModel {
	private final HashSet<PhylogenyNode> has_heat = new HashSet<PhylogenyNode>();
	private final NodeLogger logger;
	
	public BootstrapHeatModel(NodeLogger l, ColourManager cm) {
		super(l, cm);
		logger = l;
	}

	@Override
	public void start(final Phylogeny p, final BufferedDataTable in, int a_col, int heat_col) throws InvalidSettingsException {
		PhylogenyNodeIterator it = p.iteratorPreorder();
		has_heat.clear();
		int success = 0;
		int failed = 0;
		
		while (it.hasNext()) {
			PhylogenyNode n = it.next();
			if (n == null || n.getBranchData() == null)
				continue;
			if (n.getBranchData().getNumberOfConfidences() > 0) {
				Double heat_value = n.getBranchData().getConfidence(0).getValue();
				if (getColourManager().addHeat(n, Color.BLACK, heat_value)) {
					success++;
					has_heat.add(n);
				} else {
					failed++;
				}
			}
		}
		logger.info("Failed to associate heat with "+failed+" nodes in tree, succeeded for "+success);
		
		getColourManager().propagate(getModerationSelector(), getHeatModerator());
	}
	
	@Override
	public boolean hasHeat(PhylogenyNode n) {
		return has_heat.contains(n);
	}
}
