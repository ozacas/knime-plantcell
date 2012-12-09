package au.edu.unimelb.plantcell.networks.cells;

import java.awt.Color;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.property.ColorAttr;

/**
 * Properties of every vertex in the network. Each is a separate instance. Not very efficient, so will need
 * lots of memory for large graphs and annotated datasets.
 * 
 * @author andrew.cassin
 *
 */
public class MyVertex implements Paint {
	private String m_name;
	private static int m_id = 1;
	private final Properties props = new Properties();
	private ColorAttr m_colour = null;
	private double[] m_vector = null;
	
	
	public MyVertex() {
		this(null);
	}
	
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
	
	public void setID(String new_id) {
		m_name = new_id;
	}
	
	public Set<Object> getPropertyKeys() {
		return props.keySet();
	}
	
	public Object getProperty(Object key) {
		return props.get(key);
	}
	
	public void setProperty(String key, String value) {
		if (key != null && value != null) {
			props.put(key, value);
		}
	}
	
	public void setProperties(Properties new_props) {
		props.clear();
		for (Object key : new_props.keySet()) {
			setProperty(key.toString(), new_props.getProperty(key.toString()));
		}
	}

	public void setProperties(Map<String, String> new_props) {
		props.clear();
		for (Object key : new_props.keySet()) {
			setProperty(key.toString(), new_props.get(key));
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

	/**
	 * Why can't Americans learn to spell? ;)
	 * @param c
	 */
	public void setColour(ColorAttr c) {
		m_colour = c;
	}

	public Color getColour() {
		if (m_colour == null)
			return Color.black;
		return m_colour.getColor();
	}

	@Override
	public int getTransparency() {
		return 0;
	}

	@Override
	public PaintContext createContext(ColorModel cm, Rectangle deviceBounds,
			Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
		return null;
	}

	public int getNumSamples() {
		if (m_vector == null)
			return 0;
		return m_vector.length;
	}
	
	public double[] getSampleVector() {
		return m_vector;
	}
	
	public void setSampleVector(DataCell[] values) {
		if (values == null || values.length < 1)
			setSampleVector((double[])null);
		double[] new_values = new double[values.length];
		int idx = 0;
		for (DataCell dc : values) {
			if (dc ==  null || dc.isMissing() || !dc.getType().isCompatible(DoubleValue.class)) {
				new_values[idx++] = Double.NaN;
			} else {
				try {
					new_values[idx++] = Double.parseDouble(dc.toString());
				} catch (NumberFormatException nfe) {
					new_values[idx++] = Double.NaN;
				}
			}
		}
		m_vector = new_values;
	}
	
	public void setSampleVector(double[] timecourse_vector) {
		m_vector = timecourse_vector;
	}

	public void setSampleVector(DataCell collection_cell) {
		Iterator<DataCell> it = null;
		if (collection_cell instanceof ListCell) {
			ListCell lc = (ListCell) collection_cell;
			it = lc.iterator();
		} else if (collection_cell instanceof SetCell) {
			SetCell sc = (SetCell) collection_cell;
			it = sc.iterator();
		} else {
			setSampleVector((double[])null);
			return;
		}
		ArrayList<DataCell> vec = new ArrayList<DataCell>();
		if (it != null) {
			while (it.hasNext()) {
				vec.add(it.next());
			}
			setSampleVector(vec.toArray(new DataCell[0]));
		}
	}

	public String getSampleVectorAsString() {
		if (m_vector == null || m_vector.length < 1)
			return "";
		StringBuilder sb = new StringBuilder();
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(3);
		for (double d : m_vector) {
			sb.append(nf.format(d));
			sb.append(' ');
		}
		return sb.toString();
	}

}
