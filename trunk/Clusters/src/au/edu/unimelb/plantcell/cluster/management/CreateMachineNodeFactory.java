package au.edu.unimelb.plantcell.cluster.management;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "CreateMachine" Node.
 * Node which can create several machines and then run a chef wizard to initialise the nodes with a particular config.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class CreateMachineNodeFactory 
        extends NodeFactory<CreateMachineNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public CreateMachineNodeModel createNodeModel() {
        return new CreateMachineNodeModel();
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
    public NodeView<CreateMachineNodeModel> createNodeView(final int viewIndex,
            final CreateMachineNodeModel nodeModel) {
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
        return new CreateMachineNodeDialog();
    }

}

