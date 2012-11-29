package au.edu.unimelb.plantcell.networks;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;


/**
 * A cell which stores a single JUNG-based Graph and persists it via the KNIME serialisation framework
 * 
 * @author andrew.cassin
 *
 */
public class NetworkCell extends DataCell implements NetworkValue, StringValue {
	/**
	 * used for serialisation only
	 */
	private static final long serialVersionUID = 1276265215274142218L;

	private Graph<MyVertex, MyEdge> m_graph = new SparseGraph<MyVertex, MyEdge>();		// never null
	
	/**
	 * Convenience method
	 */
    public static final DataType TYPE = DataType.getType(NetworkCell.class);
    private static final NetworkSerializer<NetworkCell> SERIALIZER = new NetworkSerializer<NetworkCell>();
    
    
    public NetworkCell() {
    }
    
    public NetworkCell(Graph<MyVertex, MyEdge> new_graph) {
    	assert(new_graph != null);
    	m_graph = new_graph;
    }
    
	@Override
	public String toString() {
		return "Graph has "+m_graph.getVertexCount()+" nodes, "+m_graph.getEdgeCount()+" edges";
	}
	
	// called by the KNIME framework using reflection
	public static final DataCellSerializer<NetworkCell> getCellSerializer() {
		return SERIALIZER;
	}
	

	@Override
	protected boolean equalsDataCell(DataCell dc) {
		if (!(dc instanceof NetworkValue))
			return false;
		
		return m_graph.equals(((NetworkValue)dc).getGraph());
	}

	@Override
	public int hashCode() {
		return m_graph.hashCode();
	}

	@Override
	public int compareTo(NetworkValue nv) {
		Graph<MyVertex, MyEdge> a = getGraph();
		Graph<MyVertex, MyEdge> b = nv.getGraph();
		
		if (a.getVertexCount() < b.getVertexCount()) {
			return -1;
		} else if (a.getVertexCount() > b.getVertexCount()) {
			return 1;
		} else {
			// vertex count is equal
			if (a.getEdgeCount() < b.getEdgeCount()) {
				return -1;
			} else if (a.getEdgeCount() > b.getEdgeCount()) {
				return 1;
			} else if (a.equals(b)) {
				return 0;
			}
		}
		return 0;
	}

	@Override
	public String getStringValue() {
		return toString();
	}

	@Override
	public Graph<MyVertex, MyEdge> getGraph() {
		return m_graph;
	}

}
