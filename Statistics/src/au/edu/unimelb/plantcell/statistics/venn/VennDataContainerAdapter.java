package au.edu.unimelb.plantcell.statistics.venn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;

import au.edu.unimelb.plantcell.statistics.venn.VennOutputAdapter;

/**
 * Responsible for reporting the {@link VennModel} to a KNIME data container as specified in the constructor
 * 
 * @author acassin
 *
 */
public class VennDataContainerAdapter implements VennOutputAdapter {
		private final BufferedDataContainer bdc;
		private int m_rowid = 1;
		private final Map<List<String>,String> m_done_categories = new HashMap<List<String>,String>();
		
		public VennDataContainerAdapter(final BufferedDataContainer bdc) {
			assert(bdc != null);
			this.bdc = bdc;
		}

		public Map<List<String>,String> getDoneCategories() {
			return m_done_categories;
		}
		
		public void reset() {
			m_done_categories.clear();
		}
		
		private String asCategoryString(final List<String> l) {
			StringBuffer sb = new StringBuffer();
			int n = l.size();
			for (int i=0; i<n; i++) {
				sb.append(l.get(i));
				if (i<n-1) {
					sb.append(" AND ");
				}
			}
			return sb.toString();
		}
		
		@Override
		public void saveCategory(List<String> category_name, int count) {
			DataCell[] cells = new DataCell[2];
			cells[0]   = new StringCell(asCategoryString(category_name));
			cells[1]   = new IntCell(count);
			
			if (!m_done_categories.containsKey(category_name)) {
				String cat_row_name = "Category"+m_rowid++;

				bdc.addRowToTable(new DefaultRow(cat_row_name, cells));
				m_done_categories.put(category_name, cat_row_name);
			}
		}
}
	