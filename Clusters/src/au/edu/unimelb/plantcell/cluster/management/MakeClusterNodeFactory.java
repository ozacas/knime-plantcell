package au.edu.unimelb.plantcell.cluster.management;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "MakeCluster" Node.
 * Support for jClouds API (and maybe apache whirr too) from within KNIME for analysis requiring a cluster. Intention is to support things like Hadoop, BLAST+ etc. on the cloud
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MakeClusterNodeFactory 
        extends NodeFactory<MakeClusterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public MakeClusterNodeModel createNodeModel() {
        return new MakeClusterNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<MakeClusterNodeModel> createNodeView(final int viewIndex,
            final MakeClusterNodeModel nodeModel) {
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
        return new MakeClusterNodeDialog();
    }

}

