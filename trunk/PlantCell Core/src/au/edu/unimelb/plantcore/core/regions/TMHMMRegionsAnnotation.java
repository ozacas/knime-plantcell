package au.edu.unimelb.plantcore.core.regions;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import au.edu.unimelb.plantcell.core.cells.AnnotationType;
import au.edu.unimelb.plantcell.core.cells.SequenceAnnotation;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackRendererInterface;


/**
 * TMHMM produces predictions for inside/outside and Transmembrane helices so
 * we return a specific annotation type to make for a better user experience. Be
 * sure to instantiate this type ie. the most specific type, when you have TMHMM data!
 * 
 * @author andrew.cassin
 *
 */
public class TMHMMRegionsAnnotation extends RegionsAnnotation implements TrackRendererInterface {
	private static ImageIcon m_legend = null;
	static {
		try {
			Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.core");
  			URL u = FileLocator.find(bundle, new Path("/icons/tmhmm_legend.png"), null);
			m_legend = new ImageIcon(u);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public AnnotationType getAnnotationType() {
		return AnnotationType.TMHMM_REGIONS;
	}
	
	/**
	 * Co-ordinates of predictions start at 1 for TMHMM
	 */
	@Override
	public int getOffset() {
		return 1;
	}
	
	@Override
	public Dimension paintTrack(final Map<String,Integer> props, 
			final Graphics g, final SequenceValue sv, final Track t) {
		
		int offset = props.get("offset");
		paintLabel(g, t.getName(), offset);
		
		SequenceAnnotation sa = t.getAnnotation();
		if (sa != null && sa.countAnnotations() >= 1) {
			List<RegionInterface> regions = sa.getRegions();
			double frac = ((double)props.get("track.width")) / sv.getLength();
				
			for (RegionInterface r : regions) {
				int start = r.getZStart();
				int end   = r.getZEnd();
				
				renderRoundedRegion(g, start, end, offset, r.getLabel(), frac);
			}
		}
		g.drawImage(m_legend.getImage(), 605, offset-3, null);
		return new Dimension(0,20);
	}
	
	
	/**
	 * Renders the region based on its label (NB: hard-coded TMHMM labels)
	 * @param g
	 * @param start
	 * @param end
	 * @param offset
	 * @param label
	 * @param frac
	 */
	private void renderRoundedRegion(Graphics g, int start, int end, int offset, String label, double frac) {
		if (label.toUpperCase().startsWith("INSIDE")) {
			g.setColor(Color.GRAY);
		} else if (label.startsWith("TM")) {
			g.setColor(Color.BLUE);
		} else {
			g.setColor(Color.DARK_GRAY);
		}
		g.fillRoundRect(200+(int)(start * frac), 2 + offset, 
				(int)((end - start) * frac), 17, 4, 4);
	}
}
