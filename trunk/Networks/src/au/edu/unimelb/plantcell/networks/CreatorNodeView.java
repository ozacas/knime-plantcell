package au.edu.unimelb.plantcell.networks;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

import org.apache.commons.collections15.Transformer;
import org.knime.core.node.ExternalApplicationNodeView;

import au.edu.unimelb.plantcell.networks.cells.MyEdge;
import au.edu.unimelb.plantcell.networks.cells.MyVertex;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout2;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void onOpen(String title) {
		Graph<MyVertex, MyEdge> g = getNodeModel().getGraph();
		if (g == null)
			return;
		
		// layout algorithm gets edge distance from <code>edge.getDistance()</code>
		Layout<Integer, String> layout = new SpringLayout2(g, new Transformer<MyEdge,Integer>() {

			@Override
			public Integer transform(MyEdge e) {
				double d = e.getDistance();
				return new Integer((int) Math.round(d));
			}
			
		});
		
		Dimension sz = new Dimension(950,650);
        layout.setSize(sz); // sets the initial size of the space
      
        VisualizationViewer<Integer,String> vv = new VisualizationViewer<Integer,String>(layout);
        vv.setPreferredSize(sz);
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
        gm.setMode(ModalGraphMouse.Mode.TRANSFORMING);
        vv.setGraphMouse(gm);
        
        m_frame = new JFrame("Simple network view");
        m_frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(vv, BorderLayout.CENTER);
        JPanel instr_panel = new JPanel();
        instr_panel.add(new JLabel("Middle mouse to zoom, left-click to pan, right click to select context menu."));
        p.add(instr_panel, BorderLayout.SOUTH);
        m_frame.getContentPane().add(p);
        JMenuBar mb = new JMenuBar();
        JMenu menu = gm.getModeMenu();
        menu.setName("Mode");
        menu.setText(menu.getName());
        menu.setIcon(null);
        mb.add(menu);
        m_frame.setJMenuBar(mb);
        m_frame.pack();
        m_frame.setVisible(true);
	}

}

