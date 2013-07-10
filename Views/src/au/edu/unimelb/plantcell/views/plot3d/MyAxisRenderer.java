package au.edu.unimelb.plantcell.views.plot3d;

import org.jzy3d.plot3d.primitives.axes.layout.renderers.ITickRenderer;

/**
 * A simple-minded numeric renderer which displays the axis values rather than the computational range of the chart
 * 
 * @author andrew.cassin
 *
 */
public class MyAxisRenderer implements ITickRenderer {
	private float min;
	private float max;
	
	public MyAxisRenderer(double x_min, double x_max) {
		assert((float) x_max >=  x_min);
		
		this.min = (float) x_min;
		this.max = (float) x_max;
	}
	
	@Override
	public String format(float value) {
		return String.valueOf(min + value * (max-min));
	}

}
