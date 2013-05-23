package au.edu.unimelb.plantcell.algorithms.StringFinder;

import java.util.Collections;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;

/**
 * Reports the distance to the start of the first match from the start of the string. If there are no matches
 * a missing cell is returned.
 * 
 * @author andrew.cassin
 *
 */
public class FirstDistanceReporter implements MatchReporter {

	@SuppressWarnings("unchecked")
	@Override
	public DataCell report(FindGlobalNodeModel m, DataCell input_cell)
			throws Exception {
		List<Extent> extents = m.getMatchPos();
		if (extents == null || extents.size() < 1)
			return DataType.getMissingCell();
		Collections.sort(extents);
		
		return new IntCell(extents.get(0).getStart());
	}

}
