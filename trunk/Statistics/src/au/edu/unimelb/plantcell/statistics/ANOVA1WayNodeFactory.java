package au.edu.unimelb.plantcell.statistics;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Ranker" Node.
 * Basic descriptive and inferential statistics support built using the apache commons math v3 library.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ANOVA1WayNodeFactory 
        extends NodeFactory<ANOVA1WayNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ANOVA1WayNodeModel createNodeModel() {
        return new ANOVA1WayNodeModel();
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
    public NodeView<ANOVA1WayNodeModel> createNodeView(final int viewIndex,
            final ANOVA1WayNodeModel nodeModel) {
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
        return new ANOVA1WayNodeDialog();
    }

}

