package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import org.forester.phylogeny.PhylogenyNode;

public interface PropagationFunction {
	public void propagate(final PhylogenyNode n, final AbstractHeatModel mdl);
}
