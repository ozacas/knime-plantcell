package au.edu.unimelb.plantcell.io.pruner;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Creates a phyloxml document from the input data, decorated with data from the input table.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class TreePruneNodeFactory 
        extends NodeFactory<TreePruneNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public TreePruneNodeModel createNodeModel() {
        return new TreePruneNodeModel();
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
    public NodeView<TreePruneNodeModel> createNodeView(final int viewIndex,
            final TreePruneNodeModel nodeModel) {
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
        return new TreePruneNodeDialog();
    }

}

