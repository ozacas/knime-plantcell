package au.edu.unimelb.plantcell.annotations;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "RegionAnalyzer" Node.
 * Various nodes for analysis of sequence regions
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ExtractTrackNodeFactory extends NodeFactory<ExtractTrackNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtractTrackNodeModel createNodeModel() {
        return new ExtractTrackNodeModel();
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
    public NodeView<ExtractTrackNodeModel> createNodeView(final int viewIndex,
            final ExtractTrackNodeModel nodeModel) {
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
        return new ExtractTrackNodeDialog();
    }

}

