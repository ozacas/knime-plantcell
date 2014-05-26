package au.edu.unimelb.plantcell.io.ws.mascot.msms;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the MS/MS Mascot Ion search Node.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MSMSSearchNodeFactory extends NodeFactory<MSMSSearchNodeModel> {

	@Override
	public MSMSSearchNodeModel createNodeModel() {
		return new MSMSSearchNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<MSMSSearchNodeModel> createNodeView(int viewIndex, MSMSSearchNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new MSMSSearchNodeDialog();
	}

}