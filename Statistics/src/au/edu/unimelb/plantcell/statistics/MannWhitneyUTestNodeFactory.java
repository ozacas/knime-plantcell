package au.edu.unimelb.plantcell.statistics;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Ranker" Node.
 * Basic descriptive and inferential statistics support built using the apache commons math v3 library.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MannWhitneyUTestNodeFactory 
        extends NodeFactory<MannWhitneyUTestNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public MannWhitneyUTestNodeModel createNodeModel() {
        return new MannWhitneyUTestNodeModel();
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
    public NodeView<MannWhitneyUTestNodeModel> createNodeView(final int viewIndex,
            final MannWhitneyUTestNodeModel nodeModel) {
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
        return new MannWhitneyUTestNodeDialog();
    }

}

