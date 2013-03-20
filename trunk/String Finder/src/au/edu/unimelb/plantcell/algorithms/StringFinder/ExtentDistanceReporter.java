package au.edu.unimelb.plantcell.algorithms.StringFinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.def.IntCell;

/**
 * Reports the distances between the start positions of each match
 * @author andrew.cassin
 *
 */
public class ExtentDistanceReporter implements MatchReporter {

	@SuppressWarnings("unchecked")
	@Override
	public DataCell report(FindGlobalNodeModel m, DataCell input_cell)
			throws Exception {
		List<Extent> extents = m.getMatchPos();
		Collections.sort(extents);
		
		ArrayList<DataCell> ret = new ArrayList<DataCell>();
		for (int i=1; i<extents.size(); i++) {
			int distance = extents.get(i).getStart() - extents.get(i-1).getStart();
			ret.add(new IntCell(distance));
		}
		return (ret.size() > 0) ? CollectionCellFactory.createListCell(ret) : DataType.getMissingCell();
	}

}
