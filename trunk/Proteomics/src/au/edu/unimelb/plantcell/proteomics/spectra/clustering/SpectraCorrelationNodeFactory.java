package au.edu.unimelb.plantcell.proteomics.spectra.clustering;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "SpectraQualityAssessor" Node.
 * Implements the 'Xrea' algorithm in the paper entitled "Quality Assessment of Tandem Mass Spectra Based on Cumulative Intensity Normalization" in the journal of proteome research. May implement other algorithms at a future date.
 *
 * @author Andrew Cassin
 */
public class SpectraCorrelationNodeFactory 
        extends NodeFactory<SpectraCorrelationNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SpectraCorrelationNodeModel createNodeModel() {
        return new SpectraCorrelationNodeModel();
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
    public NodeView<SpectraCorrelationNodeModel> createNodeView(final int viewIndex,
            final SpectraCorrelationNodeModel nodeModel) {
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
        return new SpectraCorrelationNodeDialog();
    }

}

