package au.edu.unimelb.plantcell.hhsuite;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "HHSuite" Node.
 * Provides remote homology detection (much more sensitive than BLAST) with most of the performance. 
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class HHSuiteNodeFactory 
        extends NodeFactory<HHSuiteNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public HHSuiteNodeModel createNodeModel() {
        return new HHSuiteNodeModel();
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
    public NodeView<HHSuiteNodeModel> createNodeView(final int viewIndex,
            final HHSuiteNodeModel nodeModel) {
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
        return new HHSuiteNodeDialog();
    }

}

