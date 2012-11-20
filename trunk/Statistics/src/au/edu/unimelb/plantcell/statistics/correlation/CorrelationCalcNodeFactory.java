package au.edu.unimelb.plantcell.statistics.correlation;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "LinearCorrelationCalc" Node.
 * Unlike the builtin knime node, this can perform large scale correlations by not storing the results into a matrix. I use it for AffyMetrix 21k gene correlations
 *
 * @author Andrew Cassin
 */
public class CorrelationCalcNodeFactory 
        extends NodeFactory<CorrelationCalcNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public CorrelationCalcNodeModel createNodeModel() {
        return new CorrelationCalcNodeModel();
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
    public NodeView<CorrelationCalcNodeModel> createNodeView(final int viewIndex,
            final CorrelationCalcNodeModel nodeModel) {
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
        return new CorrelationCalcNodeDialog();
    }

}

