package au.edu.unimelb.plantcell.gene.prediction;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "AugustusNodeModel" Node.
 * Runs augustus (http://augustus.gobics.de) on the local computer and loads its predictions into a KNIME table.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AugustusNodeFactory 
        extends NodeFactory<AugustusNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public AugustusNodeModel createNodeModel() {
        return new AugustusNodeModel();
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
    public NodeView<AugustusNodeModel> createNodeView(final int viewIndex,
            final AugustusNodeModel nodeModel) {
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
        return new AugustusNodeDialog();
    }

}

