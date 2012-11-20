package au.edu.unimelb.plantcell.statistics.venn;

import java.util.ArrayList;
import java.util.List;

public class VennClassification {
	private VennClass m_b;
	private VennModel m_owner;
	
	public VennClassification(VennModel owner, int n) {
		assert(n >= 1 && owner != null);
		m_b = new VennClass(n);
		m_owner = owner;
	}
	
	public void setCategory(int i, boolean val) {
		m_b.setClass(i, val);
	}
	
	public void isNotInCategory(int i) {
		m_b.setClass(i, false);
	}
	
	public List<Integer> hasCategories() {
		ArrayList<Integer> ret = new ArrayList<Integer>();
		for (int i=0; i<m_b.getNumClasses(); i++) {
			if (m_b.isSet(i)) {
				ret.add(new Integer(i));
			}
		}
		return ret;
	}

	public boolean isSolelyCategory(int cat) {
		return m_b.isSolelyCategory(cat);
	}

	public boolean isCategory(VennClass bvec) {
		return m_b.isCategory(bvec);
	}
	
	public static String asCategoryString(VennModel vm, VennClass vc) {
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<vc.getNumClasses(); i++) {
			if (vc.isSet(i)) {
				if (sb.length() > 0) {
					sb.append(" AND ");
				}
				for (String cat : vm.getCategories()) {
					if (i == vm.getCategoryIndex(cat)) {
						sb.append(cat);
						break;
					}
				}
			}
		}
		
		return sb.toString();
	}
	
	public String getCategory() {
		return asCategoryString(m_owner, m_b);
	}

	public VennClass getVennClass() {
		return m_b;
	}
}
