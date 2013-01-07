package au.edu.unimelb.plantcell.networks;

import au.edu.unimelb.plantcell.networks.cells.MyEdge;
import au.edu.unimelb.plantcell.networks.cells.MyVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;

/**
 * Removes edges which do not satisfy the user chosen predicate as determined
 * by the constructor.
 * 
 * @author andrew.cassin
 *
 * @param <T>
 */
public class EdgeFilterPredicate<T extends Context<Graph<MyVertex,MyEdge>, MyEdge>> extends MyPredicate<T> {
	
	public EdgeFilterPredicate(String propName, String op, String value) {
		setProp(propName);
		if (propName.equals("<Any>")) {
			setProp(null);
		}
		setOp(op);
		setValue(value);
	}

	@Override
	public boolean isVertexPredicate() {
		return false;
	}
	
	@Override
	public boolean evaluate(T c) {
		String propName = getProp();
		
		if (propName != null && propName.toLowerCase().startsWith("<is directly connected to>")) {
			String s_name = c.element.getSource().toLowerCase();
			String d_name = c.element.getDest().toLowerCase();
			if (s_name == null || d_name == null)		// bad edge or maybe disjoint graph?
				return false;
			
			String val    = getValue().toLowerCase();
			if (s_name.indexOf(val) >= 0 || d_name.indexOf(val) >= 0)
				return true;
			return false;
		} else {
			return super.evaluate(c);
		}
	}

	@Override
	public Object[] getPropertyKeys(T c) {
		return c.element.getPropertyKeys().toArray(new Object[0]);
	}

	@Override
	public Object getProperty(T c, Object key) {
		return c.element.getProperty(key);
	}
}
