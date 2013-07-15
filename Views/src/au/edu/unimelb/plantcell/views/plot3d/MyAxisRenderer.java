package au.edu.unimelb.plantcell.views.plot3d;

import org.jzy3d.plot3d.primitives.axes.layout.renderers.ITickRenderer;

/**
 * A simple-minded numeric renderer which displays the axis values rather than the computational range of the chart
 * 
 * @author andrew.cassin
 *
 */
public class MyAxisRenderer implements ITickRenderer {
	private float v1;
	@SuppressWarnings("unused")
	private float v2;
	private float m_range;
	
	public MyAxisRenderer(double v1, double v2) {
		this((float) v1, (float) v2);
	}
	
	public MyAxisRenderer(float v1, float v2) {
		float min = (v1 < v2) ? v1 : v2;
		float max = (v1 > v2) ? v1 : v2;
		this.v1 = (float) min;
		this.v2 = (float) max;
		m_range = range(min, max);
		//Logger.getAnonymousLogger().info("Range is "+m_range);
	}
	
	private float range(float x_min, float x_max) {
		float ret = 0.0f;
    	if ((x_min < 0.0f && x_max < 0.0f) || (x_max >= 0.0f && x_min >= 0.0f)) {
    		ret = Math.abs(x_max) - Math.abs(x_min);
    	} else {
    		ret = Math.abs(x_min) + Math.abs(x_max);
    	}
    	
    	if (ret < 0)
    		return -ret;
    	return ret;
	}
	
	@Override
	public String format(float value) {
		return String.valueOf(v1 + value * m_range);
	}

}
