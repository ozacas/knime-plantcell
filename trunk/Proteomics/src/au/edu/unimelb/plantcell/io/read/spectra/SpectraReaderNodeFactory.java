package au.edu.unimelb.plantcell.io.read.spectra;

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory class for the open format spectra reader node
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class SpectraReaderNodeFactory 
        extends NodeFactory<SpectraReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SpectraReaderNodeModel createNodeModel() {
        return new SpectraReaderNodeModel();
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
    public AbstractNodeView<SpectraReaderNodeModel> createAbstractNodeView(final int viewIndex, final SpectraReaderNodeModel mdl) {
    	return new PeakHeatmapNodeView(mdl);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<SpectraReaderNodeModel> createNodeView(final int viewIndex,
            final SpectraReaderNodeModel nodeModel) {
        return (NodeView<SpectraReaderNodeModel>) createAbstractNodeView(viewIndex, nodeModel);
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
        return new SpectraReaderNodeDialog();
    }

}

