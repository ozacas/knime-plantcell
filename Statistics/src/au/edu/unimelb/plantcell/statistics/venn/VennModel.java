package au.edu.unimelb.plantcell.statistics.venn;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;

/**
 * Implements a simple-minded venn diagram with <code>n</code> pies and the specified
 * set of values in the group-by column. 
 * 
 * @author andrew.cassin
 *
 */
public class VennModel {
	private final HashMap<String,Integer>             m_categories = new HashMap<String,Integer>();
	private final HashMap<String, VennClassification> m_v          = new HashMap<String, VennClassification>();
	private int   m_n;
	
	/**
	 * Sole constructor. The set of permitted values in the group-by column is
	 * required as is the number of pies (usually 3 or 4)
	 * @param n the view can only display between 2 to five wedges so its recommended that 2 <= n <= 5
	 * @param categories
	 */
	public VennModel(int n, Set<String> categories) {
		assert(n >= 1 && categories != null && categories.size() == n);
		
		int i=0;
		m_n = n;
		for (String category : categories) {
			m_categories.put(category, new Integer(i++));
		}
	}
	
	/**
	 * Adds the specified set of values to the specified category, adjusting
	 * for previously-known occurrences of the value as appropriate. If the 
	 * specified category is not known an exception will be thrown.
	 */
	public void add(String category, Collection<String> values) throws Exception {
		assert(m_categories.containsKey(category) && values != null);
		Integer i = m_categories.get(category);
		if (i == null) {
			throw new Exception("Unknown category: "+category);
		}
		for (String v : values) {
			if (m_v.containsKey(v)) {
				VennClassification vc = m_v.get(v);
				vc.setCategory(i.intValue(), true);
			} else {
				VennClassification vc = new VennClassification(this, m_n);
				vc.setCategory(i.intValue(), true);
				m_v.put(v, vc);
			}
		}
	}


	
	/**
	 * NB: must correspond to output spec of node!
	 */
	public void outputToAdapter(final VennOutputAdapter adapter) {
		assert(adapter != null);
		VennClass bvec = new VennClass(m_n);
		
		adapter.reset();
		outputRecurse(1, bvec, adapter);
		
		bvec.setClass(0, true);
		outputRecurse(1, bvec, adapter);
	}

	private void outputRecurse(int n, final VennClass bvec, final VennOutputAdapter adapter) {
		assert(bvec != null && adapter != null);
		if (n > m_n)
			return;
		
		// not one category? ignore...
		boolean gotone = bvec.hasAny();
		
		// output current bvec
		if (gotone) {
			int count = 0;
			for (String v : m_v.keySet()) {
				VennClassification vc = m_v.get(v);
				if (vc.isCategory(bvec)) {
					count++;
				}
			}
			List<String> l = VennClassification.asCategoryList(this, bvec);
			adapter.saveCategory(l, count);
		}
		
		// recursion step
		if (n < bvec.getNumClasses()) {
			bvec.setClass(n, false);
			outputRecurse(n+1, bvec, adapter);
			
			// NB: bvec[n] is true
			bvec.setClass(n, true);
			outputRecurse(n+1, bvec, adapter);
		}
	}

    /**
     * Adds classification for each value encountered so that the user can lookup values of interest to see where in the
     * model they occur.
     * 
     * @param c
     */
	public void outputValuesToContainer(final BufferedDataContainer c, final Map<List<String>, String> map) {
		assert(c != null && map != null);
		int row_id = 1;
		
		for (String v : m_v.keySet()) {
			VennClassification vc = m_v.get(v);
			List<String> category = vc.getCategory();
			String cat_printable_name = map.get(category);
			DataCell[] cells = new DataCell[2];
			cells[0] = new StringCell(cat_printable_name);
			cells[1] = new StringCell(v);
			c.addRowToTable(new DefaultRow("v"+row_id++, cells));
		}
	}

	public Set<String> getCategories() {
		return m_categories.keySet();
	}

	public int getCategoryIndex(String cat) {
		Integer i = m_categories.get(cat);
		if (i == null) 
			return -1;
		return i.intValue();
	}
	
	
	/**
	 * Returns a map of the venn counts for each class:
	 * { class1 => count1, class2 => count2, class3 => count3 ... }
	 * 
	 * The class names are determined by the user from supplied data. Should not be called until after the
	 * data has been added to the model.
	 */
	public Map<List<String>,Integer> getVennMap() {
		final HashMap<List<String>,Integer> ret = new HashMap<List<String>,Integer>();
		VennOutputAdapter voa = new VennOutputAdapter() {
			private final Map<List<String>,String> done_categories = new HashMap<List<String>,String>();
			
			@Override
			public void saveCategory(final List<String> category, int count) {
				ret.put(category, new Integer(count));
			}

			@Override
			public Map<List<String>, String> getDoneCategories() {
				return done_categories;
			}

			@Override
			public void reset() {
				done_categories.clear();
			}
			
		};
		outputToAdapter(voa);
		return ret;
	}
}
