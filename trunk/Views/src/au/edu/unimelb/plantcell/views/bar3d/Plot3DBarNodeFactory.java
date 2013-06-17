package au.edu.unimelb.plantcell.views.bar3d;

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Plot3DBar" Node.
 * Using jzy3d, this node produces a 3d bar plot using the supplied input columns.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class Plot3DBarNodeFactory 
        extends NodeFactory<Plot3DBarNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Plot3DBarNodeModel createNodeModel() {
        return new Plot3DBarNodeModel();
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
    public AbstractNodeView<Plot3DBarNodeModel> createAbstractNodeView(final int viewIndex, final Plot3DBarNodeModel mdl) {
    	return new Plot3DBarNodeView(mdl);
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
        return new Plot3DBarNodeDialog();
    }

	@Override
	public NodeView<Plot3DBarNodeModel> createNodeView(int arg0,
			Plot3DBarNodeModel arg1) {
		return (NodeView<Plot3DBarNodeModel>) createAbstractNodeView(arg0, arg1);
	}

}

