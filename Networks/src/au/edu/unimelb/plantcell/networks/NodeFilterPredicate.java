package au.edu.unimelb.plantcell.networks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import au.edu.unimelb.plantcell.networks.cells.MyEdge;
import au.edu.unimelb.plantcell.networks.cells.MyVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * Filters nodes based on the chosen criteria. Ugly hack. Need to interoperate better with JUNG.
 * 
 * @author andrew.cassin
 *
 * @param <T>
 */
public class NodeFilterPredicate<T extends Context<Graph<MyVertex,MyEdge>, MyVertex>> extends MyPredicate<T> {
	private MyFilterRuleModel m_rules;
	private boolean m_rebuild;
	private final Set<MyVertex> m_acceptable_vertices = new HashSet<MyVertex>();
	
	public NodeFilterPredicate(String propName, String op, String value, MyFilterRuleModel rules) {
		setProp(propName);
		if (propName.equals("<Any>")) {
			setProp(null);
		}
		setOp(op);
		setValue(value);
		m_rebuild = true;
		m_rules = rules;
		rules.addListDataListener(new ListDataListener() {

			@Override
			public void contentsChanged(ListDataEvent arg0) {
				m_rebuild = true;			}

			@Override
			public void intervalAdded(ListDataEvent arg0) {
				m_rebuild = true;
			}

			@Override
			public void intervalRemoved(ListDataEvent arg0) {
				m_rebuild = true;
			}
			
		});
	}

	@Override
	public boolean isVertexPredicate() {
		return true;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean evaluate(T c) {
		String propName = getProp();
		/**
		 * Here we are testing the proposition: do we accept v2 in the view considering all the visible edges
		 * to a node which has val in them (partial or complete matches, ignoring case)?
		 */
		if (propName != null && propName.toLowerCase().startsWith("<is visibly connected to>")) {
			// interactive performance is important: if we've already computed the result just return it again
			if (!m_rebuild) {
				return m_acceptable_vertices.contains(c.element);
			}
			String val = getValue();
			m_acceptable_vertices.clear();
			// no rules?
			if (m_rules == null || m_rules.size() < 1) {
				m_rebuild = false;
				return true;
			}
			
			try {
				Collection<MyVertex> suitable_vertices = get_suitable_vertices(c.graph, val);
				// self is always considered directly connected
				MyVertex v2= c.element;

				if (v2.getID().indexOf(val) >= 0) {
					m_acceptable_vertices.add(v2);
					// here, we do not reset m_rebuild as there will be non-self vertices to process...
					return true;
				}
				
				// not directly connected (even if all edges are visible?)
				for (MyVertex v : suitable_vertices) {
					if (!c.graph.isNeighbor(v2, v))
							continue;
					
					// else check the visible edges ONLY and decide if its still a neighbour
					HashSet<MyEdge> remaining_edges = new HashSet<MyEdge>();
					remaining_edges.addAll(c.graph.getEdges());
					for (int i=0; i<m_rules.size(); i++) {
						MyPredicate p = (MyPredicate) m_rules.getElementAt(i);
						if (p.isVertexPredicate()) {
							// applied all edge filters up to this one?
							if (p == this) {
								// ok so test the edges which have survived filtering so far to see 
								// if they remain directly connected
								test_edges(c.graph, remaining_edges, v2, v);
								break;
							}
							// else
							continue;
						}
						// else must be an edge filter, so apply it to all edges and remove
						// those which get filtered
						HashSet<MyEdge> dup = new HashSet<MyEdge>();
						for (MyEdge e: remaining_edges) {
							if (p.evaluate(Context.<Graph<MyVertex,MyEdge>,MyEdge>getInstance(c.graph,e))) {
								dup.add(e);
							}
						}
						remaining_edges.clear();
						remaining_edges.addAll(dup);
					}
					
					test_edges(c.graph, remaining_edges, v2, v);
				}
				m_rebuild = false;
				return m_acceptable_vertices.contains(v2);
			} catch (IllegalArgumentException iae) {
				// may happen if the user mistypes a node ID
				return false;
			}
		} else {
			m_rebuild = false;
			return super.evaluate(c);
		}
	}
	
	private Collection<MyVertex> get_suitable_vertices(Graph<MyVertex, MyEdge> graph, String val) {
		ArrayList<MyVertex> ret = new ArrayList<MyVertex>();
		if (graph == null || val == null || val.length() < 1)
			return ret;
		for (MyVertex v : graph.getVertices()) {
			if (v.getID().toLowerCase().indexOf(val) >= 0) {
				ret.add(v);
			}
		}
		return ret;
	}

	private void test_edges(Graph<MyVertex, MyEdge> graph, Collection<MyEdge> remaining_edges, 
			MyVertex add_me_iff_acceptable, MyVertex b) {
		for (MyEdge e : remaining_edges) {
			Pair<MyVertex> p = graph.getEndpoints(e);
			if ((p.getFirst().equals(add_me_iff_acceptable) && p.getSecond().equals(b)) ||
				 p.getFirst().equals(b) && p.getSecond().equals(add_me_iff_acceptable)) {
				m_acceptable_vertices.add(add_me_iff_acceptable);
			}
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
	
	@Override
	public void setValue(String new_value) {
		m_rebuild = true;
		super.setValue(new_value);
	}
}
