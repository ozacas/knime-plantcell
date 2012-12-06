package au.edu.unimelb.plantcell.networks.cells;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;

import edu.uci.ics.jung.graph.Graph;

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
		new GraphMLIO().save(nv.getGraph(), sw);
		
		output.writeUTF(sw.getBuffer().toString());
	}

	@SuppressWarnings("unchecked")
	@Override
	public T deserialize(DataCellDataInput input) throws IOException {
		String g_str = input.readUTF();
		
		try {
			StringReader sr = new StringReader(g_str);
			Graph<MyVertex,MyEdge> g = new GraphMLIO().load(sr);
			return (T) new NetworkCell(g);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException(e.getMessage());
		} 
	}

}
