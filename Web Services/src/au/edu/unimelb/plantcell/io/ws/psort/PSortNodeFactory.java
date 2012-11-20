package au.edu.unimelb.plantcell.io.ws.psort;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class PSortNodeFactory extends NodeFactory<PSortNodeModel> {

	@Override
	public PSortNodeModel createNodeModel() {
		return new PSortNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<PSortNodeModel> createNodeView(int viewIndex,
			PSortNodeModel nodeModel) {
		// node has no view so...
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new PSortNodeDialog();
	}

}
