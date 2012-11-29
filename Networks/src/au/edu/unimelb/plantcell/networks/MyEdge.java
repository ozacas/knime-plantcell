package au.edu.unimelb.plantcell.networks;

/**
 * 
 * @author andrew.cassin
 *
 */
public class MyEdge {
	private double m_distance;
	private MyVertex m_src, m_dst;
	
	public MyEdge(MyVertex src, MyVertex dest) {
		this(src, dest, 0.0);
	}
	
	public MyEdge(MyVertex src, MyVertex dst, double distance) {
		assert(src != null && dst != null);
		setDistance(distance);
		m_src = src;
		m_dst = dst;
	}

	@Override
	public String toString() {
		return m_src.toString() + " - " + m_dst.toString();
	}
	
	public void setDistance(double d) {
		m_distance = d;
	}
	
	/**
	 * {@InheritDoc}
	 */
	@Override
	public int hashCode() {
		return m_src.hashCode() ^ m_dst.hashCode();
	}
	
	/**
	 * If an edge is between the same vertex, its the same edge (multiple edges NOT permitted for now)
	 */
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof MyEdge)) {
			return false;
		}
		
		MyEdge e2 = (MyEdge) o;
		if ((m_src.equals(e2.m_src) && m_dst.equals(e2.m_dst)) ||
				(m_src.equals(e2.m_dst) && m_dst.equals(e2.m_src)) ) {
			return true;
		}
		return false;
	}
}
