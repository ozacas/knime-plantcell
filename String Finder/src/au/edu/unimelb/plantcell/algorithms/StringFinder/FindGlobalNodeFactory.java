package au.edu.unimelb.plantcell.algorithms.StringFinder;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "StringMatcher" Node.
 * Used to match a set of strings against a column in the input table. All input columns are preserved, but the matches are appended to the table.
 *
 * @author Andrew Cassin
 */
public class FindGlobalNodeFactory 
        extends NodeFactory<FindGlobalNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public FindGlobalNodeModel createNodeModel() {
        return new FindGlobalNodeModel();
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
    public NodeView<FindGlobalNodeModel> createNodeView(final int viewIndex,
            final FindGlobalNodeModel nodeModel) {
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
        return new FindGlobalNodeDialog();
    }

}

