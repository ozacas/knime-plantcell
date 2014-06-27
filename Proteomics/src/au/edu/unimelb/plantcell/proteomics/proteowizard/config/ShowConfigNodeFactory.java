package au.edu.unimelb.plantcell.proteomics.proteowizard.config;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "AnalystWiffConverter" Node.
 * Using a JAX-WS web service, this node converts a wiff file (optionally a .wiff.scan file too) to an open-format and then loads it as per Spectra Reader.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ShowConfigNodeFactory 
        extends NodeFactory<ShowConfigNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ShowConfigNodeModel createNodeModel() {
        return new ShowConfigNodeModel();
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
    public NodeView<ShowConfigNodeModel> createNodeView(final int viewIndex,
            final ShowConfigNodeModel nodeModel) {
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
        return new ShowConfigNodeDialog();
    }

}

