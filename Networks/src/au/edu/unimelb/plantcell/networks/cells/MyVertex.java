package au.edu.unimelb.plantcell.networks.cells;

import java.util.Properties;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;

public class MyVertex {
	private String m_name;
	private static int m_id = 1;
	private final Properties props = new Properties();
	
	/**
	 * 
	 * @param name should be unique among vertices in a graph and never null or zero length
	 */
	public MyVertex(String name) {
		if (name == null) {
			name = "Unknown"+m_id++;
		}
		m_name = name;
	}
	
	public Set<Object> getPropertyKeys() {
		return props.keySet();
	}
	
	public Object getProperty(Object key) {
		return props.get(key);
	}
	
	public void setProperty(String key, String value) {
		props.put(key, value);
	}
	
	public void setProperties(Properties new_props) throws InvalidSettingsException {
		props.clear();
		for (Object key : new_props.keySet()) {
			if (key.toString().equals("id")) {
				throw new InvalidSettingsException("Column 'id' is reserved for internal use: rename column!");
			}
			props.put(key, new_props.get(key));
		}
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
