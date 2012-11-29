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
	
	
}
