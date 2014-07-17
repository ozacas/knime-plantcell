package au.edu.unimelb.plantcell.proteomics.proteowizard.filter;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class ThresholdFilterNodeFactory extends NodeFactory<ThresholdFilterNodeModel> {

	@Override
	public ThresholdFilterNodeModel createNodeModel() {
		return new ThresholdFilterNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<ThresholdFilterNodeModel> createNodeView(int viewIndex,ThresholdFilterNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new ThresholdFilterNodeDialog();
	}

}
