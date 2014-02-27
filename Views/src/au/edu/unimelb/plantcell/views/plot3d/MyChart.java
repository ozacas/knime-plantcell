package au.edu.unimelb.plantcell.views.plot3d;

import org.jzy3d.chart.Chart;
import org.jzy3d.plot3d.rendering.canvas.Quality;

public class MyChart extends Chart {
	private boolean supports_transparency;
	
	public MyChart() {
		this(Quality.Nicest, "swing");
	}
	
	public MyChart(final Quality q) {
		this(q, "swing");
	}
	
	public MyChart(Quality q, final String toolkit) {
		super(q, toolkit);
		setSupportsTransparency(q.isAlphaActivated());
	}
	
	/**
	 * By using the accessor we avoid exposing q, which helps with access to this low-level
	 * API's from other plugins.
	 * 
	 * @param new_transparency
	 */
	public void setSupportsTransparency(boolean new_transparency) {
		supports_transparency = new_transparency;
	}
	
	public boolean doesSupportTransparency() {
		return supports_transparency;
	}
}
