package au.edu.unimelb.plantcell.io.ws.mascot.config;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Node to download mascot sequence databases
 * @author acassin
 *
 */
public class DownloadDatabaseNodeFactory extends NodeFactory<DownloadDatabaseNodeModel> {

	@Override
	public DownloadDatabaseNodeModel createNodeModel() {
		return new DownloadDatabaseNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<DownloadDatabaseNodeModel> createNodeView(int viewIndex, DownloadDatabaseNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new DownloadDatabaseNodeDialog();
	}

}
