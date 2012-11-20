package au.edu.unimelb.plantcell.algorithms.StringFinder;

import org.knime.core.data.DataCell;

public interface MatchReporter {
	
	/**
	 * Called during the execute() method, this must provide the report based on the
	 * match results (current state of m) and the string being matched (str). If the 
	 * reporter encounters a problem it may throw an exception to stop executing the node.
	 * 
	 * @param m
	 * @param input_cell the input string-value cell (often not an instance of StringCell)
	 * @throws Exception
	 */
	public DataCell report(FindGlobalNodeModel m, DataCell input_cell) throws Exception;
}
