package au.edu.unimelb.plantcell.networks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;
import org.knime.core.node.ExternalApplicationNodeView;

import au.edu.unimelb.plantcell.networks.cells.MyEdge;
import au.edu.unimelb.plantcell.networks.cells.MyVertex;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout2;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.PickingGraphMousePlugin;
import edu.uci.ics.jung.visualization.decorators.GradientEdgePaintTransformer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.renderers.Renderer.Vertex;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;

/**
 * <code>NodeView</code> for the "Creator" Node.
 * Using JUNG, this node creates a network cell for the input data correlated above a chosen threshold for each target variable.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class CreatorNodeView extends ExternalApplicationNodeView<CreatorNodeModel> {


	private JFrame m_frame;
    final Set<MyVertex> selected_vertices = new HashSet<MyVertex>();
	private boolean m_locked;

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
		final Graph<MyVertex, MyEdge> g = getNodeModel().getGraph();
		if (g == null)
			return;
		
		// layout algorithm gets edge distance from <code>edge.getDistance()</code>
		Layout<MyVertex, MyEdge> layout = new ISOMLayout(g);
		
		Dimension sz = new Dimension(950,650);
        layout.setSize(sz); // sets the initial size of the space
      
        final VisualizationViewer<MyVertex,MyEdge> vv = new VisualizationViewer<MyVertex,MyEdge>(layout);
        vv.setPreferredSize(sz);
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
        gm.setMode(ModalGraphMouse.Mode.TRANSFORMING);
        vv.setGraphMouse(gm);
        final JTextArea txt_area = new JTextArea();
        vv.getRenderContext().setVertexFillPaintTransformer(new Transformer<MyVertex,Paint>() {

			@Override
			public Paint transform(MyVertex v) {
				return v.getColour();
			}
        	
        });
       
       
        vv.getRenderContext().setEdgeDrawPaintTransformer(new MyGradientTransformer(vv, getNodeModel().useEdgeGradient()));
       
        if (getNodeModel().showEdgeDistance()) {
        	vv.getRenderContext().setEdgeLabelTransformer(new Transformer<MyEdge,String>() {
        		
				@Override
				public String transform(MyEdge e) {
					NumberFormat nf = NumberFormat.getInstance();
					nf.setMaximumFractionDigits(2);
					return nf.format(e.getDistance());
				}
        		
        	});
        	vv.getRenderContext().setEdgeFontTransformer(new ConstantTransformer(new Font(Font.SANS_SERIF, Font.PLAIN, 9)));
        }
        
        if (getNodeModel().showTimecourse()) {
        	vv.getRenderer().setVertexRenderer(new TimecourseRenderer());
        }
        gm.add(new MyPickingPlugin(txt_area));
        final PickedState<MyVertex> picked_state = vv.getPickedVertexState();
        vv.getPickedVertexState().addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent ie) {
				Object v = ie.getItem();
				if (v instanceof MyVertex && !isViewLocked()) {
					MyVertex vert = (MyVertex) v;
					if (picked_state.isPicked(vert)) {
						selected_vertices.add(vert);
					} else {
						selected_vertices.remove(vert);
					}
				}
			}
        	
        });
               
        m_frame = new JFrame("Interactive view");
        m_frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        JPanel south_panel = new JPanel();
        south_panel.setLayout(new BoxLayout(south_panel, BoxLayout.X_AXIS));
        JPanel east_panel = new JPanel();
        JPanel options_panel = new JPanel();
        options_panel.setLayout(new BoxLayout(options_panel, BoxLayout.Y_AXIS));
        final JCheckBox freezer = new JCheckBox("Freeze displayed nodes and edges");
        freezer.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				setViewLocked(true);
			}
        	
        });
        options_panel.add(freezer);
        east_panel.add(options_panel);
        east_panel.setLayout(new BoxLayout(east_panel, BoxLayout.X_AXIS));
        JPanel list_panel = new JPanel();
        list_panel.setLayout(new BorderLayout());
        final MyFilterRuleModel rule_model = new MyFilterRuleModel();
        final FilterRuleList fr_list = new FilterRuleList(rule_model);
        list_panel.add(new JScrollPane(fr_list), BorderLayout.CENTER);
        east_panel.add(list_panel);
        JPanel button_panel = new JPanel();
        button_panel.setLayout(new BoxLayout(button_panel, BoxLayout.Y_AXIS));
        JButton distance_button = new JButton("Add distance filter");
        distance_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				String ret = (String) JOptionPane.showInputDialog(null, "Show nodes within ... hops of selected nodes", 
											"Add distance filter rule", JOptionPane.QUESTION_MESSAGE, null, 
											new String[] {"1", "2", "3", "4", "5", "10", "20" }, "2");
				if (ret != null) {
					rule_model.addElement(new MyDistancePredicate(selected_vertices));
					fr_list.invalidate();
				}
			}
        	
        });
        button_panel.add(distance_button);
        button_panel.add(new JButton("Add annotation filter"));
        button_panel.add(new JButton("Up"));
        button_panel.add(new JButton("Down"));
        JButton remove_button = new JButton("Remove");
        remove_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				int idx = fr_list.getSelectedIndex();
				if (idx >= 0) 
					rule_model.removeElementAt(idx);
			}
        	
        });
        button_panel.add(remove_button);
        east_panel.add(button_panel);
     
        rule_model.addListDataListener(new ListDataListener() {

        	public void redraw() {
        		if (!isViewLocked()) {
        			vv.getRenderContext().setVertexIncludePredicate(rule_model.getVertexFilter());
        			vv.getRenderContext().setEdgeIncludePredicate(rule_model.getEdgeFilter());
            		vv.repaint();
        		}
        	}
        	
			@Override
			public void contentsChanged(ListDataEvent arg0) {
				redraw();
			}

			@Override
			public void intervalAdded(ListDataEvent arg0) {
				redraw();
			}

			@Override
			public void intervalRemoved(ListDataEvent arg0) {
				redraw();
			}
        	
        });
       
        south_panel.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(txt_area), east_panel));

        JSplitPane split_pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new GraphZoomScrollPane(vv), south_panel);
        p.add(split_pane, BorderLayout.CENTER);
       
        txt_area.setMinimumSize(new Dimension(120,70));
        
        txt_area.setText("Select a node or edge.");
        m_frame.getContentPane().add(p);
        JMenuBar mb = new JMenuBar();
        JMenu menu = gm.getModeMenu();
        menu.setName("Mode");
        menu.setText(menu.getName());
        menu.setIcon(null);
        mb.add(menu);
        JMenu menu2 = new JMenu("Layout");
        JMenuItem item = new JMenuItem("ISOM");
        menu.setText("Edit/Select ");
        item.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Layout<MyVertex, MyEdge> layout = new ISOMLayout(g);
				vv.setGraphLayout(layout);
			}
        	
        });
        menu2.add(item);
        item = new JMenuItem("Spring");
        item.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Layout<MyVertex, MyEdge> layout = new SpringLayout2(g);
				vv.setGraphLayout(layout);
			}
        	
        });
        menu2.add(item);
        item = new JMenuItem("Circle");
        item.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Layout<MyVertex, MyEdge> layout = new CircleLayout(g);
				vv.setGraphLayout(layout);
			}
        	
        });
        menu2.add(item);
        mb.add(menu2);
        m_frame.setJMenuBar(mb);
        m_frame.pack();
        m_frame.setVisible(true);
	}

	protected void setViewLocked(boolean b) {
		m_locked = b;
	}

	public boolean isViewLocked() {
		return m_locked;
	};
	
	/**
	 * Displays information eg. annotations about the most recently selected edge or vertex
	 * @author andrew.cassin
	 *
	 * @param <V>
	 * @param <E>
	 */
	public class MyPickingPlugin<V,E> extends PickingGraphMousePlugin<V,E> implements MouseListener {
		private final JTextArea m_sel_display;
		
		public MyPickingPlugin(final JTextArea sel_display) {
			super();
			m_sel_display = sel_display;
		}
	

		/**
		 * For some reason, the JUNG superclass uses mousePressed() but NOT mouseClicked() for this event
		 */
		@Override
		public void mousePressed(MouseEvent ev) {
			super.mousePressed(ev);

			// anything selected?
			if (vertex != null && vertex instanceof MyVertex) {
				//Logger.getAnonymousLogger().info("vertex "+vertex.toString()+" is selected.");
				MyVertex v = (MyVertex) vertex;
				StringBuilder sb = new StringBuilder();
				sb.append("Vertex\n");
				sb.append("Name: "+v.toString());
				sb.append("\n");
				
				for (Object key : v.getPropertyKeys()) {
					String k = key.toString();
					if (k.equals("timecourse-vector"))
						sb.append("Measurements: "+v.getProperty(k));
					else
						sb.append(k+": "+v.getProperty(k));
					sb.append("\n");
				}
				m_sel_display.setText(sb.toString());
			} else if (edge != null && edge instanceof MyEdge) {
				//Logger.getAnonymousLogger().info("edge "+edge.toString()+" is selected.");
				MyEdge e = (MyEdge) edge;
				StringBuilder sb = new StringBuilder();
				sb.append("Edge\n");
				sb.append("Name: "+e.toString());
				sb.append("\n");
				for (Object key : e.getPropertyKeys()) {
					String k = key.toString();
					if (k.startsWith("__"))			// internal-use-only property key?
						continue;
					sb.append(k+": "+e.getProperty(key));
					sb.append("\n");
				}
				m_sel_display.setText(sb.toString());
			}
			
			m_sel_display.repaint();
		}
		
	}
	
	/**
	 * Paints edges as a nice colour gradient between the colours specified during construction. 
	 * @author andrew.cassin
	 *
	 * @param <V>	node class
	 * @param <E>	edge class
	 */
	public class MyGradientTransformer<V,E> extends GradientEdgePaintTransformer<V,E> {
		private boolean m_use_gradient = false;
		
		public MyGradientTransformer(VisualizationViewer<V,E> vv, boolean use_gradient) {
			this(Color.WHITE, Color.BLACK, vv);
			m_use_gradient = use_gradient;
		}
		
		public MyGradientTransformer(Color c1, Color c2,
				VisualizationViewer<V, E> vv) {
			super(c1, c2, vv);
		}
		
		@Override
		public Color getColor2(E edge) {
			if (! m_use_gradient)
				return super.getColor2(edge);
			MyEdge e = (MyEdge)edge;
			return e.getColour();
		}
	}
	

	/**
	 * Displays the timecourse as a mini "bar chart" over each node in the graph. Very ugly for >50 nodes...
	 *  
	 * @author andrew.cassin
	 *
	 */
	public class TimecourseRenderer implements Vertex<MyVertex, MyEdge> {

		@Override
		public void paintVertex(RenderContext<MyVertex, MyEdge> rc,
				Layout<MyVertex, MyEdge> layout_mgr, MyVertex v) {
			// display vertex in graph?
			if (!rc.getVertexIncludePredicate().evaluate(Context.<Graph<MyVertex,MyEdge>,MyVertex>getInstance(layout_mgr.getGraph(),v))) {
				return;
			}
			
			GraphicsDecorator gd = rc.getGraphicsContext();
			Point2D ctr = layout_mgr.transform(v);
			ctr = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, ctr);
			int n = v.getNumSamples();
			if (n < 1)
				n = 1;
			int width = 4 * n + 2;
			
			int min_x = (int)ctr.getX() - width/2;
			int min_y = (int)ctr.getY() - 10;
			int height= 20;
			Shape s = new Rectangle(min_x, min_y, width, height);
			
			gd.setPaint(Color.WHITE);
			gd.fill(s);
			gd.setPaint(Color.BLACK);
			gd.draw(s);
			
			// draw bars to reflect the magnitude (origin at 0) of each timecourse measurement
			double[] vector = v.getSampleVector();
			if (vector == null)
				return;
			double min = Double.MAX_VALUE;
			double max = -Double.MIN_VALUE;
			for (int i=0; i<vector.length; i++) {
				if (min > vector[i])
					min = vector[i];
				if (max < vector[i])
					max = vector[i];
			}
			for (int i=0; i<vector.length; i++) {
				int x = min_x + (i*4);
				int y = min_y;
				int bar_height = 1;
				if (max > 0.0) {
					bar_height = (int) ((vector[i] / max) * height);
				}
				s = new Rectangle(x+1, height+y-bar_height, 4, bar_height);
				gd.setPaint(Color.BLUE);
				gd.draw(s);
			}
		}

	}

	/**
	 * 
	 * @author andrew.cassin
	 *
	 */
	public class MyFilterRulePanel extends JPanel {
		
		/**
		 *  not used
		 */
		private static final long serialVersionUID = -6213059175312664176L;

		public MyFilterRulePanel() {
			super();
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			add(new JComboBox(new String[] {"include nodes where", "include edges where", "exclude nodes where", "exclude edges where "}));
			// this combo depends on whether node or edge is chosen
			add(new JComboBox(new String[] { "distance" }));
			add(new JComboBox(new String[] { "=", ">", ">=", "<", "<=", "!=", " contains " }));
			add(new JTextField("0"));
		}
	}

}

