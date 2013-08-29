package au.edu.unimelb.plantcell.proteomics.proteinselector;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "RepresentativeProteinSelector" Node.
 * Selects, amongst proteins which share peptides, a representative sequence. In the future, this will provide multiple strategies for doing this: but only one for now. Designed to match the results from the ACPFG String Matcher
 *
 * @author Andrew Cassin
 */
public class RepresentativeProteinSelectorNodeFactory 
        extends NodeFactory<RepresentativeProteinSelectorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public RepresentativeProteinSelectorNodeModel createNodeModel() {
        return new RepresentativeProteinSelectorNodeModel();
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
    public NodeView<RepresentativeProteinSelectorNodeModel> createNodeView(final int viewIndex,
            final RepresentativeProteinSelectorNodeModel nodeModel) {
        return new RepresentativeProteinSelectorNodeView(nodeModel);
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
        return new RepresentativeProteinSelectorNodeDialog();
    }

}

