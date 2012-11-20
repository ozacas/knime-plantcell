package au.edu.unimelb.plantcell.io.ntf;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "NaiveTaxonomyFilter" Node.
 * Used to extract sequences from a FASTA file which match a given column of taxa. Regular expressions can be provided to match the taxa entry from the description in the FASTA file. Taxa desired form the input to the node.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class NaiveTaxonomyFilterNodeView extends NodeView<NaiveTaxonomyFilterNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link NaiveTaxonomyFilterNodeModel})
     */
    protected NaiveTaxonomyFilterNodeView(final NaiveTaxonomyFilterNodeModel nodeModel) {
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
        NaiveTaxonomyFilterNodeModel nodeModel = 
            (NaiveTaxonomyFilterNodeModel)getNodeModel();
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

