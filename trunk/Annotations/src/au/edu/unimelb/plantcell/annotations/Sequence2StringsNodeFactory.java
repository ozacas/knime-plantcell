package au.edu.unimelb.plantcell.annotations;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "RegionAnalyzer" Node.
 * Various nodes for analysis of sequence regions * 
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class Sequence2StringsNodeFactory extends NodeFactory<Sequence2StringsNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Sequence2StringsNodeModel createNodeModel() {
        return new Sequence2StringsNodeModel();
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
    public NodeView<Sequence2StringsNodeModel> createNodeView(final int viewIndex,
            final Sequence2StringsNodeModel nodeModel) {
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
        return new Sequence2StringsNodeDialog();
    }

}

