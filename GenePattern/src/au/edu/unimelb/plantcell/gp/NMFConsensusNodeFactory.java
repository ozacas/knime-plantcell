package au.edu.unimelb.plantcell.gp;

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class NMFConsensusNodeFactory extends NodeFactory<NMFConsensusNodeModel> {

	@Override
	public NMFConsensusNodeModel createNodeModel() {
		return new NMFConsensusNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 1;
	}

	@Override
	public AbstractNodeView<NMFConsensusNodeModel> createAbstractNodeView(int viewIndex,
			NMFConsensusNodeModel nodeModel) {
		return new OutputFilesView<NMFConsensusNodeModel>(nodeModel);
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new NMFConsensusNodeDialog();
	}

	@Override
	public NodeView<NMFConsensusNodeModel> createNodeView(int viewIndex,
			NMFConsensusNodeModel nodeModel) {
		return null;		// done by createAbstractNodeView() instead
	}

}
