package au.edu.unimelb.plantcell.networks;

import java.util.HashSet;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;

import au.edu.unimelb.plantcell.networks.cells.MyEdge;
import au.edu.unimelb.plantcell.networks.cells.MyVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.RenderContext;

public class MyFilterRuleModel extends DefaultListModel {

	/**
	 * not serialised
	 */
	private static final long serialVersionUID = 5885583466847145012L;

	
	/*public Predicate<Context<Graph<MyVertex, MyEdge>, MyVertex>> getVertexFilter() {
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
	}*/
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	Graph<MyVertex,MyEdge> getFilteredGraph(RenderContext<MyVertex,MyEdge> rc, 
						Graph<MyVertex,MyEdge> g) throws InstantiationException, IllegalAccessException {
		Graph<MyVertex,MyEdge> new_g = g.getClass().newInstance();
		
		Logger.getAnonymousLogger().info("Entering getFilteredGraph()");
		
		// first filter out vertices which do not pass the filter rules
		HashSet<MyVertex> accepted_nodes = new HashSet<MyVertex>(g.getVertexCount());
		HashSet<MyVertex> rejected_nodes = new HashSet<MyVertex>(g.getVertexCount());
		HashSet<MyEdge> accepted_edges   = new HashSet<MyEdge>(g.getEdgeCount());
		HashSet<MyEdge> rejected_edges   = new HashSet<MyEdge>(g.getEdgeCount());
		
		accepted_edges.addAll(g.getEdges());
		accepted_nodes.addAll(g.getVertices());
		
		for (int i=0; i<this.getSize(); i++) {
			Logger.getAnonymousLogger().info("Processing filter rule "+i);
			Logger.getAnonymousLogger().info("Accepted: "+accepted_nodes.size()+" "+accepted_edges.size());
			Logger.getAnonymousLogger().info("Rejected: "+rejected_nodes.size()+" "+rejected_edges.size());

			MyPredicate p = (MyPredicate) this.getElementAt(i);
			
			// edge filter?
			if (!p.isVertexPredicate()) {
				for (MyEdge e : accepted_edges) {
					if (!p.evaluate(Context.getInstance(g, e))) {
						rejected_edges.add(e);
					}
				}
			} else {	// node filter?
				for (MyVertex n : accepted_nodes) {
					if (!p.evaluate(Context.getInstance(g, n))) {
						rejected_nodes.add(n);
					}
				}
			}
			
			// remove all rejected edges and nodes from the current accepted list for the next stage of filtering
			accepted_edges.removeAll(rejected_edges);
			rejected_edges.clear();
			accepted_nodes.removeAll(rejected_nodes);
			rejected_nodes.clear();
		}
	
		// compute the graph from whats left and return it
		for (MyVertex v : accepted_nodes) {
			new_g.addVertex(v);
		}
		for (MyEdge e : accepted_edges) {
			// only if both endpoints are in resulting graph do we add the edge
			if (e.hasVerticesIn(accepted_nodes)) {
				e.addEdge(new_g);
			}
		}
		
		Logger.getAnonymousLogger().info("Leaving getFilteredGraph() "+new_g.getVertexCount()+" "+new_g.getEdgeCount());
		return new_g;
	}
}

