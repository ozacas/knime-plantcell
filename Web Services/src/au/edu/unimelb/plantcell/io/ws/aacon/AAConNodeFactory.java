package au.edu.unimelb.plantcell.io.ws.aacon;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class AAConNodeFactory extends NodeFactory<AAConNodeModel> {

	@Override
	public AAConNodeModel createNodeModel() {
		return new AAConNodeModel(1, 1);
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<AAConNodeModel> createNodeView(int viewIndex,
			AAConNodeModel nodeModel) {
		// node has no view so...
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new AAConNodeDialog();
	}

}
