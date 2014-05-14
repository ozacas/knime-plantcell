package au.edu.unimelb.plantcell.io.ws.netnglyc;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class NetNGlycNodeFactory extends NodeFactory<NetNGlycNodeModel> {

	@Override
	public NetNGlycNodeModel createNodeModel() {
		return new NetNGlycNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<NetNGlycNodeModel> createNodeView(int viewIndex,
			NetNGlycNodeModel nodeModel) {
		// node has no view so...
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new NetNGlycNodeDialog();
	}

}
