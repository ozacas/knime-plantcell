package au.edu.unimelb.plantcell.io.ws.multialign;

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "MultiAligner" Node.
 * 
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MultiAlignerNodeFactory 
        extends NodeFactory<MultiAlignerNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public MultiAlignerNodeModel createNodeModel() {
        return new MultiAlignerNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractNodeView<MultiAlignerNodeModel> createAbstractNodeView(final int viewIndex, final MultiAlignerNodeModel mdl) {
    	return new MultiAlignerNodeView(mdl);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new MultiAlignerNodeDialog();
    }

	@Override
	public NodeView<MultiAlignerNodeModel> createNodeView(int viewIndex,
			MultiAlignerNodeModel nodeModel) {
		// the only view for this node is an external application view, so this method...
		return null;
	}

}

