package au.edu.unimelb.plantcell.views.ms;

import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealMatrixPreservingVisitor;
import org.jzy3d.chart.Chart;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.builder.Builder;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.CompileableComposite;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.rendering.scene.Graph;
import org.jzy3d.plot3d.rendering.scene.Scene;

import au.edu.unimelb.plantcell.views.bar3d.MyAxisRenderer;
import au.edu.unimelb.plantcell.views.bar3d.Plot3DBarNodeView;

/**
 * Implements a plot of MS1 peaks against retention time (summing intensity into bins) over a number of runs 
 * - see the NodeModel for details.
 * 
 * @author andrew.cassin
 *
 * @param <T> must extend from the MassSpecSurfaceNodeModel
 */
public class MassSpecSurfaceNodeView<T extends MassSpecSurfaceNodeModel> extends Plot3DBarNodeView<T> {

	protected MassSpecSurfaceNodeView(T nodeModel) {
		super(nodeModel);
		JFrame f = setup("MS1 versus RT surface plot");

        addStatus(f);
      
        final JPanel image_panel = new JPanel();
        final JPanel button_panel= addButtons(image_panel, true, false);
     
        JSplitPane split_pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        split_pane.setBottomComponent(new JScrollPane(image_panel));
        split_pane.setTopComponent(button_panel);
        f.add(split_pane, BorderLayout.EAST);
	}
	
	@Override
	public void modelChanged() {
		MassSpecSurfaceNodeModel nodeModel = (MassSpecSurfaceNodeModel)getNodeModel();
		if (nodeModel == null) {
			setStatus("Please execute the node to display the surface.");
			getChart().clear();
			return;
		}
        
        final RealMatrix matrix = nodeModel.getSurface();
        if (matrix == null || matrix.getRowDimension() < 1 || matrix.getColumnDimension() < 1) {
        	setStatus("No surface to display, please execute the node - reconfigure?");
        	getChart().clear();
        	return;
        }
        Range x_range = new Range(0.0, 1.0);
        Range y_range = new Range(0.0, 1.0);
        
        transform(matrix, this.getZTransform());
        final double z_max   = getMaximum(matrix);		// NB: AFTER transform!
        final double z_min   = getMinimum(matrix);		// NB: AFTER transform!
        final double z_range = range(z_min, z_max);
        
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
	        final int x_steps = (matrix.getRowDimension() < 500) ? matrix.getRowDimension()-1 : 500;
	        final int y_steps = (matrix.getColumnDimension() < 500) ? matrix.getColumnDimension()-1 : 500;
	        CompileableComposite surface = Builder.buildOrthonormalBig(
	        		new OrthonormalGrid(x_range, x_steps, y_range, y_steps), 
	        		new Mapper() {
	
	        	//
	        	// Given we have to downsample, what to report - mean, median, max, sum, ...? Go with max for now
	        	//
				@Override
				public double f(double x, double y) {
					int xdim = matrix.getRowDimension();
					int ydim = matrix.getColumnDimension();
					int ix = (int) (x * xdim);
					int iy = (int) (y * ydim);
					int x_n = (int) Math.floor((double)xdim / x_steps) + 1;
					int y_n = (int) Math.floor((double)ydim / y_steps) + 1;
					int cnt = 0;
					/*double sum = 0.0;*/
					double max = Double.NEGATIVE_INFINITY;
					for (int i=ix - x_n; i<ix+x_n; i++) {
						for (int j=iy - y_n; j<iy + y_n; j++) {
							if (i<0 || i>= xdim)
								continue;
							if (j<0 || j>= ydim) 
								continue;
							double val = matrix.getEntry(i, j);
							if (val <= 0.0)
								continue;
							//sum += val;
							if (val > max)
								max = val;
							cnt++;
						}
					}
					if (cnt < 1)
						return 0.0d;
					//return ((sum/cnt) - z_min) / z_range;*/
					
					return (max - z_min) / z_range;
				}
	        	
	        });
	        surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), 0.0f, 1.0f));
	       	
	        g.add(surface);
        } else {	// must be scatter for now...
	        Scatter s = new Scatter();
	        final int xdim = matrix.getRowDimension()-1;
	        final int ydim = matrix.getColumnDimension()-1;
	        final ArrayList<Coord3d> points = new ArrayList<Coord3d>();
	        /* return value ignored */ matrix.walkInRowOrder(new RealMatrixPreservingVisitor() {
				@Override
				public double end() {
					return 0.0;
				}
	
				@Override
				public void start(int arg0, int arg1, int arg2, int arg3, int arg4,
						int arg5) {
				}
	
				@Override
				public void visit(int r, int c, double val) {
					if (val > 0.0) {
						points.add(new Coord3d(((float)r)/xdim, ((float)c)/ydim, (float) (val - z_min) / z_range));
					}
				}
	        	
	        });
	        s.setData(points.toArray(new Coord3d[0]));
	        g.add(s);
        }
        c.getScene().setGraph(g);
	}
	
	private double getMinimum(RealMatrix m) {
		assert(m != null);
		return m.walkInRowOrder(new RealMatrixPreservingVisitor() {
        	private double min = Double.POSITIVE_INFINITY;
        	
			@Override
			public double end() {
				return min;
			}

			@Override
			public void start(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
			}

			@Override
			public void visit(int r, int c, double val) {
				if (val < min)
					min = val;
			}
        	
        });
	}

	double getMaximum(RealMatrix m) {
		assert(m != null);
		return m.walkInRowOrder(new RealMatrixPreservingVisitor() {
        	private double max = Double.NEGATIVE_INFINITY;
        	
			@Override
			public double end() {
				return max;
			}

			@Override
			public void start(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
			}

			@Override
			public void visit(int r, int c, double val) {
				if (val > max)
					max = val;
			}
        	
        });
		
	}
	
	private void transform(RealMatrix matrix, String zTransform) {
		if (zTransform.startsWith("Log10")) {
			for (int i=0; i<matrix.getRowDimension(); i++) {
				for (int j=0; j<matrix.getColumnDimension(); j++) {
					double val = matrix.getEntry(i, j);
					if (val > 0.0) {
						matrix.setEntry(i, j, Math.log(val));
					}
				}
			}
		} else if (zTransform.startsWith("Recip")) {
			for (int i=0; i<matrix.getRowDimension(); i++) {
				for (int j=0; j<matrix.getColumnDimension(); j++) {
					double val = matrix.getEntry(i, j);
					if (val > 0.0) {
						matrix.setEntry(i, j, 1/val);
					}
				}
			}
		} else if (zTransform.startsWith("Square")) {
			for (int i=0; i<matrix.getRowDimension(); i++) {
				for (int j=0; j<matrix.getColumnDimension(); j++) {
					double val = matrix.getEntry(i, j);
					if (val > 0.0) {
						matrix.setEntry(i, j, Math.sqrt(val));
					}
				}
			}
		}
	}

	@Override
	protected ComboBoxModel<String> getShowAsOptions() {
	    	return new DefaultComboBoxModel<String>(new String[] { "Scatter (fastest)", "Surface" });
	}
}
