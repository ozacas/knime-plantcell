package au.edu.unimelb.plantcell.io.ws.quickgo;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "QuickGOSource" Node.
 * Provides access to Gene Ontology (GO) data stored at http://www.ebi.ac.uk/QuickGO
 *
 * @author Andrew Cassin
 */
public class QuickGOSourceNodeFactory 
        extends NodeFactory<QuickGOSourceNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public QuickGOSourceNodeModel createNodeModel() {
        return new QuickGOSourceNodeModel();
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
    public NodeView<QuickGOSourceNodeModel> createNodeView(final int viewIndex,
            final QuickGOSourceNodeModel nodeModel) {
        return new QuickGOSourceNodeView(nodeModel);
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
        return new QuickGOSourceNodeDialog();
    }

}

