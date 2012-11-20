package au.edu.unimelb.plantcell.gp;

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import au.edu.unimelb.plantcell.gp.SOMClusteringNodeModel;

public class SOMClusteringNodeFactory extends NodeFactory<SOMClusteringNodeModel> {

	@Override
	public SOMClusteringNodeModel createNodeModel() {
		return new SOMClusteringNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 2;
	}

	@Override
	public AbstractNodeView<SOMClusteringNodeModel> createAbstractNodeView(int viewIndex,
			SOMClusteringNodeModel nodeModel) {
		if (viewIndex == 0) {
			return new OutputFilesView<SOMClusteringNodeModel>(nodeModel);
		} else {
			return new SOMClusteringNodeView(nodeModel);
		}
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new SOMClusteringNodeDialog();
	}

	@Override
	public NodeView<SOMClusteringNodeModel> createNodeView(int viewIndex,
			SOMClusteringNodeModel nodeModel) {
		return (NodeView<SOMClusteringNodeModel>) createAbstractNodeView(viewIndex, nodeModel);
	}

}
