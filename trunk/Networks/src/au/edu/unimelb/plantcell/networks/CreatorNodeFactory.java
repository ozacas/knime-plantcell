package au.edu.unimelb.plantcell.networks;

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Creator" Node.
 * Using JUNG, this node creates a network cell for the input data correlated above a chosen threshold for each target variable.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class CreatorNodeFactory 
        extends NodeFactory<CreatorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public CreatorNodeModel createNodeModel() {
        return new CreatorNodeModel();
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
    public AbstractNodeView<CreatorNodeModel> createAbstractNodeView(final int viewIndex,
            final CreatorNodeModel nodeModel) {
    	return new CreatorNodeView(nodeModel);
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
        return new CreatorNodeDialog();
    }

	@Override
	public NodeView<CreatorNodeModel> createNodeView(int viewIndex,
			CreatorNodeModel nodeModel) {
		// this does not get called: see abstract node view code above
		return null;
	}

}

