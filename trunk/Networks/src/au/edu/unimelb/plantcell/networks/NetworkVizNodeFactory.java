package au.edu.unimelb.plantcell.networks;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "NetworkViz" Node.
 * Support for network visualisation and analysis (jung, cytoscape, gephi)
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class NetworkVizNodeFactory 
        extends NodeFactory<NetworkVizNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkVizNodeModel createNodeModel() {
        return new NetworkVizNodeModel();
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
    public NodeView<NetworkVizNodeModel> createNodeView(final int viewIndex,
            final NetworkVizNodeModel nodeModel) {
        return new NetworkVizNodeView(nodeModel);
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
        return new NetworkVizNodeDialog();
    }

}

