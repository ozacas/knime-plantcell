package au.edu.unimelb.plantcell.algorithms.StringFinder;

import java.awt.Color;

/**
 * Used to represent the state associated with a match
 * @author andrew.cassin
 *
 */
public class Extent {
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
}
