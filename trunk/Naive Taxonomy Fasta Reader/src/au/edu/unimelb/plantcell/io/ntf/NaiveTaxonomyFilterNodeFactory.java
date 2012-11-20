package au.edu.unimelb.plantcell.io.ntf;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "NaiveTaxonomyFilter" Node.
 * Used to extract sequences from a FASTA file which match a given column of taxa. Regular expressions can be provided to match the taxa entry from the description in the FASTA file. Taxa desired form the input to the node.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class NaiveTaxonomyFilterNodeFactory 
        extends NodeFactory<NaiveTaxonomyFilterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public NaiveTaxonomyFilterNodeModel createNodeModel() {
        return new NaiveTaxonomyFilterNodeModel();
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
    public NodeView<NaiveTaxonomyFilterNodeModel> createNodeView(final int viewIndex,
            final NaiveTaxonomyFilterNodeModel nodeModel) {
        return new NaiveTaxonomyFilterNodeView(nodeModel);
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
        return new NaiveTaxonomyFilterNodeDialog();
    }

}

