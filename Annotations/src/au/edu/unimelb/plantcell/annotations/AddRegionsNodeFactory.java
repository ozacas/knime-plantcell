package au.edu.unimelb.plantcell.annotations;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "RegionAnalyzer" Node.
 * Various nodes for analysis of sequence regions * 
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AddRegionsNodeFactory extends NodeFactory<AddRegionsNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public AddRegionsNodeModel createNodeModel() {
        return new AddRegionsNodeModel();
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
    public NodeView<AddRegionsNodeModel> createNodeView(final int viewIndex,
            final AddRegionsNodeModel nodeModel) {
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
        return new AddRegionsNodeDialog();
    }

}

