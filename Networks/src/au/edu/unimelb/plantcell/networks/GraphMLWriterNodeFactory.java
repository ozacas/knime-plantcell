package au.edu.unimelb.plantcell.networks;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Creator" Node.
 * Using JUNG, this node creates a network cell for the input data correlated above a chosen threshold for each target variable.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class GraphMLWriterNodeFactory 
        extends NodeFactory<GraphMLWriterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public GraphMLWriterNodeModel createNodeModel() {
        return new GraphMLWriterNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
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
        return new GraphMLWriterNodeDialog();
    }

	@Override
	public NodeView<GraphMLWriterNodeModel> createNodeView(int viewIndex,
			GraphMLWriterNodeModel nodeModel) {
		return null;
	}

}

