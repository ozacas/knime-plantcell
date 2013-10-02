package au.edu.unimelb.plantcell.io.usearch;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "LocalMafft" Node.
 * Supports running a local mafft executable and integration into the KNIME-PlantCell platform
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class SeqClusterNodeFactory 
        extends NodeFactory<SeqClusterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SeqClusterNodeModel createNodeModel() {
        return new SeqClusterNodeModel();
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
    public NodeView<SeqClusterNodeModel> createNodeView(final int viewIndex,
            final SeqClusterNodeModel nodeModel) {
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
        return new SeqClusterNodeDialog();
    }

}

