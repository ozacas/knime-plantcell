package au.edu.unimelb.plantcell.proteomics.spectra.filtering;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "SpectraFilter" Node.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class SpectraFilteringNodeFactory 
        extends NodeFactory<SpectraFilteringNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SpectraFilteringNodeModel createNodeModel() {
        return new SpectraFilteringNodeModel();
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
    public NodeView<SpectraFilteringNodeModel> createNodeView(final int viewIndex,
            final SpectraFilteringNodeModel nodeModel) {
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
        return new SpectraFilteringNodeDialog();
    }

}

