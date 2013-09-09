package au.edu.unimelb.plantcell.io.write.phyloxml;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Creates a phyloxml document from the input data, decorated with data from the input table.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class PhyloXMLWriterNodeFactory 
        extends NodeFactory<PhyloXMLWriterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PhyloXMLWriterNodeModel createNodeModel() {
        return new PhyloXMLWriterNodeModel();
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
    public NodeView<PhyloXMLWriterNodeModel> createNodeView(final int viewIndex,
            final PhyloXMLWriterNodeModel nodeModel) {
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
        return new PhyloXMLWriterNodeDialog();
    }

}

