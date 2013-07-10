package au.edu.unimelb.plantcell.views.plot3d;

import java.awt.event.MouseWheelEvent;

import org.jzy3d.chart.controllers.mouse.camera.CameraMouseController;
import org.jzy3d.maths.Coord3d;


/**
 * Hook for future development but not really useful at the moment ;-)
 * @author andrew.cassin
 *
 */
public class MyCameraMouseController extends CameraMouseController {
	@SuppressWarnings("unused")
	private float cumulative = 0.0f;
	@SuppressWarnings("unused")
	private Coord3d original_viewpoint = null;
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent ev) {
		super.mouseWheelMoved(ev);
		/*float rot = (float) ev.getPreciseWheelRotation();
		
		cumulative += rot;
	
		Logger.getAnonymousLogger().info("Cumulative rotation: "+cumulative);
		
		@SuppressWarnings("unused")
		float factor = 1.0f;
		if (cumulative < 0.0f) 
			factor = 1.0f - (cumulative / 100.0f);
		else if (cumulative > 0.0f)
			factor = 1.0f + (cumulative / 100.0f);*/
		
	}
}
