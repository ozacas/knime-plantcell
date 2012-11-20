package au.edu.unimelb.plantcell.io.read.multiexcel;


import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "MultiExcelReader" Node.
 * Reads all Microsoft-Excel (2003 and earlier) *.xls documents in a folder and creates a unified table representing all rows from all sheets in all Excel files contained in this folder. Does not search subfolders.
 *
 * @author Andrew Cassin
 */
public class MultiExcelReaderNodeFactory 
        extends NodeFactory<MultiExcelReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public MultiExcelReaderNodeModel createNodeModel() {
        return new MultiExcelReaderNodeModel();
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
    public NodeView<MultiExcelReaderNodeModel> createNodeView(final int viewIndex,
            final MultiExcelReaderNodeModel nodeModel) {
        return new MultiExcelReaderNodeView(nodeModel);
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
        return new MultiExcelReaderNodeDialog();
    }

}

