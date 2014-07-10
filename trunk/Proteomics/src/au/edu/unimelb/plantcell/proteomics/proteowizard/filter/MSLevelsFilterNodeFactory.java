package au.edu.unimelb.plantcell.proteomics.proteowizard.filter;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class MSLevelsFilterNodeFactory extends NodeFactory<MSLevelsFilterNodeModel> {

	@Override
	public MSLevelsFilterNodeModel createNodeModel() {
		return new MSLevelsFilterNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<MSLevelsFilterNodeModel> createNodeView(int viewIndex,MSLevelsFilterNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new MSLevelsFilterNodeDialog();
	}

}
