package au.edu.unimelb.plantcell.io.ws.predgpi;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class PredGPINodeFactory extends NodeFactory<PredGPINodeModel> {

	@Override
	public PredGPINodeModel createNodeModel() {
		return new PredGPINodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<PredGPINodeModel> createNodeView(int viewIndex,
			PredGPINodeModel nodeModel) {
		// node has no view so...
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new PredGPINodeDialog();
	}

}
