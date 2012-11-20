package au.edu.unimelb.plantcell.core.cells;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.edu.unimelb.plantcore.core.regions.RegionInterface;

/**
 * Renders regions as boxes of height 20 using the 
 * 
 * @author andrew.cassin
 *
 */
public class StandardTrackRenderer implements TrackRendererInterface {
	public static final Font TRACK_NAME_FONT = new Font("Helvetica", Font.BOLD, 9);

	
	@Override
	public void paintLabel(final Graphics g, final String l, int offset) {
		g.setFont(TRACK_NAME_FONT);
		g.setColor(Color.BLACK);
		g.drawString(l, 3, 12+offset);
	}
	
	@Override
	public Dimension paintTrack(final Map<String,Integer> props, final Graphics g, final SequenceValue sv, final Track t) {
		int offset = props.get("offset");
		paintLabel(g, t.getName(), offset);
		
		SequenceAnnotation         sa = t.getAnnotation();
		int                     count = sa.countAnnotations();
		List<RegionInterface> regions = sa.getRegions();
		double frac = ((double)props.get("track.width")) / sv.getLength();
			
		for (RegionInterface r : regions) {
			int start = r.getZStart();
			int end   = r.getZEnd();
			
			renderRoundedRegion(g, start, end, offset, (count == 1) ? r.getLabel() : "", frac);
		}
		if (count > 1) {
			HashMap<String,Integer> m_freq = new HashMap<String,Integer>();
			for (RegionInterface r : regions) {
				Pattern p  = Pattern.compile("^(\\S+)");
				Matcher m  = p.matcher(r.getLabel());
				String keyword;
				if (m.find()) {
					keyword = m.group(1);
				} else {
					keyword = r.getLabel();
				}
				Integer i = m_freq.get(keyword);
				if (i == null) {
					m_freq.put(keyword, new Integer(1));
				} else {
					m_freq.put(keyword, new Integer(i.intValue()+1));
				}
			}
			StringBuilder sb = new StringBuilder();
			for (String keyword : m_freq.keySet()) {
				sb.append(keyword);
				sb.append(": ");
				sb.append(m_freq.get(keyword));
				sb.append(" ");
			}
			g.setColor(Color.BLACK);
			g.drawString(sb.toString(), 605, 12+offset);
		}
		
		return new Dimension(0, 20);
	}


	private void renderRoundedRegion(Graphics g, int start, int end, int offset, String label, double frac) {
		
		g.drawRoundRect(200+(int)(start * frac), 2 + offset, 
				(int)((end - start) * frac), 17, 4, 4);
		g.setColor(Color.DARK_GRAY);
		g.drawString(label, 605, 12+offset);
		g.setColor(Color.BLACK);
	}
	
}
