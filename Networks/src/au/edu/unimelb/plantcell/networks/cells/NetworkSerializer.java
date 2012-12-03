package au.edu.unimelb.plantcell.networks.cells;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.io.GraphMLReader;
import edu.uci.ics.jung.io.GraphMLWriter;

/**
 * Persist a network cell to/from the storage
 * @author andrew.cassin
 *
 * @param <T>
 */
public class NetworkSerializer<T extends NetworkCell> implements DataCellSerializer<T> {

	@Override
	public void serialize(T cell, DataCellDataOutput output) throws IOException {
		NetworkValue nv = (NetworkValue) cell;
		StringWriter sw = new StringWriter();
		Graph<MyVertex,MyEdge> g = nv.getGraph();
		GraphMLWriter<MyVertex,MyEdge> gmlw = new GraphMLWriter<MyVertex,MyEdge>();
		gmlw.save(g, sw);
		sw.close();
		
		output.writeUTF(sw.getBuffer().toString());
	}

	@SuppressWarnings("unchecked")
	@Override
	public T deserialize(DataCellDataInput input) throws IOException {
		String g_str = input.readUTF();
		
		try {
			Graph<MyVertex,MyEdge> g = new SparseGraph<MyVertex,MyEdge>();
			GraphMLReader<Graph<MyVertex,MyEdge>,MyVertex,MyEdge> rdr = new GraphMLReader<Graph<MyVertex,MyEdge>,MyVertex,MyEdge>();
			StringReader sr = new StringReader(g_str);
			rdr.load(sr, g);
			sr.close();
			return (T) new NetworkCell(g);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException(e.getMessage());
		} 
	}

}
