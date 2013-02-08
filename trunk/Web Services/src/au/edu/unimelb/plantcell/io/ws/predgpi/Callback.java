package au.edu.unimelb.plantcell.io.ws.predgpi;

import org.knime.core.node.CanceledExecutionException;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Interface to interact with the NodeModel and output the rows for the node's output
 * ports and to handle user interaction. Called by the <code>Form</code> instance, when
 * it has extracted a result supplied by PredGPI.
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
	 * Responsible for logging a warning
	 * @param string
	 */
	public void warn(String string);


	/**
	 * Process a prediction from the webserver
	 * @param sv
	 * @param omega_site
	 * @param fp_r
	 * @param key 
	 */
	public void addPrediction(SequenceValue sv, String omega_site, String fp_r, String key);
}
