package au.edu.unimelb.plantcell.views.ms;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.media.opengl.GLCapabilities;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.jzy3d.chart.Chart;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.primitives.AbstractDrawable;
import org.jzy3d.plot3d.primitives.CompileableComposite;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.scene.Graph;
import org.jzy3d.plot3d.rendering.scene.Scene;
import org.knime.core.node.NodeModel;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.functor.MatrixFunction;
import org.la4j.matrix.functor.MatrixProcedure;

import au.edu.unimelb.plantcell.views.plot3d.MyAxisRenderer;
import au.edu.unimelb.plantcell.views.plot3d.Plot3DBarNodeView;

/**
 * Implements a plot of MS1 peaks against retention time (summing intensity into bins) over a number of runs 
 * - see the NodeModel for details.
 * 
 * @author andrew.cassin
 *
 * @param <T> must extend from the MassSpecSurfaceNodeModel
 */
public class MassSpecSurfaceNodeView<T extends NodeModel> extends Plot3DBarNodeView<T> {

	protected MassSpecSurfaceNodeView(T nodeModel) {
		super(nodeModel);
		JFrame f = setupOpenGL("MS1 versus RT versus intensity surface");

        addStatus(f);
      
        final JPanel image_panel = new JPanel();
        final JPanel button_panel= addButtons(image_panel, true, false);
     
        JSplitPane split_pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        split_pane.setBottomComponent(new JScrollPane(image_panel));
        split_pane.setTopComponent(button_panel);
        f.add(split_pane, BorderLayout.EAST);
	}
	
	@Override
	protected void modelChanged() {
		MassSpecSurfaceNodeModel nodeModel = (MassSpecSurfaceNodeModel)getNodeModel();
		if (nodeModel == null) {
			setStatus("Please execute the node to display the surface.");
			getChart().clear();
			return;
		}
        
        Matrix m = nodeModel.getSurface();
        if (m == null || m.rows() < 1 || m.columns() < 1) {
        	setStatus("No surface to display, please execute the node - reconfigure?");
        	getChart().clear();
        	return;
        }
        Range x_range = new Range(0.0, 1.0);
        Range y_range = new Range(0.0, 1.0);
        
        final Matrix matrix = transform(m, this.getZTransform());
        final double z_max   = getMaximum(matrix);		// NB: AFTER transform!
        final double z_min   = getMinimum(matrix);		// NB: AFTER transform!
        final double z_range = range(z_min, z_max);
        final int xdim = matrix.rows()-1;
        final int ydim = matrix.columns()-1;
        
        Chart c = getChart();
        Graph g = new Graph(new Scene(), true);
        BoundingBox3d bb = g.getBounds();
    	bb.setZmax(1.0f);
    	bb.setZmin(0.0f);
    	bb.setXmin(0.0f);
    	bb.setXmax(1.0f);
    	bb.setYmin(0.0f);
    	bb.setYmax(1.0f);
    	c.getAxeLayout().setYAxeLabel("Retention Time");
    	c.getAxeLayout().setXAxeLabel("M/Z");
    	c.getAxeLayout().setZAxeLabel("Intensity (sum)");
    	c.getAxeLayout().setXTickRenderer(new MyAxisRenderer(nodeModel.getMZmin(), nodeModel.getMZmax()));
    	c.getAxeLayout().setYTickRenderer(new MyAxisRenderer(nodeModel.getRTmin(), nodeModel.getRTmax()));
    	c.getAxeLayout().setZTickRenderer(new MyAxisRenderer(z_min, z_max));
        
        // since the practical screen resolution is of the order of 1k pixels, we sample based on the matrix dimension to reduce memory usage
        String show_as = getShowAs().toLowerCase().trim();
        
        if (show_as.startsWith("surface")) {
	        CompileableComposite surface = nodeModel.getOpenGLSurface(matrix, x_range, y_range, z_min, z_range);
	        surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), 0.0f, 1.0f, new Color(1,1,1,getAlpha())));
	       	
	        g.add(surface);
        } else {	// must be scatter for now...
	        Scatter s = new Scatter();
	       
	        final ArrayList<Coord3d> points = new ArrayList<Coord3d>();
	        
	        matrix.each(new MatrixProcedure() {

				@Override
				public void apply(int r, int c, double val) {
					if (val > 0.0) {
						points.add(new Coord3d(((float)r)/xdim, ((float)c)/ydim, (float) (val - z_min) / z_range));
					}
				}
	        	
	        });
	       
	        s.setData(points.toArray(new Coord3d[0]));
	        g.add(s);
        }
        
        // finally add the MS2 heatmap
        g.add(getMS2Scatter(nodeModel.getMS2Surface(), matrix, xdim, ydim, z_min, z_range));
        
        // set the graph for the user to see...
        c.getScene().setGraph(g);
	}
	
	private AbstractDrawable getMS2Scatter(final Matrix ms2scatter, final Matrix intensity_matrix, final int xdim, final int ydim, final double z_min, final double z_range) {
		Scatter s = new Scatter();
		final ArrayList<Coord3d> points = new ArrayList<Coord3d>();
		final ArrayList<Color> colours  = new ArrayList<Color>();
		if (ms2scatter == null)
			return s;
		
		ms2scatter.each(new MatrixProcedure() {

			@Override
			public void apply(int r, int c, double arg2) {
				if (arg2 > 0.0) {
					points.add(new Coord3d(((float)r)/xdim, ((float)c)/ydim, 0.0));
					
					// the point is essentially transparent if the peak is not intense
					float intensity = (float) ((intensity_matrix.get(r, c) - z_min) / z_range);
					// bin it (to avoid occlusion problems)
					if (intensity >= 0.9)
						intensity = 1.0f;
					else if (intensity >= 0.4)
						intensity = 0.7f;
					else
						intensity = 0.4f;
					
					colours.add(new Color(1.0f, 0.0f, 0.0f, intensity));
				}
			}
			
		});
		s.setData(points.toArray(new Coord3d[0]));
		s.setColors(colours.toArray(new Color[0]));
		
		return s;
	}

	protected double getMinimum(Matrix matrix) {
		assert(matrix != null);
		
		MinimumMatrixProcedure mp = new MinimumMatrixProcedure();
		matrix.each(mp);
		return mp.min;
	}

	protected double getMaximum(Matrix matrix) {
		assert(matrix != null);
		
		MaximumMatrixProcedure mp = new MaximumMatrixProcedure();
		matrix.each(mp);
		return mp.max;
	}
	
	protected Matrix transform(Matrix matrix, String zTransform) {
		if (zTransform.startsWith("Log10")) {
			return matrix.transform(new MatrixFunction() {

				@Override
				public double evaluate(int r, int c, double val) {
					return (val > 0.0) ? Math.log(val) : 0.0;
				}
				
			});
		} else if (zTransform.startsWith("Recip")) {
			return matrix.transform(new MatrixFunction() {

				@Override
				public double evaluate(int r, int c, double val) {
					return (val > 0.0) ? 1/val : 0.0;
				}
				
			});
		} else if (zTransform.startsWith("Square")) {
			return matrix.transform(new MatrixFunction() {

				@Override
				public double evaluate(int r, int c, double val) {
					return (val > 0.0) ? Math.sqrt(val) : 0.0;
				}
				
			});
		} else
			return matrix;
	}

	@Override
	protected Quality getOpenGLQuality(final Logger logger, final GLCapabilities glc) {
    	Quality q = Quality.Advanced;
    	int transparent_bits = glc.getAlphaBits();
    	if (transparent_bits < 1) {
    		logger.warning("Your computer does not support transparency. Disabling.");
    		q.setAlphaActivated(false);
    	}
    	q.setDepthActivated(true);
    	q.setAlphaActivated(true);
    	logger.info("Depth?"+q.isDepthActivated()+" Transparency? "+q.isAlphaActivated());
        return q;
	}
	
	@Override
	protected ComboBoxModel<String> getShowAsOptions() {
	    	return new DefaultComboBoxModel<String>(new String[] { "Scatter (fastest)", "Surface" });
	}
	
	private class MinimumMatrixProcedure implements MatrixProcedure {
		public double min = Double.POSITIVE_INFINITY;
		@Override
		public void apply(int arg0, int arg1, double arg2) {
			if (arg2 < min)
				min = arg2;
		}
		
	};
	
	private class MaximumMatrixProcedure implements MatrixProcedure {
		public double max = Double.NEGATIVE_INFINITY;
		@Override
		public void apply(int arg0, int arg1, double arg2) {
			if (arg2 > max)
				max = arg2;
		}
		
	};
}
