package au.edu.unimelb.plantcell.views.bar3d;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.media.opengl.GLCapabilities;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jzy3d.chart.Chart;
import org.jzy3d.colors.Color;
import org.jzy3d.global.Settings;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Statistics;
import org.jzy3d.plot3d.primitives.CompositeParallelepiped;
import org.jzy3d.plot3d.primitives.HistogramBar;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.primitives.Sphere;
import org.jzy3d.plot3d.primitives.selectable.SelectableScatter;
import org.jzy3d.plot3d.primitives.textured.TranslucentQuad;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.scene.Graph;
import org.jzy3d.plot3d.rendering.scene.Scene;
import org.jzy3d.ui.views.ImagePanel;
import org.knime.core.node.ExternalApplicationNodeView;
import org.knime.core.node.NodeModel;

/**
 * <code>NodeView</code> for the "Plot3DBar" Node.
 * Using jzy3d, this node produces a 3d bar plot using the supplied input columns.
 * Here we derive from the {@link ExternalApplicationNodeView} since the normal KNIME view has
 * the wrong implementation for working with JOGL. The node factory must co-ordinate with this to correctly
 * instantiate this type. And it will probably break with KNIME v3 as the API is probably changing.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class Plot3DBarNodeView<T extends NodeModel> extends ExternalApplicationNodeView<T> {
	private Chart m_chart;
	private JFrame f;
	private boolean wireframe = false;		// must match default for show wireframe checkbox
	private float bar_radius = 0.0225f;
	private float transparency = 1.0f;		// by default: no transparency
	private final JLabel status = new JLabel();
	private final JComboBox<String> bar_type = new JComboBox<String>();
	private final Logger logger = Logger.getLogger("Plot 3D View");
	private boolean supports_transparency = false;
	private File m_last_folder = null;		// user convenience: last folder for screenshot save is remembered and re-used...
	private final JComboBox<String> z_transform= new JComboBox<String>(new String[] {
      		"Linear", "Log10", "Square-root", "Reciprocal"
    });
	
    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link Plot3DBarNodeModel})
     */
    protected Plot3DBarNodeView(final T nodeModel) {
        super(nodeModel);
        
        f = setup("3D Plot");
       
        addStatus(f);
      
        final JPanel image_panel = new JPanel();
        JPanel button_panel = addButtons(image_panel, true, true);
     
        JSplitPane split_pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        split_pane.setBottomComponent(new JScrollPane(image_panel));
        split_pane.setTopComponent(button_panel);
        f.add(split_pane, BorderLayout.EAST);
    }

    /**
     * responsible for adding user controls (including listeners) to the right side of the borderlayout in the main frame
     * 
     * @param image_panel
     * @param has_choice_of_symbol
     * @return
     */
    protected JPanel addButtons(final JPanel image_panel, boolean has_choice_of_symbol, boolean show_scale_slider) {
    	  JPanel button_panel = new JPanel();
          button_panel.setLayout(new BoxLayout(button_panel, BoxLayout.Y_AXIS));
          JButton ss = new JButton("Screenshot...");
          button_panel.add(Box.createRigidArea(new Dimension(5,5)));
          button_panel.add(ss);
          ss.addActionListener(new ActionListener() {

  			@Override
  			public void actionPerformed(ActionEvent arg0) {
  				JFileChooser fc = new JFileChooser(m_last_folder);
  				fc.setName("Please select an image file");
  				int result = fc.showSaveDialog(null);
  				if (result == JFileChooser.APPROVE_OPTION) {
  					File out = fc.getSelectedFile();
  					if (!out.isDirectory()) {
  						try {
  							out.delete();
  							ImageIO.write(getChart().screenshot(), "png", out);
  							m_last_folder = out.getParentFile();
  						} catch (IOException e) {
  							e.printStackTrace();
  						}
  					}
  				}
  			}
          	
          });
          JButton hilight = new JButton("Highlight...");
          button_panel.add(Box.createRigidArea(new Dimension(5,5)));
          button_panel.add(hilight);
          JButton legend = new JButton("Legend...");
          button_panel.add(Box.createRigidArea(new Dimension(5,5)));
          button_panel.add(legend);
        
          JPanel transform_panel = new JPanel();
          transform_panel.setLayout(new BoxLayout(transform_panel, BoxLayout.X_AXIS));
          transform_panel.add(new JLabel("Z Transform"));
         
          transform_panel.add(z_transform);
          z_transform.addActionListener(new ActionListener() {

  			@Override
  			public void actionPerformed(ActionEvent e) {
  				modelChanged();
  				getChart().render();
  			}
          	
          });
          legend.addActionListener(new ActionListener() {

  			@Override
  			public void actionPerformed(ActionEvent arg0) {
  				JFileChooser fc = new JFileChooser();
  				fc.setFileFilter(new FileFilter() {

  					@Override
  					public boolean accept(File f) {
  						if (f.isDirectory())
  							return true;
  						String name = f.getName().trim().toLowerCase();
  						if (name.endsWith(".gif") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))
  							return true;
  						
  						return false;
  					}

  					@Override
  					public String getDescription() {
  						return "Image files (PNG, GIF, JPEG)";
  					}
  					
  				});
  				int val = fc.showOpenDialog(null);
  				if (val == JFileChooser.APPROVE_OPTION) {
  					try {
  						File imgfile = fc.getSelectedFile();
  						ImagePanel ip = new ImagePanel(imgfile.getAbsolutePath());
  						image_panel.removeAll();
  						image_panel.setLayout(new BorderLayout());
  						image_panel.add(ip, BorderLayout.CENTER);
  						image_panel.repaint();
  					} catch (Exception e) {
  						e.printStackTrace();
  					}
  				}
  			}
          	
          });
          final JCheckBox show_wireframe = new JCheckBox("Show wireframe?");
          final JCheckBox show_box       = new JCheckBox("Show axis box?", true);
          show_wireframe.addActionListener(new ActionListener() {

  			@Override
  			public void actionPerformed(ActionEvent arg0) {
  				wireframe = show_wireframe.isSelected();
  				modelChanged();
  				getChart().render();
  			}
          	
          });
          show_box.addActionListener(new ActionListener() {

  			@Override
  			public void actionPerformed(ActionEvent arg0) {
  				getChart().getView().setAxeBoxDisplayed(show_box.isSelected());
  				getChart().render();
  			}
          	
          });
         
          button_panel.add(show_box);
          
          button_panel.add(show_wireframe);
          button_panel.add(Box.createRigidArea(new Dimension(5,5)));
          
          bar_type.addActionListener(new ActionListener() {

  			@Override
  			public void actionPerformed(ActionEvent arg0) {
  				modelChanged();
  				getChart().render();
  			}
          	
          });
          
          if (has_choice_of_symbol) {
	          JPanel drawing_panel = new JPanel();
	          drawing_panel.setLayout(new BoxLayout(drawing_panel, BoxLayout.X_AXIS));
	          drawing_panel.add(new JLabel("Show as "));
	          bar_type.setModel(getShowAsOptions());
	          drawing_panel.add(bar_type);
	          button_panel.add(drawing_panel);
          }
          button_panel.add(transform_panel);
          button_panel.add(Box.createRigidArea(new Dimension(5,5)));
          
          if (show_scale_slider) {
	          final JSlider bar_thickness = new JSlider(1, 100, 50);
	          bar_thickness.setBorder(BorderFactory.createTitledBorder("Thickness"));
	          bar_thickness.addChangeListener(new ChangeListener() {
	
	  			@Override
	  			public void stateChanged(ChangeEvent arg0) {
	  				int val = bar_thickness.getValue();
	  				if (val <= 50) {
	  					setRadius(0.0125f * ((float)val) / 50);
	  				} else {
	  					setRadius(0.0125f * (val - 50));
	  				}
	  				
	  				modelChanged();
	  				getChart().render();
	  			}
	          	
	          });
	          button_panel.add(bar_thickness);
	          button_panel.add(Box.createRigidArea(new Dimension(5,5)));
          }
          final JSlider bar_transparency = new JSlider(1,100,100);
          bar_transparency.setEnabled(supports_transparency);
          bar_transparency.setBorder(BorderFactory.createTitledBorder("Transparency (default: opaque)"));
          bar_transparency.addChangeListener(new ChangeListener() {

  			@Override
  			public void stateChanged(ChangeEvent arg0) {
  				int      val = bar_transparency.getValue();
  				setAlpha(((float)val) / 100.0f);
  				modelChanged();
  				getChart().render();
  			}
          	
          });
          button_panel.add(bar_transparency);
          button_panel.add(Box.createVerticalGlue());
          return button_panel;
	}

    protected String getShowAs() {
    	return bar_type.getSelectedItem().toString();
    }
    
    /**
     * You must pass true to the "show choice" during construction or this wont get called
     * @return a {@link ComboBoxModel} ready for the user to use
     */
    protected ComboBoxModel<String> getShowAsOptions() {
    	return new DefaultComboBoxModel<String>(new String[] { "Scatter (fastest)", "Cylinder", "Box", "Sphere"  });
	}

	/**
     * Transformation method to Z-axis (linear, log10, sqrt,...)?
     */
    public String getZTransform() {
    	return z_transform.getSelectedItem().toString();
    }
    
    /**
     * reponsible for adding the status bar (typically to the bottom) of the specified frame
     * @param f
     */
	protected void addStatus(JFrame f) {
    	 JPanel data_panel = new JPanel();
         data_panel.setLayout(new BoxLayout(data_panel, BoxLayout.Y_AXIS));
         data_panel.add(status);
         
         f.add(data_panel, BorderLayout.SOUTH);
	}

	/**
	 * draw in wireframe mode?
	 * @return
	 */
	protected boolean useWireframe() {
    	return wireframe;
    }
    
    protected void setWireframe(boolean use_wireframe) {
    	wireframe = use_wireframe;
    }
    
    protected Color getWireframeColour() {
    	return Color.GRAY;
    }
    
    /**
     * for bar plots, what is the radius of each bar in unit cube co-ordinates?
     * 
     * @return
     */
    protected double getRadius() {
    	return bar_radius;
    }
    
    protected void setRadius(double new_radius) {
    	bar_radius = (float) new_radius;
    }
    
    /**
     * what transparency value should be applied globally to elements in the scene?
     * @return
     */
    protected float getAlpha() {
    	return transparency;
    }
    
    protected void setAlpha(double new_alpha) {
    	transparency = (float) new_alpha;
    }
    
    protected Chart getChart() {
    	return m_chart;
    }
    
    protected void setChart(Chart c) {
    	assert(c != null);
    	m_chart = c;
    }
    
    protected JFrame getFrame() {
    	return f;
    }
    
    protected void setFrame(JFrame f) {
    	assert(f != null);
    	this.f = f;
    }
    
    /**
     * initialise the OpenGL/JZY3d canvas with the specified title and z-buffer/transparency settings
     * depending on available hardware. This should be customised to the requirements of the plot.
     * 
     * @param title
     * @return
     */
    protected JFrame setup(String title) {
    	// always use hardware if possible
        GLCapabilities glc = null;
        try {
        	Settings s = Settings.getInstance();
        	if (s != null) {
        		s.setHardwareAccelerated(true);
        		glc = s.getGLCapabilities();
        	}
        } catch (Exception e) {
        	e.printStackTrace();
        }
        Quality q = Quality.Intermediate;
        boolean no_depth_if_transparency = q.isDisableDepthBufferWhenAlpha();
        if (!no_depth_if_transparency) {
        	supports_transparency = true;
        	q.setAlphaActivated(true);
        	q.setDepthActivated(true);
        	logger.info("View has "+glc.getAlphaBits()+" bits per pixel for transparency.");
        } else {
        	logger.warning("Disabling transparency as your computer doesnt support both depth and transparency at the same time.");
        	supports_transparency = false;
        	q.setDepthActivated(true);
        	q.setAlphaActivated(false);
        }
        Chart c = new Chart(q, "swing");
        c.addController(new MyCameraMouseController());
        if (glc != null) {
        	logger.info("View has "+glc.getDepthBits()+" bits per pixel for depth buffer.");
        	if (glc.getHardwareAccelerated())
        		logger.info("View is hardware accelerated.");
        }
        setChart(c);
        
        JComponent canvas = (JComponent) getChart().getCanvas();
        canvas.setMinimumSize(new Dimension(300,300));
        canvas.setPreferredSize(new Dimension(600,600));
        
        JFrame f = new JFrame(title);
        f.setLayout(new BorderLayout());
        f.setMinimumSize(new Dimension(300,300));
        f.setPreferredSize(new Dimension(800,800));
        f.add(canvas, BorderLayout.CENTER);
        
        setFrame(f);
        return f;
    }
    
    /**
     * Subclasses are expected to override this with whatever drawing is required.
     */
    @Override
    protected void modelChanged() {
        Plot3DBarNodeModel nodeModel = (Plot3DBarNodeModel)getNodeModel();
        assert nodeModel != null;
        
       // long start = System.currentTimeMillis();
        String drawing_type = getShowAs();
        boolean is_sphere = drawing_type.equals("Sphere");
        boolean is_parallelepiped = drawing_type.equals("Box");
        boolean is_scatter = drawing_type.startsWith("Scatter");
        boolean has_overlay = nodeModel.hasOverlay();
        if (nodeModel != null && nodeModel.hasDataPoints()) {
        	float[] overlay = null;
        	double overlay_min = 0.0;
        	double overlay_range = 0.0;
        	double overlay_max = 0.0;
        	if (has_overlay) {
        		overlay = nodeModel.getOverlay1DVector();
        		overlay_min = Statistics.min(overlay);
        		overlay_max = Statistics.max(overlay);
        		overlay_range = range(overlay_min, overlay_max);
        		Logger.getLogger("Plot 3D View").info("Overlay min: "+overlay_min+" max="+overlay_max+" range="+overlay_range);
        	}
        	Color[] colours = new Color[nodeModel.countDataPoints()];
        	
        	FloatArrayList x = new FloatArrayList(nodeModel.countDataPoints());
        	FloatArrayList y = new FloatArrayList(nodeModel.countDataPoints());
        	FloatArrayList z = new FloatArrayList(nodeModel.countDataPoints());
        	nodeModel.getDataPoints(x, y, z, colours);
        	
        	SummaryStatistics x_stats = new SummaryStatistics();
        	SummaryStatistics y_stats = new SummaryStatistics();
        	SummaryStatistics z_stats = new SummaryStatistics();
        	nodeModel.getStatistics(x_stats, y_stats, z_stats);
        	
        	SummaryStatistics transformed_stats = transform(z, z_stats, getZTransform());
        	if (transformed_stats != null)
        		z_stats = transformed_stats;
        	
        	double x_range = range(x_stats.getMin(), x_stats.getMax());
        	double y_range = range(y_stats.getMin(), y_stats.getMax());
        	double z_range = range(z_stats.getMin(), z_stats.getMax());
        	
        	// convert each data point to [0,1] for the view
        	long n = x_stats.getN();
        	for (int i=0; i<n; i++) {
        		x.set(i, (float) ((x.get(i) - x_stats.getMin()) / x_range));
        		y.set(i, (float) ((y.get(i) - y_stats.getMin()) / y_range));
        		z.set(i, (float) ((z.get(i) - z_stats.getMin()) / z_range));
        	}
        	
        	Chart c = getChart();
        	c.getAxeLayout().setXTickRenderer(new MyAxisRenderer(x_stats.getMin(), x_stats.getMax()));
        	c.getAxeLayout().setYTickRenderer(new MyAxisRenderer(y_stats.getMin(), y_stats.getMax()));
        	c.getAxeLayout().setZTickRenderer(new MyAxisRenderer(z_stats.getMin(), z_stats.getMax()));
        	c.getAxeLayout().setXAxeLabel(nodeModel.getAxis("X"));
            c.getAxeLayout().setYAxeLabel(nodeModel.getAxis("Y"));
            c.getAxeLayout().setZAxeLabel(nodeModel.getAxis("Z"));
              
        	if (transparency < 1.0f) {
        		for (int i=0; i<colours.length; i++) {
        			colours[i] = colours[i].alphaSelf(transparency);
        		}
        	}
        	
        	// MUST create a new scene or we will deadlock java/OpenGL...
        	Graph g = new Graph(new Scene(), true);
        	BoundingBox3d bb = g.getBounds();
        	bb.setZmax(1.0f);
        	bb.setZmin(0.0f);
        	bb.setXmin(0.0f);
        	bb.setXmax(1.0f);
        	bb.setYmin(0.0f);
        	bb.setYmax(1.0f);
        	
        	if (!Double.isNaN(x_range)) {
        		// first the main plots
        		if (is_scatter) {
        			Coord3d[] points = new Coord3d[(int) n];
        			for (int i=0; i<n; i++) {
        				points[i] = new Coord3d(x.get(i), y.get(i), z.get(i));
        			}
        			
        			// jzy3d v0.9: selectablescatter doesn't provide a constructor with width parameter so...
        			Scatter s = new SelectableScatter(points, colours);
        			s.setWidth(200.0f * bar_radius);
        			g.add(s);
        		} else {
		        	for (int i=0; i<n; i++) {
		        		float xi = x.get(i);
		        		float yi = y.get(i);
		        		float zi = z.get(i);
		        		if (!is_sphere && !is_parallelepiped) {
		        			HistogramBar hb = new MyHistogramBar(xi, yi, zi, bar_radius, colours[i], wireframe, i);
		        		
		        			g.add(hb);
		        		} else if (is_parallelepiped) { 
		        			bb = new BoundingBox3d(xi - bar_radius, xi + bar_radius,
		        									yi - bar_radius, yi + bar_radius,
		        									0, (float) zi);
		        			CompositeParallelepiped box = new CompositeParallelepiped(bb);
		        			box.setColor(colours[i]);
		        			box.setWireframeDisplayed(wireframe);
		        			g.add(box);
		        		} else {
		        			Sphere s = new MySelectableSphere(new Coord3d(xi, yi, zi), bar_radius, 10, colours[i]);
		        			s.setWireframeDisplayed(wireframe);
		        			g.add(s);
		        		}
		        	}
        		}
        		
	        	// render the overlay (if any)
	        	if (overlay != null) {
	        		String axis = nodeModel.getOverlayAxis().toLowerCase();
	        		Color gray = new Color(200,200,200);
	        		
	        		addBars(g, overlay, bar_radius, x, overlay_max, 0.0, gray, axis);
	        	}
        	}
        	
        	// install the new graph as the current and redraw
        	c.getScene().setGraph(g);
        }
        
       // long end = System.currentTimeMillis();
       // logger.info("Took "+(end-start)+" milliseconds in modelChanged()");
    }

    /**
     * Perform a log10 transform
     * @param values requires only positive values
     * @param untransformed_stats
     * @param method one of Log10, Linear, Square-root or reciprocal
     * @return null if any unsuitable values are present in <code>values</code> for the chosen method
     */
    protected SummaryStatistics transform(final FloatArrayList values, final SummaryStatistics untransformed_stats, final String method) {
    	SummaryStatistics ret = new SummaryStatistics();
    	
    	if (method.startsWith("Linear"))
    		return untransformed_stats;
    	
    	if (method.equals("Log10")) {
    		if (untransformed_stats.getMin() < 0.0) 
        		return null;
	    	for (int i=0; i<values.size(); i++) {
	    		double val = Math.log10(values.get(i));
	    		ret.addValue(val);
	    		values.set(i, (float) val);
	    	}
    	} else if (method.startsWith("Square")) {
    		if (untransformed_stats.getMin() < 0.0)
    			return null;
    		for (int i=0; i<values.size(); i++) {
    			double val = Math.sqrt(values.get(i));
    			ret.addValue(val);
    			values.set(i, (float) val);
    		}
    	} else if (method.startsWith("Recip")) {
    		for (int i=0; i<values.size(); i++) {
    			double val = values.get(i);
    			if (val == 0.0) {
    				return null;
    			}
    			double recip = 1.0d/val;
    			ret.addValue(recip);
    			values.set(i, (float) recip);
    		}
    	} else {
    		Logger.getLogger("Plot3D Bar").warning("Unknown transformation: "+method+" ignored.");
    		return null;
    	}
    	
    	return ret;
    }
    
	private void addBars(Graph g, float[] overlay_vec, float bar_radius2, FloatArrayList vec, double max, double d, Color gray, String axis) {
		for (int i=0; i<overlay_vec.length; i++) {
			if (!Double.isNaN(overlay_vec[i])) {
				double ovi = overlay_vec[i];
				double vi = vec.get(i);
				TranslucentQuad p = new TranslucentQuad();
				if (axis.equals("y")) {
					p.add(new Point(new Coord3d(vi - bar_radius, 0.0, 0.0), gray));
					p.add(new Point(new Coord3d(vi + bar_radius, 0.0, 0.0), gray));
					p.add(new Point(new Coord3d(vi + bar_radius, (ovi / max), 0.0), gray));
					p.add(new Point(new Coord3d(vi - bar_radius, (ovi / max), 0.0), gray));
				} else if (axis.equals("x")) {
					p.add(new Point(new Coord3d(0.0, vi - bar_radius, 0.0), gray));
					p.add(new Point(new Coord3d(0.0, vi + bar_radius, 0.0), gray));
					p.add(new Point(new Coord3d((ovi / max), vi + bar_radius, 0.0), gray));
					p.add(new Point(new Coord3d((ovi / max), vi - bar_radius, 0.0), gray));
				} else if (axis.equals("z")) {
					p.add(new Point(new Coord3d(vi - bar_radius, 0.0, 0.0), gray));
					p.add(new Point(new Coord3d(vi + bar_radius, 0.0, 0.0), gray));
					p.add(new Point(new Coord3d(vi + bar_radius, 0.0, (ovi / max)), gray));
					p.add(new Point(new Coord3d(vi - bar_radius, 0.0, (ovi / max)), gray));
				}
				p.setAlphaFactor(0.5f * transparency);
				g.add(p);
			}
		}
	}

	protected double range(double x_min, double x_max) {
    	if ((x_min < 0.0 && x_max < 0.0) || (x_max >= 0.0 && x_min >= 0.0)) {
    		return Math.abs(x_max) - Math.abs(x_min);
    	} else {
    		return Math.abs(x_min) + Math.abs(x_max);
    	}
	}

	/**
	 * Update the status bar for the user.
	 * @param new_status if null, sets a default message. Otherwise the chosen message is presented to the user.
	 */
	protected void setStatus(String new_status) {
		if (new_status == null) {
			status.setText("Ready. Please click on a datapoint to see its data, below.");
			return;
		}
		status.setText(new_status);
	}
	
	/**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    	Chart c = getChart();
    	if (c == null)
    		return;
    	c.clear();
    	c.dispose();
    }
    
	@Override
	protected void onOpen(String arg0) {
		Chart c = getChart();
		JFrame f= getFrame();
		if (c != null) {
			setStatus("Please wait... now loading data");
			modelChanged(); 		// compute the graph
			c.render();
			f.pack();
			f.setVisible(true);
			setStatus(null);
		}
	}

}

