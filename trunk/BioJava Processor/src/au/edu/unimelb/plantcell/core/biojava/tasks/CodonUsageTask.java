package au.edu.unimelb.plantcell.core.biojava.tasks;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;

/**
 * Report the count of codons in the specified DNA/RNA sequence. T&U are equivalent are
 * far as the columns are concerned.
 * 
 * @author andrew.cassin
 *
 */
public class CodonUsageTask extends BioJavaProcessorTask {

	public CodonUsageTask() {
		
	}
	
	@Override
	public DataCell[] getCells(DataRow row) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(String task_name, int input_column_index) throws Exception {
		assert(input_column_index >= 0);
		
	}

}
