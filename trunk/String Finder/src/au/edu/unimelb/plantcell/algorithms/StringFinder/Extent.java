package au.edu.unimelb.plantcell.algorithms.StringFinder;

import java.awt.Color;

/**
 * Used to represent the state associated with a match
 * @author andrew.cassin
 * @param <T>
 *
 */
@SuppressWarnings("rawtypes")
public class Extent implements Comparable {
	public int m_start;
	public int m_end;
	private Color m_colour;
	
	public Extent(int start, int end) {
		this(start, end, Color.RED);
	}
	
	public Extent(int start, int end, Color color) {
		assert(end >= start);
		m_start = start;
		m_end = end;
		if (color != null) {
			m_colour = color;
		} else {
			m_colour = Color.RED;
		}
	}

	public Color getColour() {
		return m_colour;
	}
	
	public String toString() {
		return m_start + "-" + m_end;
	}

	@Override
	public int compareTo(Object o) {
		Extent arg0 = (Extent) o;
		
		// compare start
		if (m_start < arg0.m_start) {
			return -1;
		} else if (m_start > arg0.m_start) {
			return 1;
		} else {
			// compare end
			if (m_end < arg0.m_end)
				return -1;
			else if (m_end > arg0.m_end)
				return 1;
			else
				return 0;
		}
	}

	public int getStart() {
		return m_start;
	}
	
	public int getEnd() {
		return m_end;
	}
	
	@Override
	public boolean equals(Object oe) {
		if (oe == null || !(oe instanceof Extent))
			return false;
		Extent e = (Extent) oe;
		return (this.getStart() == e.getStart() && this.getEnd() == e.getEnd());
	}
}
