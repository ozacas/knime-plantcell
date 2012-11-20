package au.edu.unimelb.plantcell.io.ws.uniprot;

/**
 * Thrown when a uniprot entry is requested which is no longer current.
 * 
 * @author andrew.cassin
 *
 */
public class DeletedEntryException extends Exception {

	/**
	 * for serialisation, not for normal use
	 */
	private static final long serialVersionUID = -5537482140312037794L;

	public DeletedEntryException(String msg) {
		super(msg);
	}
}
