package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.knime.core.node.NodeLogger;

/**
 * Similar to AbstractHeatModel
 * 
 * @author acassin
 * @see AbstractHeatModel
 */
public abstract class AbstractWidthModel {

	public AbstractWidthModel(final NodeLogger l) {
		
	}
	
	public abstract void start(final Phylogeny p, final AbstractHeatModel hm);
	
	public abstract void apply(final PhylogenyNode n);
	
	public abstract void finish(final Phylogeny p);
}
