package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

import au.edu.unimelb.plantcell.io.read.phyloxml.FileTreeViewInterface;
import au.edu.unimelb.plantcell.io.read.phyloxml.PhylogeneticTreeView;

/**
 * applies heat to a tree using the forester library
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ApplyHeatNodeFactory 
        extends NodeFactory<NodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeModel createNodeModel() {
        return new ApplyHeatNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<NodeModel> createNodeView(final int viewIndex, final NodeModel nodeModel) {
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractNodeView<NodeModel> createAbstractNodeView(final int viewIndex, final NodeModel mdl) {
    	return new PhylogeneticTreeView(mdl, ((FileTreeViewInterface)mdl).getTreeFileForDisplay());
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
        return new ApplyHeatNodeDialog();
    }

}

