package au.edu.unimelb.plantcell.io.write.gff;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "FastaWriter" Node.
 * Creates a .fasta file with the specified accession, description, and sequence columns
 *
 * @author Andrew Cassin
 */
public class GFF2WriterNodeFactory 
        extends NodeFactory<GFF2WriterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public GFF2WriterNodeModel createNodeModel() {
        return new GFF2WriterNodeModel();
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
    public NodeView<GFF2WriterNodeModel> createNodeView(final int viewIndex,
            final GFF2WriterNodeModel nodeModel) {
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
        return new GFF2WriterNodeDialog();
    }

}

