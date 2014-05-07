package au.edu.unimelb.plantcell.core;

/**
 * Global methods for strings which are widely used in the codebase. Callers are encouraged to use these
 * methods as they make nodes more useful to most needs.
 * 
 * @author acassin
 *
 */
public class StringUtils {

	/**
	 * {@link java.lang.String.trim()} does not correctly handle non-breaking space and other whitespace
	 * characters coming from UniCode. This method builds on the String trim() by removing such additional whitespace.
	 * 
	 * @param to_be_trimmed
	 * @return trimmed string
	 */
	public static String trim(String to_be_trimmed) {
		if (to_be_trimmed == null)
			return null;
		return to_be_trimmed.replaceFirst("^[\\x00-\\x200\\xA0]+", "").replaceFirst("[\\x00-\\x200\\xA0]+$", "");
	}
}
