package au.edu.unimelb.plantcell.proteomics.fragmentation;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "InsilicoDigestor" Node.
 * Often using javaprotlib, a few useful nodes for Proteomics Mass Spectrometry
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class PeptideFragmentationNodeFactory 
        extends NodeFactory<PeptideFragmentationNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PeptideFragmentationNodeModel createNodeModel() {
        return new PeptideFragmentationNodeModel();
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
    public NodeView<PeptideFragmentationNodeModel> createNodeView(final int viewIndex,
            final PeptideFragmentationNodeModel nodeModel) {
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
        return new PeptideFragmentationNodeDialog();
    }

}

