package au.edu.unimelb.plantcell.misc.biojava;

/**
 * 
 * @author andrew.cassin
 *
 */
public class TaskParameter {
	private String m_name;
	private String m_value;
	
	public TaskParameter(String name, String initial_value) {
		assert(name != null && initial_value != null && name.length() > 0);
		
		m_name = name;
		m_value= initial_value;
	}

	public boolean isValid() {
		return true;
	}

	public String getName() {
		return m_name;
	}
	
	public String getValue() {
		return m_value;
	}
}
