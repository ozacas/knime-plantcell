package au.edu.unimelb.plantcell.core.cells;

import java.io.IOException;

import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;

/**
 * Any class which must be persisted via the SequenceCell serializer must implement this
 * to be correctly persisted (ie. saving/loading of workflows)
 * @author andrew.cassin
 *
 */
public interface SerializableInterface<T> {
	
	/**
	 * Stream the instance to the specified <code>output</code>
	 * @param output
	 * @throws IOException
	 */
	public void serialize(DataCellDataOutput output) throws IOException;
	
	/**
	 * Load the instance from the specified <code>input</code>
	 * @param input
	 * @return
	 * @throws IOException
	 */
	public T deserialize(DataCellDataInput input) throws IOException;
}
