package au.edu.unimelb.plantcell.io.ws.netsurfp;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "NetPhosAccessor" Node.
 * Implements support for the NetPhos PTM software
 *
 * @author Andrew Cassin
 */
public class NetSurfPAccessorNodeFactory 
        extends NodeFactory<NetSurfPAccessorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public NetSurfPAccessorNodeModel createNodeModel() {
        return new NetSurfPAccessorNodeModel();
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
    public NodeView<NetSurfPAccessorNodeModel> createNodeView(final int viewIndex,
            final NetSurfPAccessorNodeModel nodeModel) {
        return null;
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
        return new NetSurfPAccessorNodeDialog();
    }

}

