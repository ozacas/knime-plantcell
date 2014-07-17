package au.edu.unimelb.plantcell.proteomics.proteowizard.filter;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class MS2DenoiseFilterNodeFactory extends NodeFactory<MS2DenoiseFilterNodeModel> {

	@Override
	public MS2DenoiseFilterNodeModel createNodeModel() {
		return new MS2DenoiseFilterNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<MS2DenoiseFilterNodeModel> createNodeView(int viewIndex,MS2DenoiseFilterNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new MS2DenoiseFilterNodeDialog();
	}

}
