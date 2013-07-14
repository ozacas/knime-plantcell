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
import org.jzy3d.plot3d.primitives.axes.layout.IAxeLayout;
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
	
	private float saw_min_x, saw_max_x, saw_min_y, saw_max_y;
	
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
        	setStatus("No model to display - please execute node!");
        	return;
        }
        Chart c = getChart();
        Graph g = new Graph(new Scene(), true);
        BoundingBox3d bb = g.getBounds();
        bb.setZmin(0.0f);
    	bb.setZmax(1.0f);
    	bb.setXmin(0.0f);
    	bb.setXmax(1.0f);
    	bb.setYmin(0.0f);
    	bb.setYmax(1.0f);
    	IAxeLayout ial = c.getAxeLayout();
        ial.setXTickRenderer(new MyAxisRenderer(nodeModel.getXMin(), nodeModel.getXMax()));
    	ial.setYTickRenderer(new MyAxisRenderer(nodeModel.getYMin(), nodeModel.getYMax()));
    	
    	
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
	    	
		    AbstractDrawable surface = getOpenGLSurface(in, surface_name);
		    if (surface != null)
		    	g.add(surface);
        }
        ial.setZTickRenderer(new MyAxisRenderer(z_max, z_min));
        ial.setYAxeLabel(nodeModel.getYLabel());
    	ial.setXAxeLabel(nodeModel.getXLabel());
    	ial.setZAxeLabel("Z ("+nodeModel.getSurfaceCount()+" surfaces)");
    	
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
			final double z_min  = getMinimum(matrix);
			final double z_max  = getMaximum(matrix);
			final double z_range= range(z_min, z_max);
			final int rows      = matrix.rows();
			final int columns   = matrix.columns();
			//logger.debug("Matrix has "+rows+" rows, and "+columns+" columns.");
			
			saw_min_x = Float.POSITIVE_INFINITY;
			saw_min_y = Float.POSITIVE_INFINITY;
			saw_max_x = Float.NEGATIVE_INFINITY;
			saw_max_y = Float.NEGATIVE_INFINITY;
			matrix.each(new MatrixProcedure() {
				
				@Override
				public void apply(int r, int c, double val) {
					float x_val = ((float)r)/rows;
					float y_val = ((float)c)/columns;
					if (x_val < saw_min_x)
						saw_min_x = x_val;
					if (x_val > saw_max_x)
						saw_max_x = x_val;
					if (y_val < saw_min_y)
						saw_min_y = y_val;
					if (y_val > saw_max_y)
						saw_max_y = y_val;
					points.add(new Coord3d(x_val, y_val, (val - z_min) / z_range + z_offset));
				}
				
			});
			
			System.out.println("Saw X["+saw_min_x+", "+saw_max_x+"] Y["+saw_min_y+", "+saw_max_y+"]");
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

