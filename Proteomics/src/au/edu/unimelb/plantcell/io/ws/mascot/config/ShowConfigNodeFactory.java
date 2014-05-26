package au.edu.unimelb.plantcell.io.ws.mascot.config;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "DatFileDownload" Node.
 * Permits downloading of Mascot DAT files via a JAX-WS web service and will load each dat file into the output table as per the mascot reader. The node also saves the DAT files to the user-specified folder.
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

