package au.edu.unimelb.plantcell.annotations;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Change Track" Node.
 * Various nodes for analysis of sequence regions
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ChangeTrackNodeFactory extends NodeFactory<ChangeTrackNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeTrackNodeModel createNodeModel() {
        return new ChangeTrackNodeModel();
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
    public NodeView<ChangeTrackNodeModel> createNodeView(final int viewIndex,
            final ChangeTrackNodeModel nodeModel) {
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
        return new ChangeTrackNodeDialog();
    }

}

