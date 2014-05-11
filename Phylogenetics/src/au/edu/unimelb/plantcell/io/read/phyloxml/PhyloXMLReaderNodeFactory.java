package au.edu.unimelb.plantcell.io.read.phyloxml;

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * Creates a phyloxml document from the input data, decorated with data from the input table.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class PhyloXMLReaderNodeFactory
        extends NodeFactory<NodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PhyloXMLReaderNodeModel createNodeModel() {
        return new PhyloXMLReaderNodeModel();
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
        return new PhyloXMLReaderNodeDialog();
    }

    /**
     * We dont use this method, since running Archaeopteryx is an external view application so...
     */
	@Override
	public NodeView<NodeModel> createNodeView(int viewIndex, NodeModel nodeModel) {
		return null;
	}

}

