package au.edu.unimelb.plantcell.networks;

import java.util.Collection;
import java.util.HashSet;

import au.edu.unimelb.plantcell.networks.cells.MyEdge;
import au.edu.unimelb.plantcell.networks.cells.MyVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;

/**
 * Filters nodes based on the chosen criteria. Need to interoperate better with JUNG.
 * 
 * @author andrew.cassin
 *
 * @param <T>
 */
public class NodeFilterPredicate<T extends Context<Graph<MyVertex,MyEdge>, MyVertex>> extends MyPredicate<T> {
	
	public NodeFilterPredicate(String propName, String op, String value, MyFilterRuleModel rules) {
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
		/**
		 * Here we are testing the proposition: do we accept v2 in the view considering all the visible edges
		 * to a node which has val in them (partial or complete matches, ignoring case)?
		 */
		if (propName != null) {
			String val = getValue().toLowerCase();
			String l_prop = propName.toLowerCase();
			
			if (l_prop.equals("<is visibly connected to>")) {
				// interactive performance is important: if we've already computed the result just return it again
				// find the set of all nodes which match val
				Collection<MyVertex> matching_nodes = new HashSet<MyVertex>();
				for (MyVertex v : c.graph.getVertices()) {
					if (v.getID().indexOf(val) >= 0) {
						matching_nodes.add(v);
					}
				}
				
				try {
					for (MyVertex matcher : matching_nodes) {
						for (MyEdge e: c.graph.getEdges()) {
							if (e.hasVertices(matcher, c.element)) {
								return true;
							} else if (matcher.equals(c.element)) {
								return true;
							}
						}
					}
					return false;
				} catch (IllegalArgumentException iae) {
					// may happen if the user mistypes a node ID
					iae.printStackTrace();
					return false;
				}
			} else if (l_prop.equals("<degree>")) {
				int cnt = c.graph.degree(c.element);
				return super.eval(super.getOp(), String.valueOf(cnt), val);
			} else if (l_prop.equals("<multi connected to>")) {
				String[] ids = val.split("\\s+");
				int cnt = 0;
				for (MyEdge e : c.graph.getIncidentEdges(c.element)) {
					for (int i=0; i<ids.length; i++) {
						if (e.hasVertexNamed(ids[i])) {
							cnt++;
						}
					}
				}
				
				return (cnt > 1);
			} else {
				return super.evaluate(c);
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
