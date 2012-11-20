package au.edu.unimelb.plantcell.io.read.multiexcel;


import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "MultiExcelReader" Node.
 * Reads all Microsoft-Excel (2003 and earlier) *.xls documents in a folder and creates a unified table representing all rows from all sheets in all Excel files contained in this folder. Does not search subfolders.
 *
 * @author Andrew Cassin
 */
public class MultiExcelReaderNodeView extends NodeView<MultiExcelReaderNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link MultiExcelReaderNodeModel})
     */
    protected MultiExcelReaderNodeView(final MultiExcelReaderNodeModel nodeModel) {
        super(nodeModel);

        // TODO instantiate the components of the view here.

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {

        // TODO retrieve the new model from your nodemodel and 
        // update the view.
        MultiExcelReaderNodeModel nodeModel = 
            (MultiExcelReaderNodeModel)getNodeModel();
        assert nodeModel != null;
        
        // be aware of a possibly not executed nodeModel! The data you retrieve
        // from your nodemodel could be null, emtpy, or invalid in any kind.
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    
        // TODO things to do when closing the view
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {

        // TODO things to do when opening the view
    }

}

