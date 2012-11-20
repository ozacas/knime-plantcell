package au.edu.unimelb.plantcell.io.ws.targetp_nectar;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TargetPAccessor" Node.
 * Integrates the CBS TargetP web service into KNIME providing subcellular location predictions of given protein sequences
 *
 * @author Andrew Cassin
 */
public class TargetPAccessorNodeFactory 
        extends NodeFactory<TargetPAccessorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public TargetPAccessorNodeModel createNodeModel() {
        return new TargetPAccessorNodeModel();
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
    public NodeView<TargetPAccessorNodeModel> createNodeView(final int viewIndex,
            final TargetPAccessorNodeModel nodeModel) {
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
        return new TargetPAccessorNodeDialog();
    }

}

