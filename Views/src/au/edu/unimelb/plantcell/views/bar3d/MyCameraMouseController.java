package au.edu.unimelb.plantcell.views.bar3d;

import java.awt.event.MouseWheelEvent;
import java.util.logging.Logger;

import org.jzy3d.chart.controllers.mouse.camera.CameraMouseController;
import org.jzy3d.maths.Coord3d;




public class MyCameraMouseController extends CameraMouseController {
	private float cumulative = 0.0f;
	private Coord3d original_viewpoint = null;
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent ev) {
		float rot = (float) ev.getPreciseWheelRotation();
		
		cumulative += rot;
	
		Logger.getAnonymousLogger().info("Cumulative rotation: "+cumulative);
		
		float factor = 1.0f;
		if (cumulative < 0.0f) 
			factor = 1.0f - (cumulative / 100.0f);
		else if (cumulative > 0.0f)
			factor = 1.0f + (cumulative / 100.0f);
		
	}
}
