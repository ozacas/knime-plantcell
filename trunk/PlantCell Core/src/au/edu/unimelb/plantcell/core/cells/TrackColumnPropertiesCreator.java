package au.edu.unimelb.plantcell.core.cells;

import java.util.HashMap;

import org.knime.core.data.DataColumnProperties;

/**
 * A key class that is responsible for ensuring the list of tracks is available for
 * all the node dialogs to see. It does this by keeping a column property with all known tracks in the column.
 * When working with these columns eg. creating a {@link DataTableSpec} you'll need this class to correctly 
 * setup the column properties for the node dialogs to use.
 * 
 * @author andrew.cassin
 *
 */
public class TrackColumnPropertiesCreator {
   
    // internal state built during construction
	private DataColumnProperties m_props;
	
	/**
	 * 
	 * @param isp may be null
	 * @param new_track may not be null
	 */
	public TrackColumnPropertiesCreator(DataColumnProperties isp, Track new_track) {
		this(isp, new Track[] { new_track });
	}

	public TrackColumnPropertiesCreator(DataColumnProperties isp, Track[] new_tracks) {
		assert(new_tracks != null && new_tracks.length > 0);
		HashMap<String,String> map = new HashMap<String,String>();
		for (Track new_track : new_tracks) {
			map.put(Track.PLANTCELL_TRACK_PREFIX+new_track.getName(), new_track.asText());
		}
		if (isp == null)
			isp = new DataColumnProperties();
		m_props = isp.cloneAndOverwrite(map);
	}
	
	/**
	 * return the final properties object associated with the column in the {@link DataTableSpec}
	 * @return
	 */
	public DataColumnProperties getProperties() {
		return m_props;
	}
}
