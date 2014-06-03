package au.edu.unimelb.plantcell.proteomics.views;

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "MassSpecSurface" Node.
 * Presents a surface representing RT, m/z and heatmap (eg. identified MS2 spectra) using the sexy jzy3d library. Useful for QA and assessment of mzML-format datasets.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MassSpecSurfaceNodeFactory 
        extends NodeFactory<MassSpecSurfaceNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public MassSpecSurfaceNodeModel createNodeModel() {
        return new MassSpecSurfaceNodeModel();
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
    public AbstractNodeView<MassSpecSurfaceNodeModel> createAbstractNodeView(final int viewIndex, final MassSpecSurfaceNodeModel mdl) {
    	return new MassSpecSurfaceNodeView<MassSpecSurfaceNodeModel>(mdl);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<MassSpecSurfaceNodeModel> createNodeView(final int viewIndex,
            final MassSpecSurfaceNodeModel nodeModel) {
        return (NodeView<MassSpecSurfaceNodeModel>) createAbstractNodeView(viewIndex, nodeModel);
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
        return new MassSpecSurfaceNodeDialog();
    }

}

