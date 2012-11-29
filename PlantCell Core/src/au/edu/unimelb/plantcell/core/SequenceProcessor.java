package au.edu.unimelb.plantcell.core;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

public interface SequenceProcessor {

	/**
	 * Called during iteration over the input table, this method must process the
	 * sequence, perform any cleanup or validation desired and return the result
	 * ready for the caller (a node implementation). If null is returned the sequence
	 * will be skipped.
	 *
	 * @return the sequence to provide to the node. Usually this will be the input sequence
	 */
	public SequenceValue process(SequenceValue sv);
}
