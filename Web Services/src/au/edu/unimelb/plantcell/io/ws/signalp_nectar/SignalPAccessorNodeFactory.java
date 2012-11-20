package au.edu.unimelb.plantcell.io.ws.signalp_nectar;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class SignalPAccessorNodeFactory extends NodeFactory<SignalPAccessorNodeModel> {

	@Override
	public SignalPAccessorNodeModel createNodeModel() {
		return new SignalPAccessorNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<SignalPAccessorNodeModel> createNodeView(int viewIndex,
			SignalPAccessorNodeModel nodeModel) {
		// node has no view so...
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new SignalPAccessorNodeDialog();
	}

}
