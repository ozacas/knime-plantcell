package au.edu.unimelb.plantcell.proteomics.digestion;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "InsilicoDigestor" Node.
 * Often using javaprotlib, a few useful nodes for Proteomics Mass Spectrometry
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class InsilicoDigestorNodeFactory 
        extends NodeFactory<InsilicoDigestorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public InsilicoDigestorNodeModel createNodeModel() {
        return new InsilicoDigestorNodeModel();
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
    public NodeView<InsilicoDigestorNodeModel> createNodeView(final int viewIndex,
            final InsilicoDigestorNodeModel nodeModel) {
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
        return new InsilicoDigestorNodeDialog();
    }

}

