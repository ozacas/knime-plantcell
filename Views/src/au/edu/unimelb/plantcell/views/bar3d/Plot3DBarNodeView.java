package au.edu.unimelb.plantcell.views.bar3d;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Logger;

import javax.media.opengl.GLCapabilities;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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

import org.jzy3d.chart.Chart;
import org.jzy3d.colors.Color;
import org.jzy3d.global.Settings;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Statistics;
import org.jzy3d.plot3d.primitives.CompositeParallelepiped;
import org.jzy3d.plot3d.primitives.HistogramBar;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.Sphere;
import org.jzy3d.plot3d.primitives.textured.TranslucentQuad;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.scene.Graph;
import org.jzy3d.plot3d.rendering.scene.Scene;
import org.jzy3d.plot3d.rendering.view.modes.CameraMode;
import org.jzy3d.ui.views.ImagePanel;
import org.knime.core.node.ExternalApplicationNodeView;

/**
 * <code>NodeView</code> for the "Plot3DBar" Node.
 * Using jzy3d, this node produces a 3d bar plot using the supplied input columns.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class Plot3DBarNodeView extends ExternalApplicationNodeView<Plot3DBarNodeModel> {
	private Chart c;
	private JFrame f;
	private boolean wireframe = false;		// must match default for show wireframe checkbox
	private float bar_radius = 0.0125f;
	private float transparency = 1.0f;		// by default: no transparency
	private final JLabel status = new JLabel();
	private final JComboBox<String> bar_type = new JComboBox<String>(new String[] { "Cylinder", "Square Box", "Sphere" });
	private final Logger logger = Logger.getLogger("Plot 3D View");
	private boolean supports_transparency = false;
	
    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link Plot3DBarNodeModel})
     */
    protected Plot3DBarNodeView(final Plot3DBarNodeModel nodeModel) {
        super(nodeModel);
    
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
        c = new Chart(q, "swing");
        c.addController(new MyCameraMouseController());
        if (glc != null) {
        	logger.info("View has "+glc.getDepthBits()+" bits per pixel for depth buffer.");
        	if (glc.getHardwareAccelerated())
        		logger.info("View is hardware accelerated.");
        }
        
        JComponent chart = (JComponent) c.getCanvas();
        chart.setMinimumSize(new Dimension(300,300));
        chart.setPreferredSize(new Dimension(600,600));
        
        f = new JFrame("3D Bar chart");
        f.setLayout(new BorderLayout());
        f.setMinimumSize(new Dimension(300,300));
        f.setPreferredSize(new Dimension(800,800));
        f.add(chart, BorderLayout.CENTER);
        
        JPanel data_panel = new JPanel();
        data_panel.setLayout(new BoxLayout(data_panel, BoxLayout.Y_AXIS));
        data_panel.add(status);
        
        f.add(data_panel, BorderLayout.SOUTH);
        
        JPanel button_panel = new JPanel();
        button_panel.setLayout(new BoxLayout(button_panel, BoxLayout.Y_AXIS));
        JButton ss = new JButton("Screenshot...");
        button_panel.add(Box.createRigidArea(new Dimension(5,5)));
        button_panel.add(ss);
        JButton hilight = new JButton("Highlight...");
        button_panel.add(Box.createRigidArea(new Dimension(5,5)));
        button_panel.add(hilight);
        JButton legend = new JButton("Legend...");
        button_panel.add(Box.createRigidArea(new Dimension(5,5)));
        button_panel.add(legend);
        final JPanel image_panel = new JPanel();
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
				c.render();
			}
        	
        });
        show_box.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				c.getView().setAxeBoxDisplayed(show_box.isSelected());
				c.render();
			}
        	
        });
        final JCheckBox perspective_view = new JCheckBox("Perspective projection?");
        perspective_view.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				c.getView().setCameraMode(perspective_view.isSelected() ? CameraMode.PERSPECTIVE : CameraMode.ORTHOGONAL);
				c.render();
			}
        	
        });
        button_panel.add(show_box);
        //button_panel.add(perspective_view);
        button_panel.add(show_wireframe);
        button_panel.add(Box.createRigidArea(new Dimension(5,5)));
        
        bar_type.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				modelChanged();
				c.render();
			}
        	
        });
        button_panel.add(bar_type);
        button_panel.add(Box.createRigidArea(new Dimension(5,5)));
        
        final JSlider bar_thickness = new JSlider(1, 100, 50);
        bar_thickness.setBorder(BorderFactory.createTitledBorder("Scale"));
        bar_thickness.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				int val = bar_thickness.getValue();
				if (val <= 50) {
					bar_radius = 0.0125f * ((float)val) / 50;
				} else {
					bar_radius = 0.0125f * (val - 50);
				}
				
				modelChanged();
				c.render();
			}
        	
        });
        button_panel.add(bar_thickness);
        button_panel.add(Box.createRigidArea(new Dimension(5,5)));
        final JSlider bar_transparency = new JSlider(1,100,100);
        bar_transparency.setEnabled(supports_transparency);
        bar_transparency.setBorder(BorderFactory.createTitledBorder("Transparency (default: opaque)"));
        bar_transparency.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				int      val = bar_transparency.getValue();
				transparency = (((float)val) / 100.0f);
				
				modelChanged();
				c.render();
			}
        	
        });
        button_panel.add(bar_transparency);
        button_panel.add(Box.createVerticalGlue());
        
        JSplitPane split_pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        split_pane.setBottomComponent(new JScrollPane(image_panel));
        split_pane.setTopComponent(button_panel);
        f.add(split_pane, BorderLayout.EAST);
        
        // label the axes from the user-chosen columns
        c.getAxeLayout().setXAxeLabel(nodeModel.getAxis("X"));
        c.getAxeLayout().setYAxeLabel(nodeModel.getAxis("Y"));
        c.getAxeLayout().setZAxeLabel(nodeModel.getAxis("Z"));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        Plot3DBarNodeModel nodeModel = (Plot3DBarNodeModel)getNodeModel();
        assert nodeModel != null;
        
        boolean is_sphere = ((String)bar_type.getSelectedItem()).equals("Sphere");
        boolean is_parallelepiped = ((String)bar_type.getSelectedItem()).equals("Square Box");
        boolean has_overlay = nodeModel.hasOverlay();
        if (nodeModel != null && nodeModel.hasDataPoints()) {
        	double[] x = new double[nodeModel.countDataPoints()];
        	double[] y = new double[nodeModel.countDataPoints()];
        	double[] z = new double[nodeModel.countDataPoints()];
        	double[] overlay = null;
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
        	nodeModel.getDataPoints(x, y, z, colours);
        	
        	double x_min = Statistics.min(x);
        	double x_max = Statistics.max(x);
        	double y_min = Statistics.min(y);
        	double y_max = Statistics.max(y);
        	double z_min = Statistics.min(z);
        	double z_max = Statistics.max(z);
        	
        	double x_range = range(x_min, x_max);
        	double y_range = range(y_min, y_max);
        	double z_range = range(z_min, z_max);
        	
        	for (int i=0; i<x.length; i++) {
        		x[i] = (x[i] - x_min) / x_range;
        		y[i] = (y[i] - y_min) / y_range;
        		z[i] = (z[i] - z_min) / z_range;
        	}
        	
        	c.getAxeLayout().setXTickRenderer(new MyAxisRenderer(x_min, x_max));
        	c.getAxeLayout().setYTickRenderer(new MyAxisRenderer(y_min, y_max));
        	c.getAxeLayout().setZTickRenderer(new MyAxisRenderer(z_min, z_max));
        	
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
	        	for (int i=0; i<x.length; i++) {
	        		if (!is_sphere && !is_parallelepiped) {
	        			HistogramBar hb = new MyHistogramBar((float)x[i], (float) y[i], (float) z[i], bar_radius, colours[i], wireframe, i);
	        		
	        			g.add(hb);
	        		} else if (is_parallelepiped) { 
	        			bb = new BoundingBox3d((float) x[i] - bar_radius, (float) x[i] + bar_radius,
	        									(float) y[i] - bar_radius, (float) y[i] + bar_radius,
	        									0, (float) z[i]);
	        			CompositeParallelepiped box = new CompositeParallelepiped(bb);
	        			box.setColor(colours[i]);
	        			box.setWireframeDisplayed(wireframe);
	        			g.add(box);
	        		} else {
	        			Sphere s = new MySelectableSphere(new Coord3d(x[i], y[i], z[i]), bar_radius, 10, colours[i]);
	        			s.setWireframeDisplayed(wireframe);
	        			g.add(s);
	        		}
	        	}
	        	
	        	// render the overlay (if any)
	        	if (overlay != null) {
	        		String axis = nodeModel.getOverlayAxis().toLowerCase();
	        		Color gray = new Color(200,200,200);
	        		
	        		addBars(g, overlay, bar_radius, x, overlay_max, 0.0, gray, axis);
	        	}
        	}
        	
        	c.getScene().setGraph(g);
        }
        
    }


	private void addBars(Graph g, double[] overlay_vec, float bar_radius2, double[] vec, double max, double d, Color gray, String axis) {
		for (int i=0; i<overlay_vec.length; i++) {
			if (!Double.isNaN(overlay_vec[i])) {
				TranslucentQuad p = new TranslucentQuad();
				if (axis.equals("y")) {
					p.add(new Point(new Coord3d(vec[i] - bar_radius, 0.0, 0.0), gray));
					p.add(new Point(new Coord3d(vec[i] + bar_radius, 0.0, 0.0), gray));
					p.add(new Point(new Coord3d(vec[i] + bar_radius, (overlay_vec[i] / max), 0.0), gray));
					p.add(new Point(new Coord3d(vec[i] - bar_radius, (overlay_vec[i] / max), 0.0), gray));
				} else if (axis.equals("x")) {
					p.add(new Point(new Coord3d(0.0, vec[i] - bar_radius, 0.0), gray));
					p.add(new Point(new Coord3d(0.0, vec[i] + bar_radius, 0.0), gray));
					p.add(new Point(new Coord3d((overlay_vec[i] / max), vec[i] + bar_radius, 0.0), gray));
					p.add(new Point(new Coord3d((overlay_vec[i] / max), vec[i] - bar_radius, 0.0), gray));
				} else if (axis.equals("z")) {
					p.add(new Point(new Coord3d(vec[i] - bar_radius, 0.0, 0.0), gray));
					p.add(new Point(new Coord3d(vec[i] + bar_radius, 0.0, 0.0), gray));
					p.add(new Point(new Coord3d(vec[i] + bar_radius, 0.0, (overlay_vec[i] / max)), gray));
					p.add(new Point(new Coord3d(vec[i] - bar_radius, 0.0, (overlay_vec[i] / max)), gray));
				}
				p.setAlphaFactor(0.5f * transparency);
				g.add(p);
			}
		}
	}

	private double range(double x_min, double x_max) {
    	if ((x_min < 0.0 && x_max < 0.0) || (x_max >= 0.0 && x_min >= 0.0)) {
    		return Math.abs(x_max) - Math.abs(x_min);
    	} else {
    		return Math.abs(x_min) + Math.abs(x_max);
    	}
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    	c.clear();
    	c.dispose();
    }

	@Override
	protected void onOpen(String arg0) {
		if (c != null) {
			status.setText("Please wait... now loading data");
			modelChanged(); 		// compute the graph
			c.render();
			f.pack();
			f.setVisible(true);
			status.setText("Please click on a bar to see its data, in the table below.");
		}
	}

}

