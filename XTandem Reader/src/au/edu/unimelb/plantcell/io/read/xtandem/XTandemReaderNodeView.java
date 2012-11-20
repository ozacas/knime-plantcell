package au.edu.unimelb.plantcell.io.read.xtandem;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "XTandemReader" Node.
 * Uses the xtandem reader codebase, http://code.google.com/p/xtandem-parser/, to load identified spectra from XTandem and scores into KNIME.
 *
 * @author Andrew Cassin
 */
public class XTandemReaderNodeView extends NodeView<XTandemReaderNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link XTandemReaderNodeModel})
     */
    protected XTandemReaderNodeView(final XTandemReaderNodeModel nodeModel) {
        super(nodeModel);

        // TODO instantiate the components of the view here.

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {

        // TODO retrieve the new model from your nodemodel and 
        // update the view.
        XTandemReaderNodeModel nodeModel = 
            (XTandemReaderNodeModel)getNodeModel();
        assert nodeModel != null;
        
        // be aware of a possibly not executed nodeModel! The data you retrieve
        // from your nodemodel could be null, emtpy, or invalid in any kind.
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    
        // TODO things to do when closing the view
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {

        // TODO things to do when opening the view
    }

}

