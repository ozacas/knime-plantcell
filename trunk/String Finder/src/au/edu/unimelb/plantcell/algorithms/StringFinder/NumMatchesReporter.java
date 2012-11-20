package au.edu.unimelb.plantcell.algorithms.StringFinder;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.IntCell;

public class NumMatchesReporter implements MatchReporter {

	@Override
	public DataCell report(FindGlobalNodeModel m, DataCell str_cell)
			throws Exception {
		return new IntCell(m.getNumMatches());
	}

}
