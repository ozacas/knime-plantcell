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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;
import org.knime.core.data.RowKey;
import org.knime.core.node.ExternalApplicationNodeView;

import au.edu.unimelb.plantcell.networks.cells.MyEdge;
import au.edu.unimelb.plantcell.networks.cells.MyVertex;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout2;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
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
	private final static String NO_VERTICES_SELECTED = "No vertices selected.";
	
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
		final CreatorNodeModel my_model = getNodeModel();
		Layout<MyVertex, MyEdge> layout = new CircleLayout(g);
		Dimension sz = new Dimension(950,650);
        layout.setSize(sz); // sets the initial size of the space
      
        final VisualizationViewer<MyVertex,MyEdge> vv = new VisualizationViewer<MyVertex,MyEdge>(layout);
        vv.setPreferredSize(sz);
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        final DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
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
        
        if (getNodeModel().showTimecourseA() || getNodeModel().showTimecourseB()) {
        	vv.getRenderer().setVertexRenderer(new TimecourseRenderer());
        }
        final JLabel sel_vertices_label = new JLabel(NO_VERTICES_SELECTED);
        gm.add(new MyPickingPlugin(txt_area, sel_vertices_label));
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
        
        final MyFilterRuleModel rule_model = new MyFilterRuleModel();
        final JCheckBox freezer = new JCheckBox("Lock current nodes and edges");
        freezer.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				boolean is_checked = freezer.isSelected();
				setViewLocked(is_checked);
				
				// if the filter rules have been changed whilst frozen, we must update the current predicate and repaint...
				if (! is_checked) {
					vv.getRenderContext().setVertexIncludePredicate(rule_model.getVertexFilter());
					vv.getRenderContext().setEdgeIncludePredicate(rule_model.getEdgeFilter());
					vv.repaint();
				}
			}
        	
        });
     
        east_panel.setLayout(new BoxLayout(east_panel, BoxLayout.X_AXIS));
        JPanel mode_panel = new JPanel();
        mode_panel.setLayout(new BoxLayout(mode_panel, BoxLayout.Y_AXIS));
        east_panel.add(mode_panel);
        JPanel list_panel = new JPanel();
        list_panel.setLayout(new BorderLayout());
        final FilterRuleList fr_list = new FilterRuleList(rule_model);
        list_panel.add(new JLabel("Current node/edge filter rules:"), BorderLayout.NORTH);
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
					rule_model.addElement(new MyDistancePredicate(selected_vertices, new Integer(ret)));
					fr_list.invalidate();
				}
			}
        	
        });
        button_panel.add(distance_button);
        JButton node_button = new JButton("Add node filter");
        node_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Set<String> prop_names = new HashSet<String>();
				for (MyVertex v : g.getVertices()) {
					for (Object o : v.getPropertyKeys()) {
						String k = o.toString();
						if (!prop_names.contains(k) && !k.startsWith("__")) {
							prop_names.add(k);
						}
					}
				}
				NodeFilterDialog nfd = new NodeFilterDialog(prop_names, rule_model);
				nfd.setVisible(true);
			}
        	
        });
        button_panel.add(node_button);
        JButton edge_button = new JButton("Add edge filter");
        edge_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Collection<MyEdge> edges = g.getEdges();
				Set<String> prop_names = new HashSet<String>();
				for (MyEdge e : edges) {
					for (Object o : e.getPropertyKeys()) {
						String k = o.toString();
						if (!prop_names.contains(k) && !k.startsWith("__")) {
							prop_names.add(k);
						}
					}
				}
				EdgeFilterDialog efd = new EdgeFilterDialog(prop_names, rule_model);
				efd.setVisible(true);
			}
        	
        });
        button_panel.add(edge_button);
        JButton up = new JButton("Up");
        up.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				int idx = fr_list.getSelectedIndex();
				if (idx > 0) {	// first element cannot be moved higher!
					Object o = rule_model.remove(idx);
					rule_model.insertElementAt(o, idx-1);
					fr_list.setSelectedIndex(idx-1);
				}
			}
        	
        });
        button_panel.add(up);
        JButton down = new JButton("Down");
        down.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int idx = fr_list.getSelectedIndex();
				if (idx >= 0 && idx < rule_model.size()-1)	{ 	// last entry cant be moved
					Object o = rule_model.remove(idx);
					rule_model.insertElementAt(o, idx+1);
					fr_list.setSelectedIndex(idx+1);
				}
			}
        	
        });
        button_panel.add(down);
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
     
        JButton hilite_button = new JButton("HiLite current");
        hilite_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ev) {
				HashSet<String> rowids = new HashSet<String>();
				Predicate<Context<Graph<MyVertex, MyEdge>, MyEdge>> eip = vv.getRenderContext().getEdgeIncludePredicate();
				
				for (MyEdge e : g.getEdges()) {
					if (eip != null && !eip.evaluate(Context.<Graph<MyVertex,MyEdge>,MyEdge>getInstance(g, e)))
						continue;
					
					if (!rowids.contains(e.getRowID())) {
						rowids.add(e.getRowID());
					}
				}
				
				Logger l = Logger.getLogger("Network view");
				l.info("Number of RowID's to highlight: "+rowids.size());
				my_model.getInHiLiteHandler(0).fireClearHiLiteEvent();
				HashSet<RowKey> ids = new HashSet<RowKey>();
				for (String id : rowids) {
					ids.add(new RowKey(id));
				}
				my_model.getInHiLiteHandler(0).fireHiLiteEvent(ids);
				l.info("Highlighted "+ids.size()+" rows.");
			}
        	
        });
        button_panel.add(hilite_button);
        
        rule_model.addListDataListener(new ListDataListener() {

        	public void redraw() {
        		//if (!isViewLocked()) {
        			vv.getRenderContext().setVertexIncludePredicate(rule_model.getVertexFilter());
        			vv.getRenderContext().setEdgeIncludePredicate(rule_model.getEdgeFilter());
            		
        			vv.repaint();
        		//}
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
       
        final JComboBox cb_layout = new JComboBox(new String[] { "Circle", "FR v2 (animated)", "ISOM", "KK (animated)", "Spring (animated)" });
        cb_layout.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ae) {
				String layout_mgr = cb_layout.getSelectedItem().toString().toLowerCase().trim();
				Layout<MyVertex, MyEdge> layout = null;
				if (layout_mgr.startsWith("isom")) {
					layout = new ISOMLayout(g);
				} else if (layout_mgr.startsWith("spring")) {
					layout = new SpringLayout2(g);
				} else if (layout_mgr.startsWith("fr")) {
					layout = new FRLayout2(g);
				} else if (layout_mgr.startsWith("kk")) {
					layout = new KKLayout(g);
				} else {
					layout = new CircleLayout(g);
				}
				vv.setGraphLayout(layout);
				vv.repaint();
			}
        	
        });
        final JToggleButton pzt = new JToggleButton("Pan/Zoom/Translate");
        final JToggleButton sm  = new JToggleButton("Select/Move");

        pzt.setSelected(true);  // must match initial state of graph's mouse mode
        sm.setSelected(false);	// must match initial state of graph's mouse mode
        pzt.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ae) {
		        gm.setMode(ModalGraphMouse.Mode.TRANSFORMING);
		        pzt.setSelected(true);
		        sm.setSelected(false);
			}
        	
        });
        sm.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				gm.setMode(ModalGraphMouse.Mode.PICKING);
				pzt.setSelected(false);
				sm.setSelected(true);
			}
        	
        });
        mode_panel.add(pzt);
        mode_panel.add(sm);
        mode_panel.add(Box.createRigidArea(new Dimension(10,10)));
        mode_panel.add(freezer);
        mode_panel.add(sel_vertices_label);
        mode_panel.add(Box.createRigidArea(new Dimension(10,10)));
        mode_panel.add(Box.createVerticalGlue());
        mode_panel.add(cb_layout);
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
		private final JLabel    m_status;
		
		public MyPickingPlugin(final JTextArea sel_display, final JLabel sel_status) {
			super();
			m_sel_display = sel_display;
			m_status      = sel_status;
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
					if (k.equals(MyVertex.TIMECOURSE_VECTOR_PROPERTY))
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
			
			if (selected_vertices.size() > 0) {
				String str = ""+selected_vertices.size()+" vertices selected";
				if (isViewLocked()) {
					str += " (locked)";
				}
				m_status.setText(str+".");
			} else {
				m_status.setText(NO_VERTICES_SELECTED);
			}
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
			if (!rc.getVertexIncludePredicate().
					evaluate(Context.<Graph<MyVertex,MyEdge>,MyVertex>getInstance(layout_mgr.getGraph(),v))) {
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
			Shape   s = new Rectangle(min_x, min_y, width, height);
			
			gd.setPaint(Color.BLACK);
			gd.fill(s);
			gd.setPaint(Color.GRAY);
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
				gd.setPaint(Color.ORANGE);
				gd.draw(s);
			}
			gd.setColor(Color.WHITE);
			gd.setFont(Font.decode("Sans 8"));
			gd.drawString(new Double(max).toString(), min_x+3, min_y + 7);
		}

	}

}

