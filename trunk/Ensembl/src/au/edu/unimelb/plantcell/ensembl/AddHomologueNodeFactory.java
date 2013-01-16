package au.edu.unimelb.plantcell.ensembl;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "EnsembleAddHomologue" Node.
 * Adds homologues for the input data to the output table
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AddHomologueNodeFactory 
        extends NodeFactory<AddHomologueNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public AddHomologueNodeModel createNodeModel() {
        return new AddHomologueNodeModel();
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
    public NodeView<AddHomologueNodeModel> createNodeView(final int viewIndex,
            final AddHomologueNodeModel nodeModel) {
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
        return new AddHomologueNodeDialog();
    }

}

