package au.edu.unimelb.plantcell.algorithms.StringFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.def.StringCell;

public class SearchStringReporter implements MatchReporter {
	private boolean m_want_frequency;
	
	public SearchStringReporter(String task) {	
		m_want_frequency = task.startsWith("Pattern distribution");
	}
	
	@Override
	public DataCell report(FindGlobalNodeModel m, DataCell str_cell) throws Exception {
		List<String> patterns = m.getMatchingPatterns();
		Map<String,Integer> freq = m.getMatchPatternFrequency();
		if (patterns == null || patterns.size() < 1) 
			return DataType.getMissingCell();
		ArrayList<StringCell> vec = new ArrayList<StringCell>();
		for (String p : patterns) {
			if (m_want_frequency) 
				vec.add(new StringCell(p+"="+freq.get(p).intValue()));
			else
				vec.add(new StringCell(p));
		}
		return CollectionCellFactory.createSetCell(vec);
	}

}
