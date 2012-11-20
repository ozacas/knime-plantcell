package au.edu.unimelb.plantcell.algorithms.orthology;

public class PutativeOrthologue {
	private final BLASTRecord m_a, m_b;
	
	public PutativeOrthologue(BLASTRecord a, BLASTRecord b) throws Exception {
		assert(a != null && b != null);
		m_a = a;
		m_b = b;
		
		// a & b must have the same accessions, so throw if not since thats programmer error
		if (!a.equals(b))
			throw new Exception("Programmer error: accessions not the same!");
	}

	public String getKey(boolean want_a) {
		BLASTRecord tmp = null;
		if (want_a) {
			tmp = m_a.isA() ? m_a : m_b; 
		} else {
			tmp = m_a.isA() ? m_b : m_a;
		}
		return tmp.getSubjectAccsn();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof PutativeOrthologue))
			return false;
		
		PutativeOrthologue b = (PutativeOrthologue) o;
		
		if ((this.m_a.equals(b.m_a) && this.m_b.equals(b.m_b)) || 
				(this.m_a.equals(b.m_b) && this.m_b.equals(b.m_a))) {
			return true;
		}
		
		// else...
		
		return false;
	}
	
	@Override 
	public int hashCode() {
		assert(m_a != null && m_b != null);
		return m_a.hashCode() * m_b.hashCode();
	}
}
