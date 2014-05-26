package au.edu.unimelb.plantcell.io.read.fasta;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "SingleFastaReader" Node.
 * This nodes reads sequences from the user-specified FASTA file and outputs three columns per sequence: * n1) Accession * n2) Description - often not accurate in practice * n3) Sequence data * n * nNo line breaks are preserved.
 *
 * @author acassin
 */
public class SingleFastaReaderNodeFactory 
        extends NodeFactory<SingleFastaReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SingleFastaReaderNodeModel createNodeModel() {
        return new SingleFastaReaderNodeModel();
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
    public NodeView<SingleFastaReaderNodeModel> createNodeView(final int viewIndex,
            final SingleFastaReaderNodeModel nodeModel) {
        return null;	// no view for this node
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
        return new SingleFastaReaderNodeDialog();
    }

}

