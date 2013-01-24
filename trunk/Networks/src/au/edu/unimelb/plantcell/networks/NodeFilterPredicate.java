package au.edu.unimelb.plantcell.networks;

import java.util.HashSet;
import java.util.Set;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

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
				m_rebuild = true;			
			}

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
		if (propName != null && propName.toLowerCase().equals("<is visibly connected to>")) {
			// interactive performance is important: if we've already computed the result just return it again
			if (!m_rebuild) {
				return m_acceptable_vertices.contains(c.element);
			}
			String val = getValue().toLowerCase();
			m_acceptable_vertices.clear();
			// no rules?
			if (m_rules == null || m_rules.size() < 1) {
				m_rebuild = false;
				return true;
			}
			
			try {
				for (MyEdge e : c.graph.getEdges()) {
					if (e.hasVertexNamed(val)) {
						m_acceptable_vertices.add(e.getSourceVertex());
						m_acceptable_vertices.add(e.getDestVertex());
					}
				}
				m_rebuild = false;
				return m_acceptable_vertices.contains(c.element);
			} catch (IllegalArgumentException iae) {
				// may happen if the user mistypes a node ID
				return false;
			}
		} else {
			m_rebuild = false;
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
	
	@Override
	public void setValue(String new_value) {
		m_rebuild = true;
		super.setValue(new_value);
	}
}
