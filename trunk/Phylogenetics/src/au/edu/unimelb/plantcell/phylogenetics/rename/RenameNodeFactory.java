package au.edu.unimelb.plantcell.phylogenetics.rename;

import org.knime.base.node.preproc.rename.RenameNodeModel;
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

import au.edu.unimelb.plantcell.io.read.phyloxml.FileTreeViewInterface;
import au.edu.unimelb.plantcell.io.read.phyloxml.PhylogeneticTreeView;

/**
 * <code>NodeFactory</code> for the "PhylogenyProcessor" Node.
 * Using the PAL library, as exported from MUSCLE node, this tree takes input data and performs tree construction, bootstrapping and other phylogenetic analyses as configured by the user.
 *
 * @author Andrew Cassin
 */
public class RenameNodeFactory extends NodeFactory<NodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public RenameNodeModel createNodeModel() {
        return new RenameNodeModel();
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
    public AbstractNodeView<NodeModel> createAbstractNodeView(final int viewIndex, final NodeModel mdl) {
    	return new PhylogeneticTreeView(mdl, ((FileTreeViewInterface)mdl).getTreeFileForDisplay());
    }
    
    /**
     * We dont use this method, since running Archaeopteryx is an external view application so...
     */
	@Override
	public NodeView<NodeModel> createNodeView(int viewIndex, NodeModel nodeModel) {
		return null;
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
        return new RenameNodeDialog();
    }

}

