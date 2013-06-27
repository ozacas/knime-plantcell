package au.edu.unimelb.plantcell.io.convert;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "AnalystWiffConverter" Node.
 * Using a JAX-WS web service, this node converts a wiff file (optionally a .wiff.scan file too) to an open-format and then loads it as per Spectra Reader.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AnalystWiffConverterNodeFactory 
        extends NodeFactory<AnalystWiffConverterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalystWiffConverterNodeModel createNodeModel() {
        return new AnalystWiffConverterNodeModel();
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
    public NodeView<AnalystWiffConverterNodeModel> createNodeView(final int viewIndex,
            final AnalystWiffConverterNodeModel nodeModel) {
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
        return new AnalystWiffConverterNodeDialog();
    }

}

