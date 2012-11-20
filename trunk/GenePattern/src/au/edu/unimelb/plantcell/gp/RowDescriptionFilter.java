package au.edu.unimelb.plantcell.gp;

import org.knime.core.data.DataRow;

/**
 * returns a text description of the specified datarow. It is up to the implementation
 * to determine what cell/value should be the description for the row. Used in the heatmap view
 * 
 * @author andrew.cassin
 *
 */
public interface RowDescriptionFilter {
	public String getRowDescription(DataRow r);
	
}
