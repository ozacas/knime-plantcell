package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;

/**
 * Baseclass implementation of all width models. This one does absolutely nothing: leaving the input data unchanged
 * 
 * @author acassin
 *
 */
public class DefaultWidthModel extends AbstractWidthModel {
	public void apply(final PhylogenyNode n) {
		assert(n != null);
	}
	
	public void finish(final Phylogeny p) {
		// yep.. do nothing
	}

	@Override
	public void start(Phylogeny p) {
		// no-op
	}
}
