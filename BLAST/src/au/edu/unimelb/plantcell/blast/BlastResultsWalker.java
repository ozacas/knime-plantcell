package au.edu.unimelb.plantcell.blast;

import java.util.List;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.regions.BlastHitRegion;

public interface BlastResultsWalker {
	/**
	 * Invoked by the blast result code when a set of hits associated with the specified
	 * <code>hit</code> has been computed.
	 * 
	 * @param hit
	 * @param hits
	 */
	public void hit(SequenceValue hit, List<BlastHitRegion> hits);
	
	/**
	 * Called by the blast code when a sequence is found without hits
	 */
	public void nohit(SequenceValue hit);
}
