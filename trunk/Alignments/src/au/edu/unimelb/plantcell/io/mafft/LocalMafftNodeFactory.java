package au.edu.unimelb.plantcell.io.mafft;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "LocalMafft" Node.
 * Supports running a local mafft executable and integration into the KNIME-PlantCell platform
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class LocalMafftNodeFactory 
        extends NodeFactory<LocalMafftNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalMafftNodeModel createNodeModel() {
        return new LocalMafftNodeModel();
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
    public NodeView<LocalMafftNodeModel> createNodeView(final int viewIndex,
            final LocalMafftNodeModel nodeModel) {
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
        return new LocalMafftNodeDialog();
    }

}

