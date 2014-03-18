package au.edu.unimelb.plantcell.proteomics.itraq;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "ITraqAnalyzer" Node.
 * Given a (set of) proteomics runs, with identified peptides, proteins and iTRAQ quantitation values this nodes performs an analysis and provides normalised results for the user in easy-to-read format. Based on method published in the scientific literature.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ITraqAnalyzerNodeFactory 
        extends NodeFactory<ITraqAnalyzerNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ITraqAnalyzerNodeModel createNodeModel() {
        return new ITraqAnalyzerNodeModel();
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
    public NodeView<ITraqAnalyzerNodeModel> createNodeView(final int viewIndex,
            final ITraqAnalyzerNodeModel nodeModel) {
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
        return new ITraqAnalyzerNodeDialog();
    }

}

