package au.edu.unimelb.plantcell.gp;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "GenePattern" Node.
 * Nodes to support remote invocation of a GenePattern instance for key analyses (heatmap, clustering etc.)
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class HeatmapNodeFactory 
        extends NodeFactory<HeatmapNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public HeatmapNodeModel createNodeModel() {
        return new HeatmapNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
	@Override
    public HeatmapNodeView<HeatmapNodeModel> createAbstractNodeView(final int viewIndex,
            final HeatmapNodeModel nodeModel) {
        return new HeatmapNodeView<HeatmapNodeModel>(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new HeatmapNodeDialog();
    }

	@Override
	public NodeView<HeatmapNodeModel> createNodeView(int viewIndex,
			HeatmapNodeModel nodeModel) {
		return null;
	}

}

