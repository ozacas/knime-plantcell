package au.edu.unimelb.plantcell.networks;

import au.edu.unimelb.plantcell.networks.cells.MyEdge;
import au.edu.unimelb.plantcell.networks.cells.MyVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;

/**
 * Filters nodes based on the chosen criteria. Ugly hack. Need to interoperate better with JUNG.
 * 
 * @author andrew.cassin
 *
 * @param <T>
 */
public class NodeFilterPredicate<T extends Context<Graph<MyVertex,MyEdge>, MyVertex>> extends MyPredicate<T> {
	
	public NodeFilterPredicate(String propName, String op, String value) {
		setProp(propName);
		if (propName.equals("<Any>")) {
			setProp(null);
		}
		setOp(op);
		setValue(value);
	}

	@Override
	public boolean isVertexPredicate() {
		return true;
	}

	@Override
	public boolean evaluate(T c) {
		String propName = getProp();
		
		if (propName != null && propName.toLowerCase().startsWith("<is directly connected to>")) {
			String val = getValue();
			// leave the vertex of interest in the graph too (even if no self-edge exists)
			try {
				return (c.graph.isNeighbor(c.element, new MyVertex(val)) || c.element.getID().indexOf(val) >= 0);
			} catch (IllegalArgumentException iae) {
				// may happen if the user mistypes a node ID
				return false;
			}
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
