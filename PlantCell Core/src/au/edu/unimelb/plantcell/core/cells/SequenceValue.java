package au.edu.unimelb.plantcell.core.cells;

import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;

/**
 * All cells related to storage of sequence data must implement at least this interface to ensure
 * it works within the PlantCell and KNIME frameworks.
 * 
 * @author andrew.cassin
 *
 */
public interface SequenceValue extends DataValue, Comparable<SequenceValue> {
	 /** Meta information to this value type.
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = new SequenceUtilityFactory();

    /**
     * @return A single-letter representation of DNA/RNA/AA sequences
     */
    public String getStringValue();
    
    /**
     * @return An identifier for the sequence (eg. accession from a fasta file). Alternate
     * @return accessions can be represented as an annotation
     */
    public String getID();

    @Override
	public int compareTo(SequenceValue s2);

    /**
     * DNA/RNA/AA or unknown?
     * @return
     */
	public SequenceType getSequenceType();
	
	/**
	 * How many annotation tracks are defined for this SequenceValue?
	 */
	public int countTracks();

	/**
	 * List of tracks in order created. Will return <code>null</code> if no tracks have been defined.
	 */
	public Iterable<Track> getTracks();

	/**
	 * Add the specified track as the last track. Must not be <code>null</code>
	 */
	public Track addTrack(Track t);
	
	/**
	 * Creates a new track, removing an existing track if necessary. The specified TrackCreator is called
	 * to create the track if it is not <code>null</code>. The track is added as the last track.
	 * 
	 * @param trackName
	 * @param trackCreator
	 */
	public Track addTrack(String trackName, TrackCreator trackCreator);
	
	/**
	 * Add a comment annotation to the specified sequence
	 */
	public void addComment(Comment c) throws InvalidSettingsException;
	
	/**
	 * Does the specified sequence pass validity checks?
	 */
	public boolean isValid();
	
	/**
	 * Remove the specified track (and thus all annotations on it)
	 * 
	 */
	public void removeTrackByName(String name);
	public void removeTrack(Track t);
	
	/**
	 * Returns the {@link Track} instance for the track of the specified name.
	 * If <code>tc</code> is not null it will be invoked to create a new track as needed
	 */
	public Track getTrackByName(String name, TrackCreator tc) 
					throws InvalidSettingsException;

	/**
	 * Returns the first Track of the specified type
	 */
	public Track getTrackByType(AnnotationType at);

	/**
	 * Changes the specified sequence to the specified ID (which should not be null)
	 * @param accsn
	 */
	public void setID(String accsn);

	/**
	 * Changes the sequence type to the specified type (eg. AA, DNA/RNA etc.) It is recommended
	 * to call this immediately after construction but before any other SequenceValue methods.
	 * It is up to the caller to ensure the state of SequenceValue is appropriate to the type
	 * of SequenceType set.
	 * 
	 * @param st
	 */
	public void setSequenceType(SequenceType st);

	/**
	 * Returns the length (incl. gaps, stop codons etc.) in residues of the sequence. This
	 * should not be taken as authoritative unless you have squeezed out non-conforming
	 * characters due prior to this call.
	 * 
	 * @return
	 */
	public int getLength();

	/**
	 * Does the sequence have any textual description (checks for a primary descriptor)
	 * and then any suitable alternates from available tracks
	 * 
	 * @return
	 */
	public boolean hasDescription();

	/**
	 * Returns null if the sequence has no description, otherwise the first available description is returned
	 * @return
	 */
	public String getDescription();
	

}
