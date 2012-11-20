package au.edu.unimelb.plantcell.io.ws.interpro;

/**
 * This gets thrown when a job fails (perhaps due to the input data) at EBI
 * @author andrew.cassin
 *
 */
public class FailedJobException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2861131225558890869L;

	public FailedJobException(String s) {
		super(s);
	}
}
