package au.edu.unimelb.plantcell.io.muscle;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "LocalMuscle" Node.
 * Supports running a local muscle executable and integration into the KNIME-PlantCell platform
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class LocalMuscleNodeFactory 
        extends NodeFactory<LocalMuscleNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalMuscleNodeModel createNodeModel() {
        return new LocalMuscleNodeModel();
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
    public NodeView<LocalMuscleNodeModel> createNodeView(final int viewIndex,
            final LocalMuscleNodeModel nodeModel) {
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
        return new LocalMuscleNodeDialog();
    }

}

