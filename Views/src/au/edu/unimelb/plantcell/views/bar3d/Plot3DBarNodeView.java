package au.edu.unimelb.plantcell.views.bar3d;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.mouse.camera.CameraMouseController;
import org.jzy3d.colors.Color;
import org.jzy3d.global.Settings;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Statistics;
import org.jzy3d.plot3d.primitives.HistogramBar;
import org.jzy3d.plot3d.rendering.canvas.Quality;
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
	
    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link Plot3DBarNodeModel})
     */
    protected Plot3DBarNodeView(final Plot3DBarNodeModel nodeModel) {
        super(nodeModel);

        String model_quality = nodeModel.getQuality();
        Quality q = Quality.Nicest;
        if (model_quality.startsWith("Nice"))
        	;
        else if (model_quality.startsWith("Inter"))
        	q = Quality.Intermediate;
        else
        	q = Quality.Fastest;
      
        c = new Chart(q, "swing");
        c.addController(new CameraMouseController());
       
        f = new JFrame("3D Bar chart");
        f.setLayout(new BorderLayout());
        f.setMinimumSize(new Dimension(300,300));
        f.setPreferredSize(new Dimension(800,800));
        f.add((JComponent) c.getCanvas(), BorderLayout.CENTER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        Plot3DBarNodeModel nodeModel = (Plot3DBarNodeModel)getNodeModel();
        assert nodeModel != null;
        
        if (nodeModel != null && nodeModel.hasDataPoints()) {
        	c.clear();
        	double[] x = new double[nodeModel.countDataPoints()];
        	double[] y = new double[nodeModel.countDataPoints()];
        	double[] z = new double[nodeModel.countDataPoints()];
        	Color[] colours = new Color[nodeModel.countDataPoints()];
        	String[] rowids= new String[nodeModel.countDataPoints()];
        	nodeModel.getDataPoints(x, y, z, rowids, colours);
        	
        	double x_min = Statistics.min(x);
        	double x_max = Statistics.max(x);
        	double y_min = Statistics.min(y);
        	double y_max = Statistics.max(y);
        	double z_min = Statistics.min(z);
        	double z_max = Statistics.max(z);
        	
        	for (int i=0; i<x.length; i++) {
        		x[i] /= x_max;
        		y[i] /= y_max;
        		z[i] /= z_max;
        	}
        	
        	if (!Double.isNaN(x_max-x_min)) {
	        	for (int i=0; i<x.length; i++) {
	        		HistogramBar hb = new HistogramBar();
	        		
	        		hb.setData(new Coord3d(x[i], y[i], 0.0), (float) z[i], 0.0125f, colours[i]);
	        		hb.setWireframeDisplayed(false);
	        		c.getScene().getGraph().add(hb);
	        	}
        	}
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
			modelChanged(); 		// compute the graph
			try {
				Settings s = Settings.getInstance();
				s.setHardwareAccelerated(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			c.render();
			f.pack();
			f.setVisible(true);
		}
	}

}

