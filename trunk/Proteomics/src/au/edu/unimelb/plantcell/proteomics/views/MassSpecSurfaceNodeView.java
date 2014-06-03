package au.edu.unimelb.plantcell.proteomics.views;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.jzy3d.chart.Chart;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.builder.Builder;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.AbstractDrawable;
import org.jzy3d.plot3d.primitives.CompileableComposite;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.primitives.axes.layout.providers.RegularTickProvider;
import org.jzy3d.plot3d.rendering.legends.colorbars.ColorbarLegend;
import org.jzy3d.plot3d.rendering.scene.Graph;
import org.jzy3d.plot3d.rendering.scene.Scene;
import org.knime.core.node.NodeModel;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.matrix.functor.MatrixFunction;
import org.la4j.matrix.functor.MatrixProcedure;

import au.edu.unimelb.plantcell.views.plot3d.Plot3DBarNodeView;

/**
 * Implements a plot of MS1 peaks against retention time (summing intensity into bins) over a number of runs 
 * - see the NodeModel for details.
 * 
 * @author andrew.cassin
 *
 * @param <T> must extend from the MassSpecSurfaceNodeModel
 */
public class MassSpecSurfaceNodeView<T extends NodeModel> extends Plot3DBarNodeView<T> implements ActionListener {
	private JSpinner m_rt_lower;
	private JSpinner m_rt_upper;
	private JSpinner m_mz_lower;
	private JSpinner m_mz_upper;
	private JComboBox<String> file_list;
	
	// a simple most-recently-used cache of the downsampled matrix for display -- see downsample() below
	private Map<String,SurfaceMatrixAdapter> m_cache = new HashMap<String,SurfaceMatrixAdapter>();
	
	
	
	protected MassSpecSurfaceNodeView(T nodeModel) {
		super(nodeModel);
	}
	
	/**
	 * this implementation, which subclasses may override, requires that <code>nodeModel</code> is derived from <code>MassSpecSurfaceNodeModel</code>
	 * 
	 * @param nodeModel must not be null
	 */
	@Override
	protected void init(T nodeModel) {
	    MassSpecSurfaceNodeModel mdl = (MassSpecSurfaceNodeModel) nodeModel;

		JFrame f = setupOpenGL("Mass Spec. mzML - RT versus M/Z");
	    JPanel button_panel = addButtons(null, true, true, true, true);
	    JPanel p2 = new JPanel();
	    p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));
	    p2.add(new JLabel("File"));
	    file_list = new JComboBox<String>(mdl.getFilesByName());
	    p2.add(file_list);
	    JPanel p = new JPanel();
	    p.setBorder(BorderFactory.createTitledBorder("Region of interest"));
	    p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
	    
	    JPanel rt_panel = new JPanel();
	    rt_panel.setLayout(new BoxLayout(rt_panel, BoxLayout.X_AXIS));
	    rt_panel.add(new JLabel("RT: [ "));
	    
	    m_rt_lower = new JSpinner(new SpinnerNumberModel(mdl.getRTmin(), mdl.getRTmin(), mdl.getRTmax(), 100.0));
	    m_rt_upper = new JSpinner(new SpinnerNumberModel(mdl.getRTmax(), mdl.getRTmin(), mdl.getRTmax(), 100.0));
	   
	    rt_panel.add(m_rt_lower);
	    rt_panel.add(new JLabel(", "));
	    rt_panel.add(m_rt_upper);
	    rt_panel.add(new JLabel(" ]"));
	    p.add(rt_panel);
	    
	    JPanel mz_panel = new JPanel();
	    mz_panel.setLayout(new BoxLayout(mz_panel, BoxLayout.X_AXIS));
	    mz_panel.add(new JLabel("M/Z: [ "));
	    m_mz_lower = new JSpinner(new SpinnerNumberModel(mdl.getMZmin(), mdl.getMZmin(), mdl.getMZmax(), 100.0));
	    m_mz_upper = new JSpinner(new SpinnerNumberModel(mdl.getMZmax(), mdl.getMZmin(), mdl.getMZmax(), 100.0));
	   
	    mz_panel.add(m_mz_lower);
	    mz_panel.add(new JLabel(", "));
	    mz_panel.add(m_mz_upper);
	    mz_panel.add(new JLabel(" ]"));
	    
	    p.add(rt_panel);
	    p.add(mz_panel);
	    JButton refresh_button = new JButton("Refresh");
	    refresh_button.addActionListener(this);
	    p.add(refresh_button);
	    button_panel.add(p);
	    f.getContentPane().add(button_panel, BorderLayout.EAST);
	}
	
	@Override
	protected void modelChanged() {
		MassSpecSurfaceNodeModel nodeModel = (MassSpecSurfaceNodeModel)getNodeModel();
		if (nodeModel == null) {
			setStatus("Please execute the node to display the surface.");
			getChart().clear();
			return;
		}
		File mzmlFile = nodeModel.getFileByName(file_list.getSelectedItem().toString());
        
		double rt_min = ((Double)m_rt_lower.getValue()).doubleValue();
		double rt_max = ((Double)m_rt_upper.getValue()).doubleValue();
		double mz_min = ((Double)m_mz_lower.getValue()).doubleValue();
		double mz_max = ((Double)m_mz_upper.getValue()).doubleValue();
		
		double r1 = Math.min(rt_min, rt_max);
		double r2 = Math.max(rt_min, rt_max);
		double mz1= Math.min(mz_min, mz_max);
		double mz2= Math.max(mz_min, mz_max);
		
		long start = System.currentTimeMillis();
		logger.info("Start time: "+start);
		
		// this method always returns a square matrix (since we are projecting onto a unit cube) so the output
		// matrix does not necessarily equal the user input RT [r1, r2] and MZ [mz1, mz2] ranges
        SurfaceMatrixAdapter m  = nodeModel.getSurface(mzmlFile, false, r1, r2, mz1, mz2);
        
        if (m == null || m.rows() < 1 || m.columns() < 1) {
        	setStatus("No surface to display, please execute the node - reconfigure?");
        	getChart().clear();
        	return;
        }
      
        logger.info("Surface is "+m.rows()+" x "+m.columns());
        final SurfaceMatrixAdapter surface_matrix  = transform(m, this.getZTransform(), (m.rows() > 500) ? 500 : m.rows(), (m.columns() > 500) ? 500 : m.columns());
        long after_transform = System.currentTimeMillis();
        logger.info("Got surface matrix: "+m.rows()+" x "+m.columns());
        logger.info("Time to transform and downsampling: "+(after_transform - start));
        final double z_max   = getMaximum(surface_matrix);		// NB: AFTER transform AND downsampling!
        final double z_min   = getMinimum(surface_matrix);		// NB: AFTER transform AND downsampling!
        final double z_range = range(z_min, z_max);
        logger.info("Z range after transform: ["+z_min+", "+z_max+"]");
        logger.info("X range: ["+surface_matrix.getXMin()+", "+surface_matrix.getXMax()+"]");
        logger.info("Y range: ["+surface_matrix.getYMin()+", "+surface_matrix.getYMax()+"]");
        
        Chart c = getChart();
        Graph g = new Graph(new Scene(), true);
    	
    	String zTransform = ": "+getZTransform();
    	if (zTransform.endsWith("None"))
    		zTransform = "";
    	c.getAxeLayout().setXAxeLabel("M/Z");
    	c.getAxeLayout().setYAxeLabel("Retention Time");
    	c.getAxeLayout().setZAxeLabel("Intensity"+zTransform);
    	
    	c.getAxeLayout().setXTickRenderer(new MyAxisRenderer(surface_matrix.getXMin(), surface_matrix.getXMax()));
    	c.getAxeLayout().setYTickRenderer(new MyAxisRenderer(surface_matrix.getYMin(), surface_matrix.getYMax()));
    	c.getAxeLayout().setZTickRenderer(new MyAxisRenderer(z_min, z_max));
        
        // since the practical screen resolution is of the order of 1k pixels, we sample based on the matrix dimension to reduce memory usage
        String show_as = getShowAs().toLowerCase().trim();
	    AbstractDrawable surface = getOpenGLSurface(surface_matrix, show_as.startsWith("surface"), z_min, z_range);
	    g.add(surface);
        
	    long after_surface = System.currentTimeMillis();
	    logger.info("Time to main surface: "+(after_surface - after_transform));
	    
        // finally add the MS2 heatmap
        SurfaceMatrixAdapter heatmap_matrix = nodeModel.getSurface(mzmlFile, true, surface_matrix.getYMin(), surface_matrix.getYMax(), 
        		surface_matrix.getXMin(), surface_matrix.getXMax());
        SurfaceMatrixAdapter ms2 = downsample(heatmap_matrix, surface_matrix.rows(), surface_matrix.columns(), true);
        addMatrixToCache(ms2);
        g.add(getMS2Scatter(ms2, surface_matrix, z_min, z_range));
        
        // ensure the bounds are correctly set (so the axes scale correctly)
        BoundingBox3d bb = g.getBounds();
    	bb.setXmin(0.0f);
    	bb.setXmax(1.0f);
    	bb.setYmin(0.0f);
    	bb.setYmax(1.0f);
    	bb.setZmin(0.0f);
    	bb.setZmax(1.0f);
    	
        // set the graph for the user to see...
    	c.getScene().clear();
        c.getScene().setGraph(g);
        c.getView().setBoundManual(g.getBounds());
        
        long after_surface2 = System.currentTimeMillis();
	    logger.info("Time to ms2 surface: "+(after_surface2 - after_surface));
	}
	

	protected final double getMinimum(final SurfaceMatrixAdapter matrix) {
		return getMinimum(matrix.getMatrix());
	}

	protected double getMinimum(final Matrix matrix) {
		assert(matrix != null);
		MinimumMatrixProcedure mp = new MinimumMatrixProcedure();
		matrix.each(mp);
		return mp.min;
	}
	
	protected final double getMaximum(final SurfaceMatrixAdapter m) {
		return getMaximum(m.getMatrix());
	}
	
	protected double getMaximum(final Matrix matrix) {
		assert(matrix != null);
		
		MaximumMatrixProcedure mp = new MaximumMatrixProcedure();
		matrix.each(mp);
		return mp.max;
	}
	
	protected SurfaceMatrixAdapter addMatrixToCache(final SurfaceMatrixAdapter ret) {
		// ensure not too many cached matrices...
		if (m_cache.size() > 6)
			m_cache.remove(m_cache.keySet().toArray(new String[0])[0]);		// delete first target
		// add new matrix
		m_cache.put(ret.getKey(), ret);
		logger.info("Added matrix to cache: "+ret.getKey()+": "+m_cache.size()+" cached matrices.");
		return ret;
	}
	
	protected SurfaceMatrixAdapter getCachedMatrix(final SurfaceMatrixAdapter in, int new_rows, int new_cols, final String zTransform) {
		String key = in.getKey()+" "+new_rows+"x"+new_cols;
		if (zTransform != null)
			key += " " + zTransform;
		if (m_cache.containsKey(key)) {
			return m_cache.get(key);
		}
		logger.info("Could not find cached matrix: "+key);
		return null;
	}
	
	protected SurfaceMatrixAdapter downsample(final SurfaceMatrixAdapter surface, int new_rows, int new_cols, boolean is_ms2) {
		SurfaceMatrixAdapter m = getCachedMatrix(surface, new_rows, new_cols, null);
		if (m != null) {
			logger.info("Using cached downsampled matrix.");
			return m;
		}
		logger.info("Downsampling matrix to "+new_rows+" x "+new_cols);
		Matrix in = surface.getMatrix();
		Matrix dest = new Basic2DMatrix(new_rows, new_cols);
		
		double y_n = ((double)in.rows()) / new_rows;
		double x_n = ((double)in.columns()) / new_cols;
		
		int largest_x = -1;
		int largest_y = -1;
		for (int i=0; i<dest.rows(); i++) {
			for (int j=0; j<dest.columns(); j++) {
				double max = Double.NEGATIVE_INFINITY;
				
				int iy = (int) (y_n * i);
				int ix = (int) (x_n * j);
				for (int y=iy; y<iy + y_n; y++) {
					for (int x=ix; x<ix + x_n; x++) {
						if (y<0 || y>= in.rows())
							continue;
						if (x<0 || x>= in.columns()) 
							continue;
						double val = in.get(y, x);
						if (val > max)
							max = val;
					}
				}
				
				if (max > 0.0) {
					if (i > largest_y) 
						largest_y = i;
					if (j > largest_x)
						largest_x = j;
					dest.set(i, j, max);
				}
				
			}
		}
		
		logger.info("Got largest y="+largest_y+" x="+largest_x);
		
		SurfaceMatrixAdapter ret = new SurfaceMatrixAdapter(surface.getFile(), dest, is_ms2);
		ret.setBounds(surface);
		ret.setKey(null);
		return ret;
	}
	
	
	protected SurfaceMatrixAdapter transform(final SurfaceMatrixAdapter in, final String zTransform, int downsample_rows, int downsample_columns) {
		SurfaceMatrixAdapter m = getCachedMatrix(in, downsample_rows, downsample_columns, zTransform);
		if (m != null) {
			logger.info("Using cached matrix: "+m.getKey());
			return m;
		}
	
		return addMatrixToCache(transform(downsample(in, downsample_rows, downsample_columns, false), zTransform));
	}
	
	protected SurfaceMatrixAdapter transform(final SurfaceMatrixAdapter matrix, final String zTransform) {
		Matrix out = matrix.getMatrix().copy();
		if (zTransform.startsWith("Log10")) {
			out = out.transform(new MatrixFunction() {

				@Override
				public double evaluate(int r, int c, double val) {
					return (val > 0.0) ? Math.log(val) : 0.0;
				}
				
			});
			// FALLTHRU
		} else if (zTransform.startsWith("Recip")) {
			out = out.transform(new MatrixFunction() {

				@Override
				public double evaluate(int r, int c, double val) {
					return (val > 0.0) ? 1/val : 0.0;
				}
				
			});
			// FALLTHRU
		} else if (zTransform.startsWith("Square")) {
			out = out.transform(new MatrixFunction() {

				@Override
				public double evaluate(int r, int c, double val) {
					return (val > 0.0) ? Math.sqrt(val) : 0.0;
				}
				
			});
			// FALLTHRU
		} else
			return matrix;
		
		SurfaceMatrixAdapter ret = new SurfaceMatrixAdapter(matrix.getFile(), out);
		ret.setBounds(matrix);
		ret.setKey(ret.getKey() + " "+zTransform);
		return ret;
	}

	@Override
	protected ComboBoxModel<String> getShowAsOptions() {
	    	return new DefaultComboBoxModel<String>(new String[] { "Scatter (fastest)", "Surface" });
	}


	private AbstractDrawable getMS2Scatter(final SurfaceMatrixAdapter ms2scatter, final SurfaceMatrixAdapter intensity_matrix, 
											final double z_min, final double z_range) {
		Scatter s = new Scatter();
		final ArrayList<Coord3d> points = new ArrayList<Coord3d>();
		final ArrayList<Color> colours  = new ArrayList<Color>();
		if (ms2scatter == null)
			return s;
		
		assert(ms2scatter.rows() == intensity_matrix.rows() && 
				ms2scatter.columns() == intensity_matrix.columns());
		
		final int ydim = ms2scatter.rows();
		final int xdim = ms2scatter.columns();
		ms2scatter.getMatrix().each(new MatrixProcedure() {

			@Override
			public void apply(int r, int c, double arg2) {
				if (arg2 > 0.0) {
					float intensity = (float) ((intensity_matrix.get(r, c) - z_min) / z_range);
					
					points.add(new Coord3d(((float)c)/xdim, ((float)r)/ydim, intensity));
					float rgb = 1.0f - (float) arg2;
					if (rgb < 0.0f)
						rgb = 0.0f;
					colours.add(new Color(rgb, rgb, rgb, 1.0f));
				}
			}
			
		});
		s.setData(points.toArray(new Coord3d[0]));
		s.setColors(colours.toArray(new Color[0]));
		s.setWidth((float) getRadius() * 10.0f);
		Collections.sort(colours, new Comparator<Color>() {

			@Override
			public int compare(Color c1, Color c2) {
				float sum_a = c1.a + c1.b + c1.g;
				float sum_b = c2.a + c2.b + c2.g;
				
				if (sum_a < sum_b)
					return -1;
				else if (sum_a > sum_b)
					return 1;
				else
					return 0;
			}
			
		});
		
		// add a colour bar for the MS/MS scores...
		if (colours.size() > 0) {
			s.setLegend(new ColorbarLegend(s, 
					new RegularTickProvider(), new MyAxisRenderer(0.0, 1.0), 
					colours.get(0), colours.get(colours.size()-1)));
		}
		return s;
	}
	
	public AbstractDrawable getOpenGLSurface(final SurfaceMatrixAdapter matrix, boolean as_surface, final double z_min, final double z_range) {
		assert(matrix != null);
	
		final int xdim = matrix.columns();
		final int ydim = matrix.rows();
		
		if (as_surface) {
		    CompileableComposite surface = Builder.buildOrthonormalBig(
		    		new OrthonormalGrid(new Range(0.0, 1.0), xdim, new Range(0.0, 1.0), ydim), 
		    		new Mapper() {
		
				@Override
				public double f(double x, double y) {
					int ix = (int) (x * xdim);
					int iy = (int) (y * ydim);
					if (ix < 0)
						ix = 0;
					if (ix >= xdim)
						ix = xdim - 1;
					if (iy < 0)
						iy = 0;
					if (iy >= ydim)
						iy = ydim - 1;
					double out = ((matrix.get(iy, ix) - z_min) / z_range);
					if (out > 0.0)
						return out;
					
					// dont draw anything (better look than drawing something at the bottom of the cube)
					return Double.NaN;
				}
	    	
		    });
		    surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), 0.0f, 1.0f, new Color(1.0f,1.0f,1.0f,getAlpha())));
		    return surface;
		} else {
			Scatter s = new Scatter();
		       
	        final ArrayList<Coord3d> points = new ArrayList<Coord3d>();
	        matrix.getMatrix().each(new MatrixProcedure() {

				@Override
				public void apply(int r, int c, double val) {
					if (val > 0.0) {
						float y = ((float)r)/matrix.rows();
						float x = ((float)c)/matrix.columns();
						if (x > 1.0f || y > 1.0f || x < 0.0f || y < 0.0f) {
							logger.info("Should not happen! "+x+" "+y);
						}
						points.add(new Coord3d(x, y, (float) (val - z_min) / z_range));
					}
				}
	        	
	        });
	       
	        s.setData(points.toArray(new Coord3d[0]));
	        return s;
		}
	
	}
    
	/**
	 * called in response to a change of the surface dimensions, this code must update state and re-display the surface
	 * @param arg0 ignored by the current implementation
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// sanity check arguments
		double rt_min = ((Double)m_rt_lower.getValue()).doubleValue();
		double rt_max = ((Double)m_rt_upper.getValue()).doubleValue();
		double mz_min = ((Double)m_mz_lower.getValue()).doubleValue();
		double mz_max = ((Double)m_mz_upper.getValue()).doubleValue();
		if (rt_max <= rt_min) {
			m_rt_lower.setForeground(java.awt.Color.RED);
			m_rt_upper.setForeground(java.awt.Color.RED);
			return;
		}
		if (mz_max <= mz_min) {
			m_mz_lower.setForeground(java.awt.Color.RED);
			m_mz_upper.setForeground(java.awt.Color.RED);
			return;
		}
		m_rt_lower.setForeground(java.awt.Color.BLACK);
		m_rt_upper.setForeground(java.awt.Color.BLACK);
		m_mz_lower.setForeground(java.awt.Color.BLACK);
		m_mz_upper.setForeground(java.awt.Color.BLACK);
		
		// ok, go ahead and update...
		setStatus("Please wait... this may take a long time.");
		modelChanged();
		getChart().render();
		setStatus(null);
	}
}
