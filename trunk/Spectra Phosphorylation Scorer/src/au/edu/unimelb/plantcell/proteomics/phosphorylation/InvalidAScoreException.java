package au.edu.unimelb.plantcell.proteomics.phosphorylation;

/**
 * thrown when the AScore calculation determines an invalid state, will halt execution of the node
 * 
 * @author andrew.cassin
 *
 */
public class InvalidAScoreException extends Exception {
	/**
	 * ignored
	 */
	private static final long serialVersionUID = 2257283857220191529L;
	
	public InvalidAScoreException(String msg) {
		super(msg);
	}
}
