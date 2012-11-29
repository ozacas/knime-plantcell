package au.edu.unimelb.plantcell.networks;

import java.io.IOException;

import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;

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
		
	}

	@Override
	public T deserialize(DataCellDataInput input) throws IOException {
		
		return null;
	}

}
