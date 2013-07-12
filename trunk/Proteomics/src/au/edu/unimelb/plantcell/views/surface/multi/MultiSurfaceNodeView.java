package au.edu.unimelb.plantcell.views.surface.multi;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

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
 * Renders multiple surfaces each with different properties, datapoints are taken from the input data table.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MultiSurfaceNodeView<T extends MultiSurfaceNodeModel> extends MassSpecSurfaceNodeView<T> implements TableModelListener {
	private SurfaceTableModel m_surface_settings = null;
	private JTable t;
	
    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link MultiSurfaceNodeModel})
     */
    protected MultiSurfaceNodeView(final T nodeModel) {
        super(nodeModel);

        JFrame f = setupOpenGL("Multi-Surface 3D Plot");
        final JPanel image_panel = new JPanel();
        // no 'show as' checkbox (on the right) for this view as each surface has its own
        JPanel button_panel = addButtons(image_panel, false, false, false, false);
        f.getContentPane().add(button_panel, BorderLayout.EAST);
    }
    
    /**
     * Adds a table to the status bar for the user to configure each surface
     */
    @Override
    protected JPanel getStatusPanel() {
    	JPanel sp = new JPanel();
    	JPanel kid = new JPanel();
    	kid.setLayout(new BoxLayout(kid, BoxLayout.Y_AXIS));
    	kid.add(super.getStatusPanel());
    	kid.add(Box.createRigidArea(new Dimension(5,5)));
    	m_surface_settings = new SurfaceTableModel(getNodeModel());
    	m_surface_settings.addTableModelListener(this);
    	t = new JTable(m_surface_settings);
    	TableColumn display_as_column = t.getColumnModel().getColumn(1);
    	JComboBox<String> show_as = new JComboBox<String>(new String[] {"Scatter", "Surface"});
    	display_as_column.setCellEditor(new DefaultCellEditor(show_as));
    	t.setDefaultEditor(Color.class, new MyColourEditor());
    	t.setDefaultEditor(Integer.class, new MySliderEditor());
    	t.setCellSelectionEnabled(false);
    	t.setDefaultRenderer(Color.class, new MyColorRenderer());
    	t.setDefaultRenderer(Integer.class, new MySliderRenderer());
    	kid.add(t.getTableHeader());
    	sp.setLayout(new BorderLayout());
    	sp.add(kid, BorderLayout.NORTH);
    	sp.add(t, BorderLayout.CENTER);
    
    	return sp;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        MultiSurfaceNodeModel nodeModel = (MultiSurfaceNodeModel)getNodeModel();
        assert nodeModel != null;
        
        m_surface_settings.modelChanged(nodeModel);
        if (nodeModel == null || nodeModel.getSurfaceCount() < 1) {
        	getChart().clear();
        	return;
        }
        Chart c = getChart();
        Graph g = new Graph(new Scene(), true);
        
        double z_min = Double.POSITIVE_INFINITY;
        double z_max = Double.NEGATIVE_INFINITY;
        for (String surface_name : nodeModel.getZNames()) {
        	final Matrix in = nodeModel.getMatrix(surface_name, getZTransform());
        	if (in == null)
        		continue;
        	double min = nodeModel.getMinimum(surface_name);		// NB: NO transform!
        	if (min < z_min)
        		z_min = min;
        	double max = nodeModel.getMaximum(surface_name);		// NB: NO transform!
        	if (max > z_max)
        		z_max = max;
        	
	        BoundingBox3d bb = g.getBounds();
	    	bb.setZmax(1.0f);
	    	bb.setZmin(0.0f);
	    	bb.setXmin(0.0f);
	    	bb.setXmax(1.0f);
	    	bb.setYmin(0.0f);
	    	bb.setYmax(1.0f);
	    	
		    AbstractDrawable surface = getOpenGLSurface(in, surface_name);
		    if (surface != null)
		    	g.add(surface);
        }
        c.getAxeLayout().setXTickRenderer(new MyAxisRenderer(nodeModel.getXMin(), nodeModel.getXMax()));
    	c.getAxeLayout().setYTickRenderer(new MyAxisRenderer(nodeModel.getYMin(), nodeModel.getYMax()));
    	c.getAxeLayout().setZTickRenderer(new MyAxisRenderer(z_min, z_max));
    	c.getAxeLayout().setYAxeLabel(nodeModel.getYLabel());
    	c.getAxeLayout().setXAxeLabel(nodeModel.getXLabel());
    	c.getAxeLayout().setZAxeLabel("Z ("+nodeModel.getSurfaceCount()+" surfaces)");
        c.getScene().setGraph(g);
    }
    
    @Override
    public String getDefaultStatusMessage() {
    	return "To alter the appearance of a surface, edit the table below.";
    }
    
    /**
     * For now we only support scatter plots... we really should map this to a surface, but it requires too much
     * memory to be practical???
     * 
     * @param matrix
     * @param surface_name
     * @return
     */
	private AbstractDrawable getOpenGLSurface(final Matrix matrix, final String surface_name) {
		String type = m_surface_settings.getShowAs(surface_name);
		logger.info("Displaying surface "+surface_name+" as "+type.toLowerCase());
		double alpha = m_surface_settings.getAlpha(surface_name);
    	logger.info("Transparency for "+surface_name+" is "+alpha);
    	int size = m_surface_settings.getSize(surface_name);
    	logger.info("Relative size for objects in "+surface_name+" is "+size);
    	Color surface_colour = m_surface_settings.getColour(surface_name);
    	logger.info("Color for object in "+surface_name+" is RGB: "+surface_colour.r+" "+surface_colour.g+" "+surface_colour.b);
    	
    	
		final double z_offset = (-50.0d + (Integer) m_surface_settings.getZOffset(surface_name)) / 100.0;
		//if (type.startsWith("Scatter")) {
			final ArrayList<Coord3d> points = new ArrayList<Coord3d>();
			final double z_min = getMinimum(matrix);
			final double z_max = getMaximum(matrix);
			final double z_range= range(z_min, z_max);
			
			matrix.each(new MatrixProcedure() {
	
				@Override
				public void apply(int r, int c, double val) {
					points.add(new Coord3d(((float)r)/matrix.rows(), ((float)c)/matrix.columns(), (val - z_min) / z_range + z_offset));
				}
				
			});
			
			surface_colour.alphaSelf((float)alpha);
			return new Scatter(points.toArray(new Coord3d[0]), surface_colour, (float) (getRadius() * 10.0f * size));
		//} else  { // must be surface
		//	return null;
		//}
	}

	@Override
	public void tableChanged(TableModelEvent e) {
		//System.err.println("Changed user settings: redrawing...");
		modelChanged();
		getChart().render();
	}

}

