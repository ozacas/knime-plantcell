package au.edu.unimelb.plantcell.networks;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;

import org.apache.commons.collections15.Predicate;

import au.edu.unimelb.plantcell.networks.cells.MyEdge;
import au.edu.unimelb.plantcell.networks.cells.MyVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;

public class MyFilterRuleModel extends DefaultListModel {

	/**
	 * not serialised
	 */
	private static final long serialVersionUID = 5885583466847145012L;

	
	public Predicate<Context<Graph<MyVertex, MyEdge>, MyVertex>> getVertexFilter() {
		final ListModel l = this;
		return new Predicate<Context<Graph<MyVertex,MyEdge>, MyVertex>>() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public boolean evaluate(
					Context<Graph<MyVertex, MyEdge>, MyVertex> c) {
				for (int i=0; i<l.getSize(); i++) {
					MyPredicate p = (MyPredicate) l.getElementAt(i);
					if (p.isVertexPredicate() && !p.evaluate(c)) {
						return false;
					}
				}
				// default is to accept
				return true;
			}
			
		};
	}

	public Predicate<Context<Graph<MyVertex, MyEdge>, MyEdge>> getEdgeFilter() {
		final ListModel l = this;
		return new Predicate<Context<Graph<MyVertex,MyEdge>,MyEdge>>() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public boolean evaluate(
					Context<Graph<MyVertex, MyEdge>, MyEdge> c) {
				for (int i=0; i<l.getSize(); i++) {
					MyPredicate p = (MyPredicate) l.getElementAt(i);
					if (!p.isVertexPredicate() && !p.evaluate(c))
						return false;
				}
				return true;
			}
			
		};
	}
	
}
