package au.edu.unimelb.plantcell.proteomics.gravy;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "GRAVY Scorer" Node.
 * Often using javaprotlib, a few useful nodes for Proteomics calculations
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class GravyScorerNodeFactory 
        extends NodeFactory<GravyScorerNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public GravyScorerNodeModel createNodeModel() {
        return new GravyScorerNodeModel();
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
    public NodeView<GravyScorerNodeModel> createNodeView(final int viewIndex,
            final GravyScorerNodeModel nodeModel) {
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
        return new GravyScorerNodeDialog();
    }

}

