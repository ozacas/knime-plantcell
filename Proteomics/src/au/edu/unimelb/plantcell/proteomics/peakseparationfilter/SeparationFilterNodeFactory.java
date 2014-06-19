package au.edu.unimelb.plantcell.proteomics.peakseparationfilter;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the peak separation filter node, which looks for particular
 * peaks a certain mass apart and either accepts or rejects the input rows based on the presence/abscence of these peaks
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class SeparationFilterNodeFactory 
        extends NodeFactory<SeparationFilterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SeparationFilterNodeModel createNodeModel() {
        return new SeparationFilterNodeModel();
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
    public NodeView<SeparationFilterNodeModel> createNodeView(final int viewIndex,
            final SeparationFilterNodeModel nodeModel) {
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
        return new SeparationFilterNodeDialog();
    }

}

