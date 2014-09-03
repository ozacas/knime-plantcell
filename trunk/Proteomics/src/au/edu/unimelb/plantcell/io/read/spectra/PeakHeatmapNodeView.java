package au.edu.unimelb.plantcell.io.read.spectra;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.jzy3d.chart.Chart;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.rendering.scene.Graph;
import org.jzy3d.plot3d.rendering.scene.Scene;
import org.knime.core.node.ExternalApplicationNodeView;

import au.edu.unimelb.plantcell.views.plot3d.Plot3DCapabilities;


/**
 * Plots a surface of chromatogram points on the 3D plot
 * @author andrew.cassin
 *
 */
public class PeakHeatmapNodeView extends ExternalApplicationNodeView<SpectraReaderNodeModel> {
	private final Logger logger = Logger.getLogger("Chromatogram 3D View");
	private final Chart c;
	private final JFrame f;
	
	protected PeakHeatmapNodeView(SpectraReaderNodeModel model) {
		super(model);

		c = Plot3DCapabilities.make3DChartInstance(logger);
		
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
			Scatter surface = new Scatter(points.toArray(new Coord3d[0]), Color.BLUE);
			
			g.add(surface);
			c.getScene().setGraph(g);
		}
	}

}
