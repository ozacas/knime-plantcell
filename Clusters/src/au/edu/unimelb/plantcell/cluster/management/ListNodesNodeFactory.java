package au.edu.unimelb.plantcell.cluster.management;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "ListNodes" Node.
 * Lists the nodes available via the chosen provider
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ListNodesNodeFactory 
        extends NodeFactory<ListNodesNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ListNodesNodeModel createNodeModel() {
        return new ListNodesNodeModel();
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
    public NodeView<ListNodesNodeModel> createNodeView(final int viewIndex,
            final ListNodesNodeModel nodeModel) {
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
        return new ListNodesNodeDialog();
    }

}

