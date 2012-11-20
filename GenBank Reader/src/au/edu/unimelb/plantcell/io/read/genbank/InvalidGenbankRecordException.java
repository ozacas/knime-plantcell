package au.edu.unimelb.plantcell.io.read.genbank;

/**
 * Wrapper class which is thrown when a record is encountered which does not
 * meet mandatory genbank format requirements eg. missing accession in a given record
 * 
 * @author andrew.cassin
 *
 */
public class InvalidGenbankRecordException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1150619818581765224L;

	public InvalidGenbankRecordException(String msg) {
		super(msg);
	}
}
