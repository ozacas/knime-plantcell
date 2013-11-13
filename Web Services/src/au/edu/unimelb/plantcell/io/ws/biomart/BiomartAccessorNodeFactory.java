package au.edu.unimelb.plantcell.io.ws.biomart;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class BiomartAccessorNodeFactory extends NodeFactory<BiomartAccessorNodeModel> {

	@Override
	public BiomartAccessorNodeModel createNodeModel() {
		return new BiomartAccessorNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<BiomartAccessorNodeModel> createNodeView(int viewIndex,
			BiomartAccessorNodeModel nodeModel) {
		// node has no view so...
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new BiomartAccessorNodeDialog();
	}

}
