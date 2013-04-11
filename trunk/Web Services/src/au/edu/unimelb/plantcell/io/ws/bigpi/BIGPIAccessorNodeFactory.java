package au.edu.unimelb.plantcell.io.ws.bigpi;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class BIGPIAccessorNodeFactory extends NodeFactory<BIGPIAccessorNodeModel> {

	@Override
	public BIGPIAccessorNodeModel createNodeModel() {
		return new BIGPIAccessorNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<BIGPIAccessorNodeModel> createNodeView(int viewIndex,
			BIGPIAccessorNodeModel nodeModel) {
		// node has no view so...
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new BIGPIAccessorNodeDialog();
	}

}
