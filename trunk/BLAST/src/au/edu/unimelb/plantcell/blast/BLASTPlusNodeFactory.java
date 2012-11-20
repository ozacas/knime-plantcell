package au.edu.unimelb.plantcell.blast;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "BLASTPlus" Node.
 * Supports local execution of NCBI BLAST+ executables (which must be  installed separately)
 *
 * @author http://www.plantcell.unimelb.edu.au/
 */
public class BLASTPlusNodeFactory 
        extends NodeFactory<BLASTPlusNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public BLASTPlusNodeModel createNodeModel() {
        return new BLASTPlusNodeModel();
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
    public NodeView<BLASTPlusNodeModel> createNodeView(final int viewIndex,
            final BLASTPlusNodeModel nodeModel) {
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
        return new BLASTPlusNodeDialog();
    }

}

