package au.edu.unimelb.plantcell.networks.cells;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.io.GraphIOException;
import edu.uci.ics.jung.io.GraphMLWriter;
import edu.uci.ics.jung.io.graphml.EdgeMetadata;
import edu.uci.ics.jung.io.graphml.GraphMLReader2;
import edu.uci.ics.jung.io.graphml.GraphMetadata;
import edu.uci.ics.jung.io.graphml.HyperEdgeMetadata;
import edu.uci.ics.jung.io.graphml.NodeMetadata;

/**
 * Save/load graphml into the graphml data type using JUNG's capabilities. 
 * 
 * @author andrew.cassin
 *
 */
public class GraphMLIO {
	
	
	/**
	 * Load a graph from the specified <code>Reader</code> instance
	 * 
	 * @param sr always closed at the completion of the method call
	 * @return
	 * @throws IllegalArgumentException
	 * @throws GraphIOException
	 * @throws NullPointerException
	 * @throws IOException
	 */
	public Graph<MyVertex,MyEdge> load(Reader sr) throws IllegalArgumentException, GraphIOException, NullPointerException, IOException {

		try {
			// graph metadata
			Transformer<GraphMetadata,Graph<MyVertex,MyEdge>> graph_transformer = new Transformer<GraphMetadata,Graph<MyVertex,MyEdge>>() {
	
				@Override
				public Graph<MyVertex, MyEdge> transform(GraphMetadata gm) {
					Graph<MyVertex,MyEdge> g = new SparseGraph<MyVertex,MyEdge>();
					
					// TODO...
					return g;
				}
				
			};
			
			// vertex transformer
			final Map<String,MyVertex> vertex_map = new HashMap<String,MyVertex>();
			Transformer<NodeMetadata,MyVertex> vertex_transformer = new Transformer<NodeMetadata,MyVertex>() {
	
				@Override
				public MyVertex transform(NodeMetadata nm) {
					MyVertex v = new MyVertex();
					v.setID(nm.getId());
					v.setProperties(nm.getProperties());
					vertex_map.put(nm.getId(), v);
					return v;
				}
				
			};
			
			Transformer<EdgeMetadata,MyEdge> edge_transformer = new Transformer<EdgeMetadata,MyEdge>() {
	
				@Override
				public MyEdge transform(EdgeMetadata em) {
					MyEdge e = new MyEdge();
					e.setSource(vertex_map.get(em.getSource()));
					e.setDestination(vertex_map.get(em.getTarget()));
					e.setProperties(em.getProperties());
					return e;
				}
				
			};
			
			// TODO: this node does not support hyperedges, so we just...
			Transformer<HyperEdgeMetadata,MyEdge> hyperedge_transformer = new Transformer<HyperEdgeMetadata,MyEdge>() {
	
				@Override
				public MyEdge transform(HyperEdgeMetadata arg0) {
					return null;
				}
				
			};
			GraphMLReader2<Graph<MyVertex,MyEdge>, MyVertex, MyEdge> rdr = new GraphMLReader2<Graph<MyVertex,MyEdge>, MyVertex, MyEdge>(
					sr, graph_transformer, vertex_transformer, edge_transformer, hyperedge_transformer);
			Graph<MyVertex,MyEdge> g = rdr.readGraph();
			rdr.close();
			return g;
		} finally {
			sr.close();
		}
	}
	
	/**
	 * This implementation will call w.close() after save is done (even if an exception occurs)
	 * 
	 * @param g
	 * @param w
	 * @throws IOException
	 */
	public void save(Graph<MyVertex,MyEdge> g, Writer w) throws IOException {
		try {
			GraphMLWriter<MyVertex,MyEdge> gmlw = new GraphMLWriter<MyVertex,MyEdge>();
			Properties edge_properties   = new Properties();
			Properties vertex_properties = new Properties();
			for (MyVertex v : g.getVertices()) {
				for (final Object key : v.getPropertyKeys()) {
					if (!vertex_properties.containsKey(key)) {
						vertex_properties.put(key, "");
						gmlw.addVertexData(key.toString(), key.toString(), "", new Transformer<MyVertex,String>() {

							@Override
							public String transform(MyVertex v) {
								Object o = v.getProperty(key);
								if (o == null)
									return "";
								return o.toString();
							}
							
						});
					}
				}
			}
			for (MyEdge e : g.getEdges()) {
				for (final Object key : e.getPropertyKeys()) {
					if (!edge_properties.containsKey(key)) {
						edge_properties.put(key, "");
						gmlw.addEdgeData(key.toString(), key.toString(), "", new Transformer<MyEdge,String>() {

							@Override
							public String transform(MyEdge e) {
								Object o = e.getProperty(key);
								if (o == null)
									return "";
								return o.toString();
							}
							
						});
					}
				}
			}
			gmlw.save(g, w);
		} finally {
			w.close();
		}
	}
}
