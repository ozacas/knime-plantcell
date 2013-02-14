package au.edu.unimelb.plantcell.annotations;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Alignment2Sequence" Node.
 * Various nodes for analysis of sequence regions * 
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class Alignment2SequenceNodeFactory extends NodeFactory<Alignment2SequenceNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Alignment2SequenceNodeModel createNodeModel() {
        return new Alignment2SequenceNodeModel();
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
    public NodeView<Alignment2SequenceNodeModel> createNodeView(final int viewIndex,
            final Alignment2SequenceNodeModel nodeModel) {
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
        return new Alignment2SequenceNodeDialog();
    }

}

