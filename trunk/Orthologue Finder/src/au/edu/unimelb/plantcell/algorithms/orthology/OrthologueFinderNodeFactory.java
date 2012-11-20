package au.edu.unimelb.plantcell.algorithms.orthology;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "OrthologueFinder" Node.
 * Given two blast results (sequence A against database B and sequence B against database A) this node computes orthologous genes using Reciprocal Blast Hit (RBH) and other algorithms
 *
 * @author Andrew Cassin
 */
public class OrthologueFinderNodeFactory 
        extends NodeFactory<OrthologueFinderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public OrthologueFinderNodeModel createNodeModel() {
        return new OrthologueFinderNodeModel();
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
    public NodeView<OrthologueFinderNodeModel> createNodeView(final int viewIndex,
            final OrthologueFinderNodeModel nodeModel) {
        return new OrthologueFinderNodeView(nodeModel);
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
        return new OrthologueFinderNodeDialog();
    }

}

