package au.edu.unimelb.plantcell.io.ws.golgip;

import org.knime.core.node.CanceledExecutionException;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Interface to interact with the NodeModel and output the rows for the node's output
 * ports and to handle user interaction. Called by the <code>Form</code> instance, when
 * it has extracted a result supplied by GolgiP.
 * 
 * @author andrew.cassin
 *
 */
public interface Callback {

	/*
	 * Called when waiting for the final page to arrive, this method must
	 * update the node status to indicate waiting
	 */
	public void updateStatus() throws CanceledExecutionException;

	/**
	 * Each <code>prediction_columns</code> is given three values:
	 * [1]: localisation prediction (either golgi or not)
	 * [2]: score
	 * [3]: accuracy (% ie. 0..100)
	 * @param sv protein sequence from input to node
	 * @param prediction_columns
	 */
	public void process_predictions(SequenceValue sv, String[] prediction_columns);

	/**
	 * For identified conserved domains within a sequence, this routine is called
	 * once per CDD. The five data values given each call are:
	 * [0]: protein sequence ID
	 * [1]: Bit score
	 * [2]: CDD ID
	 * [3]: CDD Name (abbreviated)
	 * [4]: link to CDD (currently buggy)
	 * @param cdd_columns
	 */
	public void process_conserved_domains(String[] cdd_columns);

	/**
	 * Responsible for logging a warning
	 * @param string
	 */
	public void warn(String string);
}
