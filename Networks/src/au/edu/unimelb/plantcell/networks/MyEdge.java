package au.edu.unimelb.plantcell.networks;

/**
 * 
 * @author andrew.cassin
 *
 */
public class MyEdge {
	private double m_distance;
	
	public MyEdge() {
		this(0.0);
	}
	
	public MyEdge(double distance) {
		setDistance(distance);
		
	}

	public void setDistance(double d) {
		m_distance = d;
	}
}
