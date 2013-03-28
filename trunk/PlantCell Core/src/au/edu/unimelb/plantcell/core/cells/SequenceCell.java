package au.edu.unimelb.plantcell.core.cells;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;



/**
 * An abstract container for a biological sequence. It is up to the {@link SequenceImpl}
 * to determine how to represent and serialise the sequence so that it is persisted
 * for the user. This is all arranged carefully, so be careful not to break backward compatibility
 * or a particular node requirement!
 * 
 * @author andrew.cassin
 *
 */
public class SequenceCell extends DataCell implements SequenceValue, StringValue {
	/**
	 * for serialisation
	 */
	private static final long serialVersionUID = -6221828202181296617L;
	
	/**
	 * The only persisted member is this. All the methods in SequenceValue just delegate
	 * to the implementation. There may be subclasses of this at a future date, but for now...
	 */
	private SequenceImpl m_sequence;
	
	/**
	 * Accessed by KNIME core using reflection to construct the cell
	 */
    public static final DataType TYPE = DataType.getType(SequenceCell.class);
    private static final SequenceImplSerializer<SequenceCell> SERIALIZER = new SequenceImplSerializer<SequenceCell>();
    

    /**
     * Commonly used constructor for a new sequence cell (requires a sequence type, ID and the sequence data).
     * @param st
     * @param accsn
     * @param seq
     * @throws InvalidSettingsException an exception may be thrown if {@link} 
     */
	public SequenceCell(SequenceType st, String accsn, String seq) throws InvalidSettingsException {
		m_sequence = new SequenceImpl(st, accsn, seq);
	}

	/**
	 * Constructs a new {@link SequenceCell} from the specified cell. If the supplied cell is a sequence,
	 * a copy is made. Otherwise the cell's string content is used to create a sequence of unknown type.
	 * 
	 * @param dc
	 * @throws InvalidSettingsException if the cell is null
	 */
	public SequenceCell(DataCell dc) throws InvalidSettingsException {
		if (dc == null)
			throw new InvalidSettingsException("Cannot create a sequence from null data!");
		if (dc instanceof SequenceCell) {
			SequenceValue sv = (SequenceValue) dc;
			m_sequence = new SequenceImpl(sv);
		} else {
			m_sequence = new SequenceImpl(SequenceType.UNKNOWN, "", dc.toString());
		}
	}
	
	/**
	 * A {@link SequenceCell} has a preferred value class: {@link SequenceValue} which we establish here.
	 * Great care must be taken with this method, it has significant impacts right across the entire KNIME platform.
	 */
	public static final Class<? extends DataValue> getPreferredValueClass() {
		return SequenceValue.class;
	}
	
	/**
	 * Constructs a new {@link SequenceImpl} from the specified {@link SequenceValue} and initialises
	 * the cell to the new constructed <code>SequenceImpl</code>. An exception is thrown if <code>null</code> is supplied
	 * 
	 * @param sv
	 * @throws InvalidSettingsException
	 */
	public SequenceCell(SequenceValue sv) throws InvalidSettingsException {
		if (sv == null)
			throw new InvalidSettingsException("No sequence to construct!");
		m_sequence = new SequenceImpl(sv);
	}
	
	public static final DataCellSerializer<SequenceCell> getCellSerializer() {
		return SERIALIZER;
	}
	
	public String getStringValue() {
		return m_sequence.toString();
	}

	@Override
	public String toString() {
		return getStringValue();
	}

	@Override
	protected boolean equalsDataCell(DataCell dc) {
		if (!(dc instanceof SequenceCell))
			return false;
		return (m_sequence.compareTo((SequenceValue)dc) == 0);
	}
	
	@Override
	public int hashCode() {
		return m_sequence.hashCode();
	}

	@Override
	public String getID() {
		return m_sequence.getID();
	}

	@Override
	public int compareTo(SequenceValue s2) {
		return m_sequence.compareTo(s2);
	}

	@Override
	public SequenceType getSequenceType() {
		return m_sequence.getSequenceType();
	}

	@Override 
	public int countTracks() {
		return m_sequence.countTracks();
	}
	
	@Override
	public Track addTrack(Track t) {
		m_sequence.addTrack(t);
		return t;
	}

	@Override 
	public Track addTrack(String name, TrackCreator tc) {
		return m_sequence.addTrack(name, tc);
	}
	
	@Override
	public Iterable<Track> getTracks() {
		return m_sequence.getTracks();
	}

	@Override
	public void removeTrackByName(String name) {
		m_sequence.removeTrackByName(name);
	}

	@Override 
	public void removeTrack(Track t) {
		m_sequence.removeTrack(t);
	}
	
	@Override
	public boolean isValid() {
		return m_sequence.isValid();
	}
	
	@Override
	public Track getTrackByType(AnnotationType at) {
		return m_sequence.getTrackByType(at);
	}

	@Override
	public Track getTrackByName(String name, TrackCreator tc) throws InvalidSettingsException {
		return m_sequence.getTrackByName(name, tc);
	}

	@Override
	public void setID(String accsn) {
		m_sequence.setID(accsn);
	}

	@Override
	public void setSequenceType(SequenceType st) {
		m_sequence.setSequenceType(st);
	}

	@Override
	public int getLength() {
		return m_sequence.getLength();
	}

	@Override
	public void addComment(Comment c) throws InvalidSettingsException {
		m_sequence.addComment(c);
	}

	@Override
	public boolean hasDescription() {
		return m_sequence.hasDescription();
	}

	@Override
	public String getDescription() {
		return m_sequence.getDescription();
	}

}
