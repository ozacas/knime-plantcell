package au.edu.unimelb.plantcell.algorithms.StringFinder;

import java.util.Collections;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;

/**
 * Reports the distance from the end of the last match to the end of the string. A missing
 * cell is returned if there are no matches.
 * 
 * @author andrew.cassin
 *
 */
public class LastDistanceReporter implements MatchReporter {

	@SuppressWarnings("unchecked")
	@Override
	public DataCell report(FindGlobalNodeModel m, DataCell input_cell)
			throws Exception {
		List<Extent> extents = m.getMatchPos();
		if (extents == null || extents.size() < 1)
			return DataType.getMissingCell();
		Collections.sort(extents);
		
		return new IntCell(input_cell.toString().length() - extents.get(extents.size()-1).getEnd());
	}

}
