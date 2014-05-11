package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;

/**
 * Similar to AbstractHeatModel
 * 
 * @author acassin
 * @see AbstractHeatModel
 */
public abstract class AbstractWidthModel {

	public abstract void start(final Phylogeny p);
	
	public abstract void apply(final PhylogenyNode n);
	
	public abstract void finish(final Phylogeny p);
}
