package au.edu.unimelb.plantcell.networks;

import java.util.logging.Logger;

import org.apache.commons.collections15.Predicate;

/**
 * Abstract base class for edge and vertex predicates for common code between them. Not pretty, but it works.
 * 
 * @author andrew.cassin
 *
 * @param <T>
 */
public abstract class MyPredicate<T> implements Predicate<T> {
	private String m_prop, m_value, m_op;
	
	protected void setProp(String s) {
		m_prop = s;
	}
	
	protected final String getProp() {
		return m_prop;
	}
	
	protected final String getValue() {
		return m_value;
	}
	
	protected void setValue(String s) {
		m_value = s;
	}
	
	protected void setOp(String s) {
		m_op = s;
	}
	
	/**
	 * is the predicate an edge or vertex filter? (true = vertex, false = edge)
	 */
	public abstract boolean isVertexPredicate();
	
	public abstract Object[] getPropertyKeys(T c);
	
	public abstract Object getProperty(T c, Object key);
	
	/**
	 * Must handle all operators as specified by the dialog (or a subclass implementation must)
	 */
	@Override
	public boolean evaluate(T c) {
		if (c == null)
			return false;
		
		if (m_op.startsWith("has annotation")) {
			return getProperty(c, getProp()) != null;
		} else if (m_op.startsWith("is not annotated")) {
			return getProperty(c, getProp()) == null;
		}
		// search all properties?
		if (m_prop == null) {
			for (Object key : getPropertyKeys(c)) {
				Object o = getProperty(c, key);
				if (o == null)
					return false;
				if (eval(m_op, o.toString(), m_value)) 
					return true;
			}
		} else {
			Object o = getProperty(c, m_prop);
			if (o == null)
				return false;
			if (eval(m_op, o.toString(), m_value))
				return true;
		}
		
		// catch-all
		return false;
	}
	
	/*
	 * Only invoked for string or numeric comparison (other operators are handled elsewhere)
	 */
	private boolean eval(String op, String val, String required_value) {
		if (val == null || required_value == null || op == null || op.length() < 1)
			return false;
		
		if (op.equals("=")) {
			return val.equals(required_value);
		} else if (op.equals(" contains ")) {
			return val.toLowerCase().indexOf(required_value.toLowerCase()) >= 0;
		} else if (op.trim().equals("does not contain")) {
			return val.toLowerCase().indexOf(required_value.toLowerCase()) < 0;
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
			if (op.equals(">")) {
				return d_val > d_required;
			} else if (op.equals("<")) {
				return d_val < d_required;
			} else if (op.equals(">=")) {
				return d_val >= d_required;
			} else if (op.equals("<=")) {
				return d_val <= d_required;
			} else if (op.equals("=")) {
				// use fixed small value to compare for floating point equality
				return (Math.abs(d_val - d_required) < 1e-10d);
			} else {
				Logger.getAnonymousLogger().warning("Unknown eval operation:"+op);
			}
		}
		return false;
	}

	/**
	 * Returns a representation of the predicate as a string suitable for displaying to the user
	 */
	@Override
	public String toString() {
		String type = "node";
		if (!isVertexPredicate()) {
			type = "edge";
		}
		type = type.toUpperCase();
		if (m_prop == null) {
			return type+" filter: Any annotation data "+m_op+" "+m_value+".";
		} else if (m_prop.startsWith("<Is directly conn")) {
			return type+" filter: those with a direct connection to "+m_value+".";
		} else if (m_prop.startsWith("<Is visibly conn")) {
			return type+" filter: those with a visible and direct connection to "+m_value+".";
		} else {
			return type+" filter: Annotation named '"+m_prop+"' with a value "+m_op+" "+m_value+".";
		}
	}
}
