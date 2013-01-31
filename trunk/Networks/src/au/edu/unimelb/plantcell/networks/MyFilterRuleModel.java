package au.edu.unimelb.plantcell.networks;

import java.util.HashMap;
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	Graph<MyVertex,MyEdge> getFilteredGraph(RenderContext<MyVertex,MyEdge> rc, 
						Graph<MyVertex,MyEdge> g) throws Exception {
		Graph<MyVertex,MyEdge> new_g = g.getClass().newInstance();
		
		Logger.getAnonymousLogger().info("Entering getFilteredGraph()");
		
		// first filter out vertices which do not pass the filter rules
		HashSet<MyVertex> rejected_nodes = new HashSet<MyVertex>(g.getVertexCount());
		HashSet<MyEdge> rejected_edges   = new HashSet<MyEdge>(g.getEdgeCount());
		
		// build new graph
		make_new_graph(new_g, g);
		
		Logger l = Logger.getAnonymousLogger();
		for (int i=0; i<this.getSize(); i++) {
			l.info("Processing filter rule "+i);
			l.info("Current graph size: "+new_g.getVertexCount()+" vertices, "+new_g.getEdgeCount()+" edges.");
			MyPredicate p = (MyPredicate) this.getElementAt(i);
			
			// edge filter?
			if (!p.isVertexPredicate()) {
				for (MyEdge e : new_g.getEdges()) {
					if (!p.evaluate(Context.getInstance(new_g, e))) {
						rejected_edges.add(e);
					}
				}
			} else {	// node filter?
				for (MyVertex n : new_g.getVertices()) {
					if (!p.evaluate(Context.getInstance(new_g, n))) {
						rejected_nodes.add(n);
					}
				}
			}
			
			// rebuild new_g to contain only the remaining nodes and edges
			l.info("Rejected: "+rejected_nodes.size()+" "+rejected_edges.size());
			for (MyVertex v : rejected_nodes) {
				new_g.removeVertex(v);
			}
			for (MyEdge e : rejected_edges) {
				// only if both endpoints are in resulting graph do we add the edge
				new_g.removeEdge(e);
			}
			
			// remove all rejected edges and nodes from the current accepted list for the next stage of filtering
			rejected_edges.clear();
			rejected_nodes.clear();
		}
		
		l.info("Leaving getFilteredGraph() "+new_g.getVertexCount()+" "+new_g.getEdgeCount());
		return new_g;
	}
	
	/**
	 * MUST deep copy to keep jung happy
	 * @param new_g
	 * @param orig
	 * @throws Exception
	 */
	private void make_new_graph(Graph<MyVertex,MyEdge> new_g, Graph<MyVertex,MyEdge> orig) throws Exception {
		HashMap<String,MyVertex> nodes = new HashMap<String,MyVertex>();
		
		for (MyVertex v : orig.getVertices()) {
			MyVertex v2 = new MyVertex(v);
			nodes.put(v.getID(), v2);
			if (!new_g.addVertex(v2))
				throw new Exception("Cannot add vertex: "+v2.getID());
		}
		for (MyEdge e : orig.getEdges()) {
			// must deep copy to keep JUNG happy
			MyEdge e2 = new MyEdge(nodes.get(e.getSource()), nodes.get(e.getDest()), e);
			if (!e2.addEdge(new_g)) {
				throw new Exception("Cannot add edge: "+e2.getID());
			}
		}
	}
}

