package au.edu.unimelb.plantcell.core.cells;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataColumnSpec;

/**
 * Designed to implement a "bag full of stuff" for the convenience of users. Not sure what it will 
 * look like for now, so just empty...
 * 
 * @author andrew.cassin
 *
 */
public class UserDefinedAnnotation extends SequenceAnnotation {

	@Override
	public int countAnnotations() {
		return 0;
	}

	@Override
	public AnnotationType getAnnotationType() {
		return AnnotationType.USER_DEFINED;
	}

	@Override
	public void serialize(DataCellDataOutput output) throws IOException {
		throw new IOException("not yet implemented");
	}

	@Override
	public SequenceAnnotation deserialize(DataCellDataInput input)
			throws IOException {
		throw new IOException("not yet implemented");
	}

	@Override
	public List<DataColumnSpec> asColumnSpec(String prefix) {
		return new ArrayList<DataColumnSpec>();
	}

}
