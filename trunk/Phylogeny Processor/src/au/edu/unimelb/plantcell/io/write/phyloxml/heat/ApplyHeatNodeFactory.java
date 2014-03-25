package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * applies heat to a tree using the forester library
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ApplyHeatNodeFactory 
        extends NodeFactory<ApplyHeatNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ApplyHeatNodeModel createNodeModel() {
        return new ApplyHeatNodeModel();
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
    public NodeView<ApplyHeatNodeModel> createNodeView(final int viewIndex,
            final ApplyHeatNodeModel nodeModel) {
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
        return new ApplyHeatNodeDialog();
    }

}

