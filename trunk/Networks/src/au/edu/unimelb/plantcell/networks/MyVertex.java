package au.edu.unimelb.plantcell.networks;

public class MyVertex {
	private String m_name;
	
	/**
	 * 
	 * @param name should be unique among vertices in a graph and never null or zero length
	 */
	public MyVertex(String name) {
		assert(name != null && name.length() > 0);
		m_name = name;
	}
	
	@Override
	public String toString() {
		return m_name;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof MyVertex)) {
			return false;
		}
		
		return ((MyVertex)o).m_name.equals(this.m_name);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int hashCode() {
		return m_name.hashCode();
	}
}
