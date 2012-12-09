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
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;
import org.knime.core.node.ExternalApplicationNodeView;

import au.edu.unimelb.plantcell.networks.cells.MyEdge;
import au.edu.unimelb.plantcell.networks.cells.MyVertex;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout2;
import edu.uci.ics.jung.algorithms.shortestpath.UnweightedShortestPath;
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
		Layout<MyVertex, MyEdge> layout = new SpringLayout2(g, new Transformer<MyEdge,Integer>() {

			@Override
			public Integer transform(MyEdge e) {
				double d = e.getDistance();
				return new Integer((int) Math.round(d));
			}
			
		});
		
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
				if (v instanceof MyVertex) {
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
        south_panel.add(new JScrollPane(txt_area));
        JPanel east_panel = new JPanel();
        east_panel.setLayout(new BoxLayout(east_panel, BoxLayout.Y_AXIS));
        JPanel east_kid1 = new JPanel();
        east_kid1.setLayout(new BoxLayout(east_kid1, BoxLayout.X_AXIS));
        
        final JCheckBox cb = new JCheckBox("Filter selected incl. within ");
        east_kid1.add(cb);
        final JSpinner sb_hop_count = new JSpinner(new SpinnerNumberModel(2,1,1000,1));
        final MyVertexFilter vertex_filter = new MyVertexFilter();
        sb_hop_count.getModel().addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				int hops = Integer.valueOf(sb_hop_count.getValue().toString());
				if (hops < 1)
					hops = 1;
				vertex_filter.setDistance(hops);
			}
        	
        });
        east_kid1.add(sb_hop_count);
        east_kid1.add(new JLabel("hops"));
		
        cb.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (cb.isSelected()) {
					vv.getRenderContext().setVertexIncludePredicate(vertex_filter);
				} else {
					vv.getRenderContext().setVertexIncludePredicate(new Predicate<Context<Graph<MyVertex, MyEdge>, MyVertex>>() {

						@Override
						public boolean evaluate(
								Context<Graph<MyVertex, MyEdge>, MyVertex> arg0) {
							return true;
						}
						
					});
				}
				vv.repaint();
			}
        	
        });
        east_panel.add(east_kid1);
        east_panel.add(new JCheckBox("Filter distance >= 0"));
        south_panel.add(east_panel);
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
				sb.append("Type: vertex\n");
				sb.append("Name: "+v.toString());
				sb.append("\n");
				if (v.getSampleVector() != null) {
					sb.append("Measurements (2 decimal places): "+v.getSampleVectorAsString());
					sb.append("\n");
				}
				for (Object key : v.getPropertyKeys()) {
					sb.append(key.toString()+": "+v.getProperty(key));
					sb.append("\n");
				}
				m_sel_display.setText(sb.toString());
			} else if (edge != null && edge instanceof MyEdge) {
				//Logger.getAnonymousLogger().info("edge "+edge.toString()+" is selected.");
				MyEdge e = (MyEdge) edge;
				StringBuilder sb = new StringBuilder();
				sb.append("Type: edge\n");
				sb.append("Name: "+e.toString());
				sb.append("\n");
				for (Object key : e.getPropertyKeys()) {
					sb.append(key.toString()+": "+e.getProperty(key));
					sb.append("\n");
				}
				m_sel_display.setText(sb.toString());
			}
			
			m_sel_display.repaint();
		}
		
	}
	


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
	 * HACK TODO BUGGY: does not handle zoom/pan etc. properly!
	 * 
	 * @author andrew.cassin
	 *
	 */
	public class TimecourseRenderer implements Vertex<MyVertex, MyEdge> {

		@Override
		public void paintVertex(RenderContext<MyVertex, MyEdge> rc,
				Layout<MyVertex, MyEdge> layout_mgr, MyVertex v) {
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
	
	public class MyVertexFilter implements Predicate<Context<Graph<MyVertex,MyEdge>, MyVertex>> {
		private Graph<MyVertex,MyEdge> m_g = null;
		private UnweightedShortestPath<MyVertex,MyEdge> m_shortest = null;
		private double max_hops = 2.0;
		
		@Override
		public boolean evaluate(
				Context<Graph<MyVertex, MyEdge>, MyVertex> c) {
			if (c == null || c.graph == null)
				return false;
			if (m_g != c.graph) {
				m_shortest = new UnweightedShortestPath<MyVertex, MyEdge>(c.graph);
				m_g = c.graph;
			}
			if (m_shortest == null || c.element == null)
				return false;
			
			for (MyVertex v : selected_vertices) {
				double d = (int) m_shortest.getDistance(c.element, v).doubleValue();
				if (d <= max_hops) 
					return true;
			} 
			return false;
		}

		public void setDistance(int hops) {
			assert(hops >= 1);
			max_hops = hops;
		}
		
	};
}

