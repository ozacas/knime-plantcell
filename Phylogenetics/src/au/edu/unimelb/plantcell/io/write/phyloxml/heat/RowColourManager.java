package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.data.BranchColor;

/**
 * Assigns colours to nodes based on row colour for each node name. Only colourise the branches connecting
 * the taxa to the tree, does not support propagation since the colours cannot be inferred
 * 
 * @author acassin
 *
 */
public class RowColourManager implements ColourManager {
	private final Map<PhylogenyNode,Color> heat_map = new HashMap<PhylogenyNode,Color>();
	
	@Override
	public boolean addHeat(final PhylogenyNode n, final Color row_colour, Double heat_value) {
		if (n.isExternal()) {
			heat_map.put(n, row_colour);
			return true;
		}
		return false;
	}

	@Override
	public void decorate(PhylogenyNode n, ModerationSelector ms,
			HeatModerator hm) {
		if (heat_map.containsKey(n)) {
			n.getBranchData().setBranchColor(new BranchColor(heat_map.get(n)));
		}
	}

	@Override
	public boolean canPropagate() {
		return false;
	}

}
