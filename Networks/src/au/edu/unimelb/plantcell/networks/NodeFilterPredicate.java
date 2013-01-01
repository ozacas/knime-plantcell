package au.edu.unimelb.plantcell.networks;

import au.edu.unimelb.plantcell.networks.cells.MyEdge;
import au.edu.unimelb.plantcell.networks.cells.MyVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;

/**
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
	public Object[] getPropertyKeys(T c) {
		return c.element.getPropertyKeys().toArray(new Object[0]);
	}

	@Override
	public Object getProperty(T c, Object key) {
		return c.element.getProperty(key);
	}
}
