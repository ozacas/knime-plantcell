package au.edu.unimelb.plantcell.views.surface.multi;

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "MultiSurface" Node.
 * Represents multiple surfaces each with different properties, surface datapoints are taken from the input data table.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MultiSurfaceNodeFactory 
        extends NodeFactory<MultiSurfaceNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public MultiSurfaceNodeModel createNodeModel() {
        return new MultiSurfaceNodeModel();
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
    public AbstractNodeView<MultiSurfaceNodeModel> createAbstractNodeView(final int viewIndex, final MultiSurfaceNodeModel mdl) {
    	return new MultiSurfaceNodeView<MultiSurfaceNodeModel>(mdl);
    }
    
    @Override
	public NodeView<MultiSurfaceNodeModel> createNodeView(int arg0,
			MultiSurfaceNodeModel arg1) {
		return (NodeView<MultiSurfaceNodeModel>) createAbstractNodeView(arg0, arg1);
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
        return new MultiSurfaceNodeDialog();
    }

}

