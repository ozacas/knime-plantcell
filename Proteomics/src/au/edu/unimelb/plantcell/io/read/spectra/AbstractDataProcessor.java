package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * An interface to permit the model to be generic to any format. The model simply invokes
 * these interface methods to get the data into the KNIME table. Readers for a particular format
 * must implement the methods below.
 * 
 * @author andrew.cassin
 *
 */
public abstract class AbstractDataProcessor {
	
	public AbstractDataProcessor() {
	}
	
	/**
	 * Give the DP a chance to capture the data and its location (id) which is typically a filename
	 * 
	 * @param id
	 */
	public abstract void setInput(String id) throws Exception;
	
	/**
	 * Can this processor handle the specified file?
	 */
	public abstract boolean can(File f) throws Exception;
	
	/**
	 * Loads the data from the file into the KNIME output ports, responsible for checking that the
	 * user has not cancelled and updating the progress bar for the specified execution context.
	 * 
	 * @param load_spectra
	 * @param exec
	 * @param scan_container
	 * @param file_container
	 * @return
	 */
	public abstract void process(boolean load_spectra, ExecutionContext exec, 
			MyDataContainer scan_container, MyDataContainer file_container)
				throws Exception;
	
	
	/**
	 * Called once per file once all processing of the file has ended, this routine must
	 * perform any cleanup required and return true if processing was successful or false
	 * otherwise (in which case another data processor may be tried if it supports the file)
	 */
	public boolean finish() {
		return true;
	}
	
	/**
	 * KNIME throws exceptions at null cells, so we return a missing cell instead
	 */
    protected DataCell safe_cell(String content) {
    	if (content != null)
    		return new StringCell(content);
    	else
    		return DataType.getMissingCell();
    }
    
    /**
     * Return an initialised array of {@link DataType.getMissingCell()} cells of the specified length
     */
    protected DataCell[] missing_cells(int n) {
    	assert(n > 0);
    	DataCell[] ret = new DataCell[n];
    	for (int i=0; i<n; i++) {
    		ret[i] = DataType.getMissingCell();
    	}
    	return ret;
    }
}
