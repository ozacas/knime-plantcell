package au.edu.unimelb.plantcell.io.ws.pfam;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "QuickGOSource" Node.
 * Provides access to Gene Ontology (GO) data stored at http://www.ebi.ac.uk/QuickGO
 *
 * @author Andrew Cassin
 */
public class PFAMSourceNodeFactory 
        extends NodeFactory<PFAMSourceNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PFAMSourceNodeModel createNodeModel() {
        return new PFAMSourceNodeModel();
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
    public NodeView<PFAMSourceNodeModel> createNodeView(final int viewIndex, final PFAMSourceNodeModel nodeModel) {
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
        return new PFAMSourceNodeDialog();
    }

}

