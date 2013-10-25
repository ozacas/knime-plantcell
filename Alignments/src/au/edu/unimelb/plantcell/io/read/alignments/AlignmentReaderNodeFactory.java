package au.edu.unimelb.plantcell.io.read.alignments;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> 
 * Supports running a local mafft executable and integration into the KNIME-PlantCell platform
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AlignmentReaderNodeFactory 
        extends NodeFactory<AlignmentReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public AlignmentReaderNodeModel createNodeModel() {
        return new AlignmentReaderNodeModel();
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
    public NodeView<AlignmentReaderNodeModel> createNodeView(final int viewIndex,
            final AlignmentReaderNodeModel nodeModel) {
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
        return new AlignmentReaderNodeDialog();
    }

}

