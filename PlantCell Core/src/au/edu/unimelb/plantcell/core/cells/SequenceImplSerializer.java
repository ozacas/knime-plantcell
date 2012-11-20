package au.edu.unimelb.plantcell.core.cells;

import java.io.IOException;

import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.node.InvalidSettingsException;

/**
 * 
 * @author andrew.cassin
 *
 * @param <T>
 */
public class SequenceImplSerializer<T extends SequenceCell> implements DataCellSerializer<T> {

	@Override
	public void serialize(T cell, DataCellDataOutput output) throws IOException {
		String seq = cell.getStringValue();
		String id  = cell.getID();
		int n_tracks = cell.countTracks();
		output.writeInt(n_tracks);
		output.writeUTF(cell.getSequenceType().name());
		output.writeUTF(id);
		output.writeUTF(seq);
		// dump each track incl. track/annotation specific data
		if (n_tracks > 0) {
			for (Track t : cell.getTracks()) {
				t.serialize(output);
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public T deserialize(DataCellDataInput input) throws IOException {
		int n_tracks = input.readInt();
		String st = input.readUTF();
		String id = input.readUTF();
		String seq = input.readUTF();
		// instantiate the cell and return (all going well)
		try {
			
			SequenceValue c = new SequenceCell(SequenceType.valueOf(st), id, seq);

			for (int i=0; i<n_tracks; i++) {
				Track t = new Track("");
				t.deserialize(input);
				c.addTrack(t);
			}
		
			return (T) c;
		} catch (InvalidSettingsException ise) {
			ise.printStackTrace();
			throw new IOException(ise.getMessage());
		}
	}

}
