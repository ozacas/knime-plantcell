package au.edu.unimelb.plantcell.io.ws.blast.wublast;

/**
 * Thrown when the XML Blast Parser cannot find XML to parse (causes a text parser to be invoked)
 * @author andrew.cassin
 *
 */
public class UnsuitableParserException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public UnsuitableParserException() {
		this("BLAST results parser cannot recognise data from BLAST");
	}
	
	public UnsuitableParserException(String msg) {
		super(msg);
	}
}
