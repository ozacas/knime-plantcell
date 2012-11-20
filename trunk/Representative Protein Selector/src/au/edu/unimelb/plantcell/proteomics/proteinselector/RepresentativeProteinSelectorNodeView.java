package au.edu.unimelb.plantcell.proteomics.proteinselector;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "RepresentativeProteinSelector" Node.
 * Selects, amongst proteins which share peptides, a representative sequence. In the future, this will provide multiple strategies for doing this: but only one for now. Designed to match the results from the ACPFG String Matcher
 *
 * @author Andrew Cassin
 */
public class RepresentativeProteinSelectorNodeView extends NodeView<RepresentativeProteinSelectorNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link RepresentativeProteinSelectorNodeModel})
     */
    protected RepresentativeProteinSelectorNodeView(final RepresentativeProteinSelectorNodeModel nodeModel) {
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
        RepresentativeProteinSelectorNodeModel nodeModel = 
            (RepresentativeProteinSelectorNodeModel)getNodeModel();
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

