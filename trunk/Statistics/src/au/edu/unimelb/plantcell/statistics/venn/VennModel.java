package au.edu.unimelb.plantcell.statistics.venn;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;

/**
 * Implements a simple-mind venn diagram with <code>n</code> pies and the specified
 * set of values in the group-by column. 
 * 
 * @author andrew.cassin
 *
 */
public class VennModel {
	private final HashMap<String,Integer>            m_categories = new HashMap<String,Integer>();
	private final HashMap<String, VennClassification> m_v = new HashMap<String, VennClassification>();
	private int   m_n;
	private static int m_rowid;
	private static HashMap<String,String> m_done_categories = new HashMap<String,String>();
	
	/**
	 * Sole constructor. The set of permitted values in the group-by column is
	 * required as is the number of pies (usually 3 or 4)
	 * @param n
	 * @param categories
	 */
	public VennModel(int n, Set<String> categories) {
		assert(n >= 1 && categories != null && categories.size() == n);
		
		int i=0;
		m_n = n;
		for (String category : categories) {
			m_categories.put(category, new Integer(i++));
		}
		m_rowid = 1;
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
				m_v.put(v , vc);
			}
		}
	}


	/**
	 * NB: must correspond to output spec of node!
	 */
	public void outputToContainer(BufferedDataContainer container) {
		VennClass bvec = new VennClass(m_n);
		
		m_done_categories.clear();
		outputRecurse(1, bvec, container);
		
		bvec.setClass(0, true);
		outputRecurse(1, bvec, container);
	}


	private void outputRecurse(int n, VennClass bvec,
			BufferedDataContainer container) {
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
			
			DataCell[] cells = new DataCell[2];
			String cat = VennClassification.asCategoryString(this, bvec);
			cells[0]   = new StringCell(cat);
			cells[1]   = new IntCell(count);
			
			if (!m_done_categories.containsKey(cat)) {
				String cat_printable_name = "Category"+m_rowid++;
				container.addRowToTable(new DefaultRow(cat_printable_name, cells));
				m_done_categories.put(cat, cat_printable_name);
			}
		}
		
		// recursion step
		if (n < bvec.getNumClasses()) {
			bvec.setClass(n, false);
			outputRecurse(n+1, bvec, container);
			
			// NB: bvec[n] is true
			bvec.setClass(n, true);
			outputRecurse(n+1, bvec, container);
		}
	}

    /**
     * Adds counts to the outport 1. 
     * @param c
     */
	public void outputValuesToContainer(BufferedDataContainer c) {
		//HashMap<VennClass, String> class2category = new HashMap<VennClass,String>();
		int row_id = 1;
		
		for (String v : m_v.keySet()) {
			VennClassification vc = m_v.get(v);
			String category = vc.getCategory();
			String cat_printable_name = m_done_categories.get(category);
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
}
