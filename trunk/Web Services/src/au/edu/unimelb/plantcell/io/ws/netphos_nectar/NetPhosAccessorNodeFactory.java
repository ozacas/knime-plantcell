package au.edu.unimelb.plantcell.io.ws.netphos_nectar;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class NetPhosAccessorNodeFactory extends NodeFactory<NetPhosAccessorNodeModel> {

	@Override
	public NetPhosAccessorNodeModel createNodeModel() {
		return new NetPhosAccessorNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<NetPhosAccessorNodeModel> createNodeView(int viewIndex,
			NetPhosAccessorNodeModel nodeModel) {
		// node has no view so...
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new NetPhosAccessorNodeDialog();
	}

}
