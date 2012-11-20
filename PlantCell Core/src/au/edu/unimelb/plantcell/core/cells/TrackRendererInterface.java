package au.edu.unimelb.plantcell.core.cells;

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Map;

/**
 * Responsible for rendering a single track. The contract is as follows:
 * 1) a track must render the specified track using the specified graphics instance
 * 2) a track must inform the framework of the dimensions of the track via
 *    getWidth() and getHeight()
 * 3) limits for drawing are imposed by the framework calling setLimits() before
 *    painting the track
 *    
 * @author andrew.cassin
 *
 */
public interface TrackRendererInterface {

	/**
	 * Renders the label for the track (usually the track name)
	 * @param g
	 * @param l
	 * @param offset
	 */
	public void paintLabel(final Graphics g, final String l, int offset);
	
	/**
	 * Draw the track t, for sequence sv, using properties props onto canvas g.
	 * 
	 * @param props
	 * @param g
	 * @param sv
	 * @param t
	 * @return the dimensions of the painted track
	 */
	public Dimension paintTrack(final Map<String,Integer> props, final Graphics g, final SequenceValue sv, final Track t);
}
