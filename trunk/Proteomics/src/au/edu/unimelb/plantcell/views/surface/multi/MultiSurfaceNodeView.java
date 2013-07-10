package au.edu.unimelb.plantcell.views.surface.multi;

import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.jzy3d.chart.Chart;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.AbstractDrawable;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.rendering.scene.Graph;
import org.jzy3d.plot3d.rendering.scene.Scene;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.functor.MatrixProcedure;

import au.edu.unimelb.plantcell.views.ms.MassSpecSurfaceNodeView;
import au.edu.unimelb.plantcell.views.plot3d.MyAxisRenderer;

/**
 * <code>NodeView</code> for the "MultiSurface" Node.
 * Represents multiple surfaces each with different properties, surface datapoints are taken from the input data table.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MultiSurfaceNodeView<T extends MultiSurfaceNodeModel> extends MassSpecSurfaceNodeView<T> {
	private final static Color[] m_colors = new Color[] { Color.BLACK, Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN };
	private int m_surface_idx;
	
    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link MultiSurfaceNodeModel})
     */
    protected MultiSurfaceNodeView(final T nodeModel) {
        super(nodeModel);

        JFrame f = setupOpenGL("Multi-surface 3D plot");

        addStatus(f);
      
        final JPanel image_panel = new JPanel();
        final JPanel button_panel= addButtons(image_panel, true, false);
     
        JSplitPane split_pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        split_pane.setBottomComponent(new JScrollPane(image_panel));
        split_pane.setTopComponent(button_panel);
        f.add(split_pane, BorderLayout.EAST);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        MultiSurfaceNodeModel nodeModel = (MultiSurfaceNodeModel)getNodeModel();
        assert nodeModel != null;
        
        if (nodeModel == null || nodeModel.getSurfaceCount() < 1) {
        	getChart().clear();
        	return;
        }
        Chart c = getChart();
        Graph g = new Graph(new Scene(), true);
        
        m_surface_idx=0;
        double z_min = Double.POSITIVE_INFINITY;
        double z_max = Double.NEGATIVE_INFINITY;
        for (String surface_name : nodeModel.getZNames()) {
        	final Matrix in = nodeModel.getMatrix(surface_name);
        	if (in == null)
        		continue;
        	double min = getMinimum(in);
        	if (min < z_min)
        		z_min = min;
        	double max = getMaximum(in);
        	if (max > z_max)
        		z_max = max;
        	final Matrix matrix  = transform(in, this.getZTransform());
        	
	        BoundingBox3d bb = g.getBounds();
	    	bb.setZmax(1.0f);
	    	bb.setZmin(0.0f);
	    	bb.setXmin(0.0f);
	    	bb.setXmax(1.0f);
	    	bb.setYmin(0.0f);
	    	bb.setYmax(1.0f);
	    	c.getAxeLayout().setYAxeLabel(nodeModel.getYLabel());
	    	c.getAxeLayout().setXAxeLabel(nodeModel.getXLabel());
	    	c.getAxeLayout().setZAxeLabel(surface_name);
	    	
	    
		    AbstractDrawable surface = getOpenGLSurface(matrix);
		    g.add(surface);
        }
        c.getAxeLayout().setXTickRenderer(new MyAxisRenderer(nodeModel.getXMin(), nodeModel.getXMax()));
    	c.getAxeLayout().setYTickRenderer(new MyAxisRenderer(nodeModel.getYMin(), nodeModel.getYMax()));
    	c.getAxeLayout().setZTickRenderer(new MyAxisRenderer(z_min, z_max));
        c.getScene().setGraph(g);
    }

    /**
     * You must pass true to the "show choice" during construction or this wont get called
     * @return a {@link ComboBoxModel} ready for the user to use
     */
    @Override
    protected ComboBoxModel<String> getShowAsOptions() {
    	return new DefaultComboBoxModel<String>(new String[] { "Scatter (fastest)", "Surface"  });
	}
    
	private AbstractDrawable getOpenGLSurface(final Matrix matrix) {
		String type = getShowAs();
		if (type.startsWith("Scatter")) {
			final ArrayList<Coord3d> points = new ArrayList<Coord3d>();
			final double z_min = getMinimum(matrix);
			final double z_max = getMaximum(matrix);
			final double z_range= range(z_min, z_max);
			matrix.each(new MatrixProcedure() {
	
				@Override
				public void apply(int r, int c, double val) {
					if (val > 0.0)
						points.add(new Coord3d(((float)r)/matrix.rows(), ((float)c)/matrix.columns(), (val - z_min) / z_range));
				}
				
			});
			Scatter s = new Scatter(points.toArray(new Coord3d[0]));
			s.setColor(m_colors[m_surface_idx++ % m_colors.length]);
			return s;
		} else  { // must be surface
			return null;
		}
	}

}

