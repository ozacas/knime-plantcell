package au.edu.unimelb.plantcell.io.read.spectra;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import javax.media.opengl.GLCapabilities;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.jzy3d.chart.Chart;
import org.jzy3d.colors.Color;
import org.jzy3d.global.Settings;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.scene.Graph;
import org.jzy3d.plot3d.rendering.scene.Scene;
import org.knime.core.node.ExternalApplicationNodeView;

import au.edu.unimelb.plantcell.views.bar3d.MyCameraMouseController;

public class PeakDistributionHeatmapNodeView extends ExternalApplicationNodeView<SpectraReaderNodeModel> {
	private final Logger logger = Logger.getLogger("MS1 3D View");
	private final Chart c;
	private final JFrame f;
	
	@SuppressWarnings("restriction")
	protected PeakDistributionHeatmapNodeView(SpectraReaderNodeModel model) {
		super(model);

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
        c = new Chart(q, "swing");
        c.addController(new MyCameraMouseController());
        if (glc != null) {
        	logger.info("View has "+glc.getDepthBits()+" bits per pixel for depth buffer.");
        	if (glc.getHardwareAccelerated())
        		logger.info("View is hardware accelerated.");
        }
        
        JComponent chart = (JComponent) c.getCanvas();
        chart.setMinimumSize(new Dimension(300,300));
        chart.setPreferredSize(new Dimension(600,600));
        
        f = new JFrame("MS1 Base Peaks by file");
        f.setLayout(new BorderLayout());
        f.setMinimumSize(new Dimension(300,300));
        f.setPreferredSize(new Dimension(800,800));
        f.add(chart, BorderLayout.CENTER);
		
        
	}

	@Override
	protected void onClose() {
		if (c != null) {
			c.clear();
			c.dispose();
		}
	}

	@Override
	protected void onOpen(String arg0) {
		if (c != null) {
			modelChanged();
			c.render();
			f.pack();
			f.setVisible(true);
		}
	}

	@Override
	protected void modelChanged() {
		SpectraReaderNodeModel mdl = (SpectraReaderNodeModel) getNodeModel();
		assert mdl != null;
		
		if (mdl != null && mdl.hasChromatogramPoints()) {
			File[] fractions = new File[mdl.countChromatogramFiles()];
			
			List<Coord3d> points = mdl.getChromatogramSurface(fractions);
			Graph g = new Graph(new Scene());
			Scatter surface = new Scatter(points.toArray(new Coord3d[0]), Color.GRAY);
			
			g.add(surface);
			c.getScene().setGraph(g);
		}
	}

}
