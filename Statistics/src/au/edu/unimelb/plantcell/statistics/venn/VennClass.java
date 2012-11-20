package au.edu.unimelb.plantcell.statistics.venn;

public class VennClass {
	private boolean[] m_b;
	
	public VennClass(int n) {
		assert(n > 0);
		m_b = new boolean[n];
		for (int i=0; i<n; i++) {
			m_b[i] = false;
		}
	}

	public void setClass(int i, boolean val) {
		assert(i < m_b.length);
		m_b[i] = val;
	}

	public int getNumClasses() {
		return m_b.length;
	}

	public boolean isSet(int i) {
		assert(i>=0 && i<getNumClasses());
		return (m_b[i]);
	}

	public boolean isNotSet(int i) {
		return (!isSet(i));
	}

	public boolean hasAny() {
		for (int i=0; i<m_b.length; i++) {
			if (m_b[i]) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isSolelyCategory(int cat) {
		for (int i=0; i<getNumClasses(); i++) {
			if ((isSet(i) && i != cat) || (i == cat && isNotSet(i))) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isCategory(VennClass bvec) {
		for (int i=0; i<bvec.getNumClasses(); i++) {
			if (bvec.isSet(i) != isSet(i))
				return false;
		}
		return true;
	}
	
	/**
	 * Two VennClass'es are considered equal if their bit vectors match, false otherwise.
	 */
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof VennClass)) {
			return false;
		}
		VennClass vc2 = (VennClass) o;
		if (getNumClasses() != vc2.getNumClasses()) {
			return false;
		} else {
			for (int i=0; i<getNumClasses(); i++) {
				if (m_b[i] != vc2.m_b[i]) {
					return false;
				}
			}
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		int ret = 0;
		int mult= 1;
		for (int i=0; i<getNumClasses(); i++) {
			if (m_b[i]) {
				ret += mult;
			}
			mult *= 2;
		}
		return ret;
	}
}
