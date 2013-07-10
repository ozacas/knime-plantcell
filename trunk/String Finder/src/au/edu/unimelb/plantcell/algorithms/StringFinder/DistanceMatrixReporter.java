package au.edu.unimelb.plantcell.algorithms.StringFinder;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.def.StringCell;

/**
 * Reports a distance matrix of each match in the query sequence (all versus all except self).
 * The matrix is reported as a collection where each value is match1-match2=distance where
 * distance is reported in terms of the length of the query sequence.
 * 
 * @author andrew.cassin
 *
 */
public class DistanceMatrixReporter implements MatchReporter {
	private boolean report_percent = false;
	
	public DistanceMatrixReporter(String task) {
		report_percent = task.indexOf("%") >= 0;
	}
	
	@Override
	public DataCell report(FindGlobalNodeModel m, DataCell input_cell)
			throws Exception {
		List<Extent> extents = m.getMatchPos();
		String input = input_cell.toString();
		if (extents == null || extents.size() < 1)
			return DataType.getMissingCell();
		
		List<StringCell> cells = new ArrayList<StringCell>();
		for (Extent i : extents) {
			for (Extent j : extents) {
				if (!i.equals(j)) {
					String i_match = input.substring(i.getStart(), i.getEnd());
					String j_match = input.substring(j.getStart(), j.getEnd());
					int distance = Math.abs(i.getStart() - j.getStart());
					double dist=0.0d;
					if (input.length() > 0)
						dist = report_percent ? ((double)distance / input.length())*100.0d : distance;
					cells.add(new StringCell(i_match+"-"+j_match+"="+dist));
				}
			}
		}
		
		return CollectionCellFactory.createListCell(cells);
	}

}
