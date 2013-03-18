package au.edu.unimelb.plantcell.annotations;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "RegionAnalyzer" Node.
 * Various nodes for analysis of sequence regions * 
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class SequenceWindow2ColumnsNodeFactory extends NodeFactory<SequenceWindow2ColumnsNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SequenceWindow2ColumnsNodeModel createNodeModel() {
        return new SequenceWindow2ColumnsNodeModel();
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
    public NodeView<SequenceWindow2ColumnsNodeModel> createNodeView(final int viewIndex,
            final SequenceWindow2ColumnsNodeModel nodeModel) {
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
        return new SequenceWindow2ColumnsNodeDialog();
    }

}

