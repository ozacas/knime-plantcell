package au.edu.unimelb.plantcell.core.cells;

import java.util.Map;

import org.knime.core.data.DataCell;

/**
 * When an object needs to be able to be serialised into a table, make sure it implements this contract
 * for the required functionality.
 * 
 * @author andrew.cassin
 *
 */
public interface TableMappable {
	/**
	 * Returns a set of cells, all named (keys in the map) with prefix. Not all
	 * attributes need be returned (missing cells will be given for any columns missing).
	 * See {@link RegionAnnotation} for an example of the <code>asColumnSpec()</code>
	 * method which defines what columns are permitted by a given annotation.
	 * 
	 * @param prefix
	 * @return
	 */
	public Map<String,DataCell> asCells(String prefix);
}
