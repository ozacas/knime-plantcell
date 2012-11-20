package au.edu.unimelb.plantcell.io.ws.estscan_nectar;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class ESTScanNodeFactory extends NodeFactory<ESTScanNodeModel> {

	@Override
	public ESTScanNodeModel createNodeModel() {
		return new ESTScanNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<ESTScanNodeModel> createNodeView(int viewIndex,
			ESTScanNodeModel nodeModel) {
		// node has no view so...
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new ESTScanNodeDialog();
	}

}
