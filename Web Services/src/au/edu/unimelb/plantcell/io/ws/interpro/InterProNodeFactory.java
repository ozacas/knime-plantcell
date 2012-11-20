package au.edu.unimelb.plantcell.io.ws.interpro;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "InterProScan" Node.
 * Accesses the EBI webservice: interproscan with the user-specified settings
 *
 * @author Andrew Cassin
 */
public class InterProNodeFactory 
        extends NodeFactory<InterProNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public InterProNodeModel createNodeModel() {
        return new InterProNodeModel();
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
    public NodeView<InterProNodeModel> createNodeView(final int viewIndex,
            final InterProNodeModel nodeModel) {
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
        return new InterProNodeDialog();
    }

}

