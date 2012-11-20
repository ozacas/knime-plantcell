package au.edu.unimelb.plantcell.core.cells;

import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;


/**
 * Integrates sequence comparison into the KNIME framework. Actual sequence comparison
 * is delegated to the first sequence's implementation of the {@link Comparable} java interface.
 * 
 * @author andrew.cassin
 *
 */
public class SequenceValueComparator extends DataValueComparator {

	@Override
	protected int compareDataValues(final DataValue v1, final DataValue v2) {
		final SequenceValue s1 = (SequenceValue) v1;
		final SequenceValue s2 = (SequenceValue) v2;
		return s1.compareTo(s2);
	}

}
