package au.edu.unimelb.plantcell.core.cells;

import org.knime.core.node.InvalidSettingsException;

/**
 * classes must implement this to be called to create a new track if it
 * does not already exist. The implementation should be fast if large-scale
 * processing is being done!
 * 
 * @author andrew.cassin
 *
 */
public interface TrackCreator {
	public Track createTrack(String name) throws InvalidSettingsException;

}
