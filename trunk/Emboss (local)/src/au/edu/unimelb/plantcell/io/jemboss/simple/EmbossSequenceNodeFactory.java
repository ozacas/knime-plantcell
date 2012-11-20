package au.edu.unimelb.plantcell.io.jemboss.simple;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "EmbossPredictor" Node.
 * Runs EMBOSS tools which take sequence(s) as input and provide a GFF output for inclusion as a annotation track on the output sequences.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class EmbossSequenceNodeFactory 
        extends NodeFactory<EmbossSequenceNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public EmbossSequenceNodeModel createNodeModel() {
        return new EmbossSequenceNodeModel();
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
    public NodeView<EmbossSequenceNodeModel> createNodeView(final int viewIndex,
            final EmbossSequenceNodeModel nodeModel) {
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
        return new EmbossSequenceNodeDialog();
    }

}

