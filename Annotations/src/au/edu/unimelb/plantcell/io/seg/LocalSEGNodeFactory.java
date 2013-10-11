package au.edu.unimelb.plantcell.io.seg;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "LocalMafft" Node.
 * Supports running a local mafft executable and integration into the KNIME-PlantCell platform
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class LocalSEGNodeFactory 
        extends NodeFactory<LocalSEGNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalSEGNodeModel createNodeModel() {
        return new LocalSEGNodeModel();
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
    public NodeView<LocalSEGNodeModel> createNodeView(final int viewIndex,
            final LocalSEGNodeModel nodeModel) {
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
        return new LocalSEGNodeDialog();
    }

}

