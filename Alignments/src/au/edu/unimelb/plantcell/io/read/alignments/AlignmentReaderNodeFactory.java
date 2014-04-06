package au.edu.unimelb.plantcell.io.read.alignments;

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentNodeView;

/**
 * <code>NodeFactory</code> 
 * Supports running a local mafft executable and integration into the KNIME-PlantCell platform
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AlignmentReaderNodeFactory 
        extends NodeFactory<NodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeModel createNodeModel() {
        return new AlignmentReaderNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * Since this view is an external application (jalview) we override this method to ensure the view
     * works correctly with the KNIME platform, rather than the normal <code>createNodeView()</code>
     */
    @Override
    public AbstractNodeView<NodeModel> createAbstractNodeView(final int viewIndex, final NodeModel mdl) {
    	return new AlignmentNodeView(mdl);
    }
    
    /**
     * Not used by this view, hence always returns null.
     */
	@Override
	public NodeView<NodeModel> createNodeView(int viewIndex,NodeModel nodeModel) {
		// the only view for this node is an external application view, so this method...
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
        return new AlignmentReaderNodeDialog();
    }

}

