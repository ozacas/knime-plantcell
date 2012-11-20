package au.edu.unimelb.plantcell.algorithms.StringFinder;

import org.knime.core.node.NodeDialogPane;

/**
 * <code>NodeFactory</code> for the "StringMatcher" Node.
 * Used to match a set of strings against a column in the input table. All input columns are preserved, but the matches are appended to the table.
 *
 * @author Andrew Cassin
 */
public class FindLocalNodeFactory 
        extends FindGlobalNodeFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public FindLocalNodeModel createNodeModel() {
        return new FindLocalNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new FindLocalNodeDialog();
    }

}

