package au.edu.unimelb.plantcell.networks.cells;

import java.awt.Color;
import java.util.Properties;
import java.util.Set;

import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.InvalidSettingsException;

/**
 * 
 * @author andrew.cassin
 *
 */
public class MyEdge {
	private double m_distance;
	private MyVertex m_src, m_dst;
	private static int m_idx = 1;
	private String m_id;
	private final Properties props = new Properties();
	private ColorAttr m_colour = null;
	
	public MyEdge() {
		m_src = null;
		m_dst = null;
		m_id  = "E"+m_idx++;
		setDistance(0.0);
	}
	
	public MyEdge(MyVertex src, MyVertex dest) {
		this(src, dest, 0.0);
	}
	
	public MyEdge(MyVertex src, MyVertex dst, double distance) {
		this();
		setDistance(distance);
		m_src = src;
		m_dst = dst;
	}

	public void setSource(MyVertex new_src) {
		assert(new_src != null);
		m_src = new_src;
	}
	
	public void setDestination(MyVertex new_dst) {
		assert(new_dst != null);
		m_dst = new_dst;
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
	
	public String getID() {
		return m_id;
	}

	
	@Override
	public String toString() {
		return m_src.toString() + " - " + m_dst.toString();
	}
	
	public double getDistance() {
		return m_distance;
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

	public void setColour(ColorAttr c) {
		m_colour = c;
	}
	
	public Color getColour() {
		if (m_colour == null)
			return Color.black;
		return m_colour.getColor();
	}
}
