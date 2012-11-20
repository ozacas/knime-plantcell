package au.edu.unimelb.plantcell.io.ws.suba;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class SubaNodeFactory extends NodeFactory<SubaNodeModel> {

	@Override
	public SubaNodeModel createNodeModel() {
		return new SubaNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<SubaNodeModel> createNodeView(int viewIndex,
			SubaNodeModel nodeModel) {
		// node has no view so...
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new SubaNodeDialog();
	}

}
