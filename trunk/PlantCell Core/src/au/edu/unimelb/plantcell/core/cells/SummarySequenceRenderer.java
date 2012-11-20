package au.edu.unimelb.plantcell.core.cells;

import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.MultiLineStringValueRenderer;

public class SummarySequenceRenderer extends MultiLineStringValueRenderer {
	private static final String DESCR = "Summary";
	
	
	public SummarySequenceRenderer(String description) {
		super(description);
	}

	/**
	 * for serialisation
	 */
	private static final long serialVersionUID = -1356987884400232879L;
	
	/**
	 * used by the {@link SequenceUtilityFactory} to render {@link SequenceValue}'s
	 */
	public static final DataValueRenderer INSTANCE = new SummarySequenceRenderer(DESCR);

	
	@Override
	public String getDescription() {
		return DESCR;
	}
	
	@Override
	public void setValue(Object o) {
		if (o instanceof SequenceValue) {
			StringBuilder sb = new StringBuilder(1024);
			SequenceValue sv = (SequenceValue) o;
			String descr = sv.getDescription();
			if (descr != null) {
				sb.append(sv.getID()+": "+descr);
			} else {
				sb.append(sv.getID());
			}
			sb.append('\n');
			sb.append("Length: "+sv.getStringValue().length());
			sb.append(' ');
			sb.append(sv.getSequenceType().name());
			sb.append('\n');
			int track_cnt = sv.countTracks();
			sb.append("Tracks: "+track_cnt);
			StringBuilder sb_tracks = new StringBuilder(1024);
			StringBuilder sb_annot = new StringBuilder(1024);

			sb.append('\n');
			if (track_cnt > 0) {
				sb.append("Totals by track: ");
				for (Track t : sv.getTracks()) {
					sb_tracks.append(t.getName());
					sb_tracks.append(" (");
					sb_tracks.append(t.countAnnotations());
					sb_tracks.append(") ");
					if (t.countAnnotations() > 0) {
						sb_annot.append(t.toString());
						sb_annot.append('\n');
					}
				}
				sb.append(sb_tracks.toString());
				sb.append('\n');
			}
			sb.append(sb_annot.toString());
			super.setValue(sb.toString());
		} else {
			super.setValue("");
		}
	}
}
