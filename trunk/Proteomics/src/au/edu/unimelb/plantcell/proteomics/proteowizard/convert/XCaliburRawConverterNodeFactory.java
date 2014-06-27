package au.edu.unimelb.plantcell.proteomics.proteowizard.convert;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "XCaliburRawConverter" Node.
 * Converts XCalibur(tm) Raw files to open formats: mzML, mzXML or MGF using msconvert invoked via a SOAP webservice
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class XCaliburRawConverterNodeFactory 
        extends NodeFactory<XCaliburRawConverterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public XCaliburRawConverterNodeModel createNodeModel() {
        return new XCaliburRawConverterNodeModel();
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
    public NodeView<XCaliburRawConverterNodeModel> createNodeView(final int viewIndex,
            final XCaliburRawConverterNodeModel nodeModel) {
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
        return new XCaliburRawConverterNodeDialog();
    }

}

