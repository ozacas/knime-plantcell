package au.edu.unimelb.plantcell.io.ws.quickgo;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "QuickGOSource" Node.
 * Provides access to Gene Ontology (GO) data stored at http://www.ebi.ac.uk/QuickGO
 *
 * @author Andrew Cassin
 */
public class QuickGOSourceNodeView extends NodeView<QuickGOSourceNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link QuickGOSourceNodeModel})
     */
    protected QuickGOSourceNodeView(final QuickGOSourceNodeModel nodeModel) {
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
        QuickGOSourceNodeModel nodeModel = 
            (QuickGOSourceNodeModel)getNodeModel();
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

