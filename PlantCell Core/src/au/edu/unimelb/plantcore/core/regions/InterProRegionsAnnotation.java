package au.edu.unimelb.plantcore.core.regions;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.def.StringCell;
import org.osgi.framework.Bundle;

import au.edu.unimelb.plantcell.core.cells.AnnotationType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackRendererInterface;


/**
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class InterProRegionsAnnotation extends RegionsAnnotation implements TrackRendererInterface {
	// legend to display next to interpro track
	private static ImageIcon m_interpro_legend = null;
	static {
		try {
			Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.core");
  			URL u = FileLocator.find(bundle, new Path("/icons/interpro-legend.png"), null);
			m_interpro_legend = new ImageIcon(u);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	// interpro colours
	private final static HashMap<String,Color> interpro_colors = new HashMap<String,Color>();
	static {
		interpro_colors.put("prodom", new Color(102,153,255));		// colors are chosen to match EBI's current colour scheme as at 15th Aug 2012
		interpro_colors.put("hamap", new Color(102,255,255));
		interpro_colors.put("prosite", new Color(255,204,51));
		interpro_colors.put("prints", new Color(51,204,51));
		interpro_colors.put("pir", new Color(217,119,249));
		interpro_colors.put("superfamily", Color.BLACK );
		interpro_colors.put("pfam", new Color(102, 51, 255));
		interpro_colors.put("signalp", new Color(217,119,249));
		interpro_colors.put("smart", new Color(206, 0, 49));
		interpro_colors.put("tmhmm", new Color(46,140,12));
		interpro_colors.put("tigrfams", new Color(46, 140, 12));
		interpro_colors.put("panther", new Color(153, 102, 51));
		interpro_colors.put("profile", new Color(255,153,51));
		interpro_colors.put("gene3d", new Color(119, 2, 221));
	};
	
	
	@Override
	public AnnotationType getAnnotationType() {
		return AnnotationType.INTERPRO_REGIONS;
	}
	
	/**
	 * Co-ordinates of predictions start at 1 for InterPro v4
	 */
	@Override
	public int getOffset() {
		return 1;
	}
	
	@Override
	public List<DataColumnSpec> asColumnSpec(String prefix) {
		List<DataColumnSpec> cols = super.asColumnSpec(prefix);
		cols.add(new DataColumnSpecCreator(prefix+": InterPro ID", StringCell.TYPE).createSpec());
		cols.add(new DataColumnSpecCreator(prefix+": Evidence", StringCell.TYPE).createSpec());
		cols.add(new DataColumnSpecCreator(prefix+": Status", StringCell.TYPE).createSpec());
		cols.add(new DataColumnSpecCreator(prefix+": Database", StringCell.TYPE).createSpec());
		cols.add(new DataColumnSpecCreator(prefix+": Database ID", StringCell.TYPE).createSpec());
		cols.add(new DataColumnSpecCreator(prefix+": Name", StringCell.TYPE).createSpec());
		return cols;
	}
	
	@Override
	public Dimension paintTrack(final Map<String,Integer> props, final Graphics g, 
			final SequenceValue sv, final Track t) {
		int offset = props.get("offset");
		int start_offset = offset;
		double frac = ((double)props.get("track.width")) / sv.getLength();
		paintLabel(g, t.getName(), offset);
		
		// each interpro region identifies the member database that gave rise to it. We
		// render each member database in a separate line
		HashMap<String,List<InterProRegion>> db2regions = new HashMap<String,List<InterProRegion>>();
		// HACK BUG TODO: not all colours for annotations are correctly matched to interpro v5, acassin@unimeb.edu.au, 7th march 2014
		for (RegionInterface r : t.getAnnotation().getRegions()) {
			InterProRegion ipr = (InterProRegion) r;
			String db = ipr.getDatabase().toLowerCase().trim();
			if (!db2regions.containsKey(db)) {
				db2regions.put(db, new ArrayList<InterProRegion>());
			}
			db2regions.get(db).add(ipr);
		}
		
		// user convenience: member databases are sorted before display
		ArrayList<String> keys = new ArrayList<String>();
		keys.addAll(db2regions.keySet());
		Collections.sort(keys);
		
		// display hits from each member database by line
		g.drawImage(m_interpro_legend.getImage(), 605, offset-3, null);
		for (String key : keys) {
			Color c = interpro_colors.get(key);
			if (c == null) {
				//Logger.getAnonymousLogger().info(key);
				c = Color.BLACK;
			}
			g.setColor(c);
			for (InterProRegion ipr : db2regions.get(key)) {
				int start = ipr.getZStart();
				int end   = ipr.getZEnd();
				render_hit(g, start, end, offset, frac);
			}
			offset += 5;
		}
		g.setColor(Color.BLACK);
		
		int height = offset - start_offset;
		if (height < 20)
			height = 20;
		return new Dimension(0, height);
	}
	
	private void render_hit(Graphics g, int start, int end, int offset, double frac) {
		g.fillRect(200+(int)(start * frac), offset, 
				(int) ((end - start) * frac), 3);
	}
}
