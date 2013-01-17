package au.edu.unimelb.plantcell.ensembl;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "EnsembleAddHomologue" Node.
 * Adds homologues for the input data to the output table
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AddTranscriptNodeFactory 
        extends NodeFactory<AddTranscriptNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public AddTranscriptNodeModel createNodeModel() {
        return new AddTranscriptNodeModel();
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
    public NodeView<AddTranscriptNodeModel> createNodeView(final int viewIndex,
            final AddTranscriptNodeModel nodeModel) {
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
        return new AddTranscriptNodeDialog();
    }

}

