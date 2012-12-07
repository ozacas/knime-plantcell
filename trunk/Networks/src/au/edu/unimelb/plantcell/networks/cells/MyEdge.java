package au.edu.unimelb.plantcell.networks.cells;

import java.awt.Color;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.knime.core.data.property.ColorAttr;

/**
 * 
 * @author andrew.cassin
 *
 */
public class MyEdge {
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

	public void setDistance(double d) {
		props.put("distance", new Double(d).toString());
	}

	private void setColour(String key, ColorAttr rowColor) {
		if ("__colour".equals(key)) {
			if (rowColor == null) {
				m_colour = ColorAttr.getInstance(Color.BLACK);
			} else {
				m_colour = rowColor;
			}
		}
	}
	
	public void setColour(String rgb) {
		if (rgb == null || rgb.indexOf(',') < 0) {
			setColour("__colour", ColorAttr.getInstance(Color.BLACK));
			return;
		}
		String[] rgb_vec = rgb.split(",");
		if (rgb_vec.length == 3) {
			Color c = new Color(Integer.valueOf(rgb_vec[0]), 
					Integer.valueOf(rgb_vec[1]), 
					Integer.valueOf(rgb_vec[2]));
			setColour("__colour", ColorAttr.getInstance(c));
		} else {
			setColour("__colour", ColorAttr.getInstance(Color.BLACK));
		}
	}
	
	public Color getColour() {
		if (m_colour == null)
			return Color.black;
		return m_colour.getColor();
	}

	public void setProperty(String key, String value) {
		if (key.equals("id") && value != null) {
			m_id = value;
		} else if (key.equals("__colour")) {
			setColour(value);
		}
		props.put(key, value);
	}
	
	public void setProperties(Properties new_props) {
		props.clear();
		for (Object key : new_props.keySet()) {
			setProperty(key.toString(), new_props.getProperty(key.toString()));
		}
	}
	
	public void setProperties(Map<String,String> new_props) {
		for (String key : new_props.keySet()) {
			setProperty(key, new_props.get(key));
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
		return Double.parseDouble(props.getProperty("distance"));
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
