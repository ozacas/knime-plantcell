package au.edu.unimelb.plantcell.statistics.venn;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "VennAnalyzer" Node.
 * Performs an n-way (3 or 4 recommended) venn analysis based over the values in chosen columns based on a group-by column.
 *
 * @author Andrew Cassin
 */
public class VennAnalyzerNodeFactory 
        extends NodeFactory<VennAnalyzerNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public VennAnalyzerNodeModel createNodeModel() {
        return new VennAnalyzerNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<VennAnalyzerNodeModel> createNodeView(final int viewIndex,
            final VennAnalyzerNodeModel nodeModel) {
    	try {
    		return new VennAnalyzerNodeView(nodeModel);
    	} catch (Exception e) {
    		// probably NoClassDef... since KNIME SVG is not installed
    		e.printStackTrace();
    		return null;
    	}
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
        return new VennAnalyzerNodeDialog();
    }

}

