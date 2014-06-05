package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import java.util.List;

import org.forester.phylogeny.PhylogenyNode;

public interface ModerationSelector {

	public List<PhylogenyNode> select(final PhylogenyNode n);
}
