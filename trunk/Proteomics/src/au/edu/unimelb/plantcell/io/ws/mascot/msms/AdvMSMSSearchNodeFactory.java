package au.edu.unimelb.plantcell.io.ws.mascot.msms;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the MS/MS Mascot Ion search Node.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AdvMSMSSearchNodeFactory extends NodeFactory<AdvMSMSSearchNodeModel> {

	@Override
	public AdvMSMSSearchNodeModel createNodeModel() {
		return new AdvMSMSSearchNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<AdvMSMSSearchNodeModel> createNodeView(int viewIndex, AdvMSMSSearchNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new AdvMSMSSearchNodeDialog();
	}

}