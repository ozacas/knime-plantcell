package au.edu.unimelb.plantcell.networks;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "NetworkViz" Node.
 * Support for network visualisation and analysis (jung, cytoscape, gephi)
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class NetworkVizNodeView extends NodeView<NetworkVizNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link NetworkVizNodeModel})
     */
    protected NetworkVizNodeView(final NetworkVizNodeModel nodeModel) {
        super(nodeModel);
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // TODO: generated method stub
    }

}

