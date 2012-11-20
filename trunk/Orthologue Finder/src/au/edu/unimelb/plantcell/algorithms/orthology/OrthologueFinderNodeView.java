package au.edu.unimelb.plantcell.algorithms.orthology;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "OrthologueFinder" Node.
 * Given two blast results (sequence A against database B and sequence B against database A) this node computes orthologous genes using Reciprocal Blast Hit (RBH) and other algorithms
 *
 * @author Andrew Cassin
 */
public class OrthologueFinderNodeView extends NodeView<OrthologueFinderNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link OrthologueFinderNodeModel})
     */
    protected OrthologueFinderNodeView(final OrthologueFinderNodeModel nodeModel) {
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
        OrthologueFinderNodeModel nodeModel = 
            (OrthologueFinderNodeModel)getNodeModel();
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

