package au.edu.unimelb.plantcell.io.read.xtandem;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "XTandemReader" Node.
 * Uses the xtandem reader codebase, http://code.google.com/p/xtandem-parser/, to load identified spectra from XTandem and scores into KNIME.
 *
 * @author Andrew Cassin
 */
public class XTandemReaderNodeFactory 
        extends NodeFactory<XTandemReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public XTandemReaderNodeModel createNodeModel() {
        return new XTandemReaderNodeModel();
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
    public NodeView<XTandemReaderNodeModel> createNodeView(final int viewIndex,
            final XTandemReaderNodeModel nodeModel) {
        return new XTandemReaderNodeView(nodeModel);
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
        return new XTandemReaderNodeDialog();
    }

}

