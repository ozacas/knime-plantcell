package au.edu.unimelb.plantcell.io.ws.togows;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class EntryReaderNodeFactory extends NodeFactory<EntryReaderNodeModel> {

	@Override
	public EntryReaderNodeModel createNodeModel() {
		return new EntryReaderNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<EntryReaderNodeModel> createNodeView(int viewIndex,
			EntryReaderNodeModel nodeModel) {
		// node has no view so...
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new EntryReaderNodeDialog();
	}

}
