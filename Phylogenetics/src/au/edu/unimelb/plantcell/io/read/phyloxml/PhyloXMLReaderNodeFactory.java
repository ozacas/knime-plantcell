package au.edu.unimelb.plantcell.io.read.phyloxml;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Creates a phyloxml document from the input data, decorated with data from the input table.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class PhyloXMLReaderNodeFactory
        extends NodeFactory<PhyloXMLReaderNodeModel> {

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
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<PhyloXMLReaderNodeModel> createNodeView(final int viewIndex,
            final PhyloXMLReaderNodeModel nodeModel) {
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
        return new PhyloXMLReaderNodeDialog();
    }

}

