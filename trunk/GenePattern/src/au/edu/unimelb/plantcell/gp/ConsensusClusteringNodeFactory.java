package au.edu.unimelb.plantcell.gp;

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class ConsensusClusteringNodeFactory extends NodeFactory<ConsensusClusteringNodeModel> {

	@Override
	public ConsensusClusteringNodeModel createNodeModel() {
		return new ConsensusClusteringNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 1;
	}

	@Override
	public AbstractNodeView<ConsensusClusteringNodeModel> createAbstractNodeView(int viewIndex,
			ConsensusClusteringNodeModel nodeModel) {
		if (viewIndex == 0) {
			return new OutputFilesView<ConsensusClusteringNodeModel>(nodeModel);
		} 
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new ConsensusClusteringNodeDialog();
	}

	@Override
	public NodeView<ConsensusClusteringNodeModel> createNodeView(int viewIndex,
			ConsensusClusteringNodeModel nodeModel) {
		return (NodeView<ConsensusClusteringNodeModel>) createAbstractNodeView(viewIndex, nodeModel);
	}

}
