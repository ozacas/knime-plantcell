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
public class String2SequenceNodeFactory extends NodeFactory<String2SequenceNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public String2SequenceNodeModel createNodeModel() {
        return new String2SequenceNodeModel();
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
    public NodeView<String2SequenceNodeModel> createNodeView(final int viewIndex,
            final String2SequenceNodeModel nodeModel) {
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
        return new String2SequenceNodeDialog();
    }

}

