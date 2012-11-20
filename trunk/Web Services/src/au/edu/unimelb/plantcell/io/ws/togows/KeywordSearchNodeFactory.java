package au.edu.unimelb.plantcell.io.ws.togows;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class KeywordSearchNodeFactory extends NodeFactory<KeywordSearchNodeModel> {

	@Override
	public KeywordSearchNodeModel createNodeModel() {
		return new KeywordSearchNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<KeywordSearchNodeModel> createNodeView(int viewIndex,
			KeywordSearchNodeModel nodeModel) {
		// node has no view so...
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new KeywordSearchNodeDialog();
	}

}
