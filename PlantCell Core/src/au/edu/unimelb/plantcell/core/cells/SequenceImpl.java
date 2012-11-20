package au.edu.unimelb.plantcell.core.cells;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;

/**
 * A sequence cannot contain whitespace (it will be automagically removed)
 * @author andrew.cassin
 *
 */
public class SequenceImpl implements SequenceValue {
	private String m_seq;
	private String m_id;		    // kept here rather than as an annotation
	private SequenceType m_type;    // ditto
	private List<Track> m_tracks;
	
	public SequenceImpl(SequenceType st, String accsn, String seq) throws InvalidSettingsException {
		m_type = st;
		m_id   = accsn;
		m_seq  = seq;
		m_tracks = null;
	}

	/**
	 * deep copy the sequence data from the input {@link SequenceValue}
	 * @param sv
	 */
	public SequenceImpl(SequenceValue sv) {
		m_type   = SequenceType.valueOf(sv.getSequenceType().name());
		m_id     = new String(sv.getID());
		m_seq    = new String(sv.getStringValue());
		m_tracks = null;
		if (sv.countTracks() > 0) {
			ensure_tracks();
			for (Track t : sv.getTracks()) {
				m_tracks.add(new Track(t));
			}
		}
	}

	private List<Track> ensure_tracks() {
		if (m_tracks != null)
			return m_tracks;
		m_tracks = new ArrayList<Track>();
		return m_tracks;
	}
	
	@Override
	public String toString() {
		return getStringValue();
	}
	
	@Override
	public String getStringValue() {
		return m_seq;
	}

	@Override
	public String getID() {
		return m_id;
	}

	@Override
	public int hashCode() {
		return m_seq.hashCode();
	}
	
	/**
	 * Sequences compare equal if their *IDs* match
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof SequenceValue) {
			SequenceValue sv2 = (SequenceValue) o;
			return (getID().equals(sv2.getID()));
		}
		
		return false;
	}
	
	/**
	 * Sequences ONLY compare equal if they are of the same type, length, and composed of the same
	 * residues in order. Other definitions of sequence equality will need to implemented separately.
	 * This implementation will throw a {@link ClassCastException} if the sequences are not of the same type.
	 */
	@Override
	public int compareTo(SequenceValue s2) {
		if (!getSequenceType().equals(s2.getSequenceType())) 
			throw new ClassCastException("Sequences cannot be compared: "+getSequenceType()+" versus "+s2.getSequenceType());
		int ret = getStringValue().compareTo(s2.getStringValue());
		return ret;
	}

	@Override
	public SequenceType getSequenceType() {
		return m_type;
	}

	@Override
	public int countTracks() {
		return (m_tracks == null) ? 0 : m_tracks.size();
	}

	@Override
	public Track addTrack(Track t) {
		assert(t != null);
		ensure_tracks();
		m_tracks.add(t);
		return t;
	}

	@Override
	public Iterable<Track> getTracks() {
		return (m_tracks != null) ? m_tracks : null;
	}

	protected int findTrack(String name) {
		int idx = 0;
		if (m_tracks == null)
			return -1;
		for (Track t : m_tracks) {
			if (t.hasName(name)) {
				return idx;
			}
			idx++;
		}
		
		return -1;
	}
	
	public void removeTrackByName(String name) {
		int idx = findTrack(name);
		if (idx >= 0) {
			m_tracks.remove(idx);
		}
	}

	@Override
	public void removeTrack(Track t) {
		m_tracks.remove(t);
	}

	@Override
	public Track getTrackByName(String name, TrackCreator tc) throws InvalidSettingsException {
		int idx = findTrack(name);
		if (idx >= 0) {
			return m_tracks.get(idx);
		}
		if (tc == null)
			return null;
		Track t = tc.createTrack(name);
		addTrack(t);
		return t;
	}

	@Override
	public Track getTrackByType(AnnotationType at) {
		if (m_tracks == null)
			return null;
		for (Track t : m_tracks) {
			if (t.hasType(at))
				return t;
		}
		return null;
	}

	@Override
	public void setID(String accsn) {
		assert(accsn != null);
		m_id = accsn;
	}

	@Override
	public void setSequenceType(SequenceType st) {
		assert(st != null);
		m_type = st;
	}

	@Override
	public int getLength() {
		return m_seq.length();
	}

	@Override
	public boolean isValid() {
		// not yet implemented so...
		return true;
	}

	@Override
	public void addComment(Comment c) throws InvalidSettingsException {
		assert(c != null);
		Track t = getTrackByName("Summary", new TrackCreator() {

			@Override
			public Track createTrack(String name)
					 throws InvalidSettingsException {
				Track t = new Track(name);
				t.addAnnotation(new CommentsAnnotation());
				return t;
			}
			
		});
		CommentsAnnotation ca = (CommentsAnnotation) t.getAnnotation();
		ca.addComment(c);
	}

	@Override
	public Track addTrack(String trackName, TrackCreator tc) {
		Track t = null;
		removeTrackByName(trackName);
		if (tc != null) {
			try {
				t = tc.createTrack(trackName);
			} catch (InvalidSettingsException ise) {
				ise.printStackTrace();
			}
		}
		if (t == null) {
			t = new Track(trackName);
		}
		return addTrack(t);
	}

	@Override
	public boolean hasDescription() {
		String descr = getDescription();
		return (descr != null && descr.length() > 0);
	}

	@Override
	public String getDescription() {
		Track t = getTrackByType(AnnotationType.COMMENTS);	 // TODO: only consider the first comment track
		if (t == null)
			return null;
		CommentsAnnotation ca = (CommentsAnnotation) t.getAnnotation();
		for (Comment c : ca.getComments()) {
			if (c.hasType(CommentType.Description))
				return c.getText();
		}
		return null;	// no suitable description
	}
}
