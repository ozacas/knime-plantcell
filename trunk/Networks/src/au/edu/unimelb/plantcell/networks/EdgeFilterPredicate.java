package au.edu.unimelb.plantcell.networks;

import java.util.logging.Logger;

import au.edu.unimelb.plantcell.networks.cells.MyEdge;
import au.edu.unimelb.plantcell.networks.cells.MyVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;

public class EdgeFilterPredicate<T extends Context<Graph<MyVertex,MyEdge>, MyEdge>> implements MyPredicate<T> {
	private String m_prop, m_op, m_value;
	
	public EdgeFilterPredicate(String propName, String op, String value) {
		m_prop = propName;
		if (m_prop.equals("<Any>")) {
			m_prop = null;
		}
		m_op   = op;
		m_value= value;
	}
	
	@Override
	public boolean evaluate(T c) {
		if (c == null || c.element == null)
			return false;
		
		// search all properties?
		if (m_prop == null) {
			for (Object key : c.element.getPropertyKeys()) {
				Object o = c.element.getProperty(key);
				if (o == null)
					return false;
				if (eval(m_op, o.toString(), m_value)) 
					return true;
			}
		} else {
			Object o = c.element.getProperty(m_prop);
			if (o == null)
				return false;
			if (eval(m_op, o.toString(), m_value))
				return true;
		}
		
		// catch-all
		return false;
	}
	
	private boolean eval(String op, String val, String required_value) {
		if (val == null || required_value == null || op == null || op.length() < 1)
			return false;
		
		if (op.equals("=") && val.equals(required_value)) {
			return true;
		} else if (op.equals(" contains ") && val.toLowerCase().indexOf(required_value.toLowerCase()) >= 0) {
			return true;
		} else {
			// numeric comparison (HACK TODO: done as non-type safe double comparison)
			double d_val, d_required;
			try {
				d_val      = Double.parseDouble(val);
				d_required = Double.parseDouble(required_value);
			} catch (NumberFormatException nfe) {
				if (op.equals("=")) {		// cant compare numbers? ok try string then...
					return val.equals(required_value);
				}
				return false;
			}
			if (op.equals(">")         && d_val > d_required) {
				return true;
			} else if (op.equals("<")  && d_val < d_required) {
				return true;
			} else if (op.equals(">=") && d_val >= d_required) {
				return true;
			} else if (op.equals("<=") && d_val <= d_required) {
				return true;
			} else if (op.equals("=")) {
				// use fixed small value to compare for floating point equality
				return (Math.abs(d_val - d_required) < 1e-10d);
			} else {
				Logger.getAnonymousLogger().warning("Unknown eval operation: "+op);
			}
		}
		return false;
	}

	@Override
	public boolean isVertexPredicate() {
		return false;
	}

	@Override
	public String toString() {
		if (m_prop == null) {
			return "Any edge annotation data "+m_op+" "+m_value+".";
		} else {
			return "Annotation named '"+m_prop+"' with a value "+m_op+" "+m_value+" in an edge.";
		}
	}
}
