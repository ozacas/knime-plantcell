package au.edu.unimelb.plantcell.core.regions;

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
 * Specification of annotations for phobius results
 * @author andrew.cassin
 *
 */
public class PhobiusRegionsAnnotation extends RegionsAnnotation implements TrackRendererInterface {
	// legend to display next to interpro track
	private static ImageIcon m_legend = null;
	static {
		try {
			Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.core");
  			URL u = FileLocator.find(bundle, new Path("/icons/phobius_legend.png"), null);
			m_legend = new ImageIcon(u);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public AnnotationType getAnnotationType() {
		return AnnotationType.PHOBIUS_REGIONS;
	}
	
	/**
	 * Co-ordinates of predictions start at 1 for phobius?
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
	 * Renders the region based on its label (NB: hard-coded EBI Phobius labels)
	 * @param g
	 * @param start
	 * @param end
	 * @param offset
	 * @param label
	 * @param frac
	 */
	private void renderRoundedRegion(Graphics g, int start, int end, int offset, String label, double frac) {
		if (label.startsWith("SIGNAL")) {
			g.setColor(Color.RED);
		} else if (label.startsWith("TRANS")) {
			g.setColor(Color.BLUE);
		} else {
			g.setColor(Color.DARK_GRAY);
		}
		g.fillRoundRect(200+(int)(start * frac), 2 + offset, 
				(int)((end - start) * frac), 17, 4, 4);
		//g.drawString(label, 605, 12+offset);
		//g.setColor(Color.BLACK);
	}
}
