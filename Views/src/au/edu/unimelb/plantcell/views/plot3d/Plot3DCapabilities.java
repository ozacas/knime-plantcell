package au.edu.unimelb.plantcell.views.plot3d;

import java.util.logging.Logger;

import javax.media.opengl.GLCapabilities;

import org.jzy3d.global.Settings;
import org.jzy3d.plot3d.rendering.canvas.Quality;

public class Plot3DCapabilities {
	
	public static MyChart make3DChartInstance(final Logger logger) {
		assert(logger != null);
		
		// always use hardware if possible
        GLCapabilities glc = null;
        try {
        	Settings s = Settings.getInstance();
        	if (s != null) {
        		s.setHardwareAccelerated(true);
        		glc = s.getGLCapabilities();
        	}
        } catch (Exception e) {
        	e.printStackTrace();
        }
        Quality q = Quality.Intermediate;
        boolean no_depth_if_transparency = q.isDisableDepthBufferWhenAlpha();
        if (!no_depth_if_transparency) {
        	q.setAlphaActivated(true);
        	q.setDepthActivated(true);
        	logger.info("View has "+glc.getAlphaBits()+" bits per pixel for transparency.");
        } else {
        	logger.warning("Disabling transparency as your computer doesnt support both depth and transparency at the same time.");
        	q.setDepthActivated(true);
        	q.setAlphaActivated(false);
        } 
        if (glc != null) {
        	logger.info("View has "+glc.getDepthBits()+" bits per pixel for depth buffer.");
        	if (glc.getHardwareAccelerated())
        		logger.info("View is hardware accelerated.");
        }
        MyChart c = new MyChart(q, "swing");
        c.addController(new MyCameraMouseController());
        if (glc != null) {
        	logger.info("View has "+glc.getDepthBits()+" bits per pixel for depth buffer.");
        	if (glc.getHardwareAccelerated())
        		logger.info("View is hardware accelerated.");
        }
        
        return c;
	}
	
	/**
     * Return the Quality instance which the canvas must use
     * @param logger  the logger instance to use for hardware capabilities or null if no logging desired.
     * @param the OpenGL capabilities for the KNIME instance
     * @return must not be null
     */
    @SuppressWarnings("unused")
	private static Quality getOpenGLQuality(final Logger logger, final GLCapabilities glc) {
    	Quality q = Quality.Nicest;
    	boolean no_depth_if_transparency = q.isDisableDepthBufferWhenAlpha();
        if (!no_depth_if_transparency) {
        	q.setAlphaActivated(true);
        	q.setDepthActivated(true);
        	if (logger != null)
        		logger.info("View has "+glc.getAlphaBits()+" bits per pixel for transparency.");
        } else {
        	if (logger != null)
        		logger.warning("Disabling transparency as your computer doesnt support both depth and transparency at the same time.");
        	q.setDepthActivated(true);
        	q.setAlphaActivated(false);
        }
        return q;
	}
}
