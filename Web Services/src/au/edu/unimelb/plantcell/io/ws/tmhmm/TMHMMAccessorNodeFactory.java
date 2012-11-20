package au.edu.unimelb.plantcell.io.ws.tmhmm;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class TMHMMAccessorNodeFactory extends NodeFactory<TMHMMAccessorNodeModel> {

	@Override
	public TMHMMAccessorNodeModel createNodeModel() {
		return new TMHMMAccessorNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<TMHMMAccessorNodeModel> createNodeView(int viewIndex,
			TMHMMAccessorNodeModel nodeModel) {
		// node has no view so...
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new TMHMMAccessorNodeDialog();
	}

}
