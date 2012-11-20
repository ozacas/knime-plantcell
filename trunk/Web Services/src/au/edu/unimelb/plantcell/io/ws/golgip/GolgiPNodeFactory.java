package au.edu.unimelb.plantcell.io.ws.golgip;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class GolgiPNodeFactory extends NodeFactory<GolgiPNodeModel> {

	@Override
	public GolgiPNodeModel createNodeModel() {
		return new GolgiPNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<GolgiPNodeModel> createNodeView(int viewIndex,
			GolgiPNodeModel nodeModel) {
		// node has no view so...
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new GolgiPNodeDialog();
	}

}
