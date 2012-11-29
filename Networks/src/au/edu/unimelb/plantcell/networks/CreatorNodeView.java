package au.edu.unimelb.plantcell.networks;

import java.awt.Dimension;

import javax.swing.JFrame;

import org.knime.core.node.ExternalApplicationNodeView;

import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;

/**
 * <code>NodeView</code> for the "Creator" Node.
 * Using JUNG, this node creates a network cell for the input data correlated above a chosen threshold for each target variable.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class CreatorNodeView extends ExternalApplicationNodeView<CreatorNodeModel> {
	private JFrame m_frame;
    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link CreatorNodeModel})
     */
    protected CreatorNodeView(final CreatorNodeModel nodeModel) {
        super(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        CreatorNodeModel nodeModel = 
            (CreatorNodeModel)getNodeModel();
        assert nodeModel != null;
        
        // be aware of a possibly not executed nodeModel! The data you retrieve
        // from your nodemodel could be null, emtpy, or invalid in any kind.
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    }

	@Override
	protected void onOpen(String title) {
		Layout<Integer, String> layout = new ISOMLayout(getNodeModel().getGraph());
		Dimension sz = new Dimension(950,650);
        layout.setSize(sz); // sets the initial size of the space
        // The BasicVisualizationServer<V,E> is parameterized by the edge types
        
        BasicVisualizationServer<Integer,String> vv =
        new BasicVisualizationServer<Integer,String>(layout);
        vv.setPreferredSize(sz); //Sets the viewing area size
        
        
        m_frame = new JFrame("Basic graph view");
        m_frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        m_frame.getContentPane().add(vv);
        m_frame.pack();
        m_frame.setVisible(true);
	}

}

