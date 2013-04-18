package au.edu.unimelb.plantcell.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.node.InvalidSettingsException;


/**
 * Used to map results from external programs to sequences within KNIME. An important class with great care
 * taken in the definition of the methods here. Change with caution and test BLAST/EMBOSS/... with care!
 * 
 * @author andrew.cassin
 *
 */
public class UniqueID {
	public static int cur = 1;
	private final int m_id;
	private static Pattern p = Pattern.compile("^[^\\d]+?(\\d+)");
	
	public UniqueID(boolean restart) {
		if (restart) 
			restart();
		m_id = cur++;
	}
	
	public UniqueID() {
		this(false);
	}
	
	/**
	 * Takes an id from (eg. EMBOSS results) like "S10_1" and instantiate a UniqueID with
	 * the id 10. Used to identify which sequence in a batch the reported result relates to. We cant use
	 * the user-supplied sequence ID's as they may be incompatible with the chosen program. 
	 * 
	 * @param numeric_seq_id a string like "S10_1". Must not be null.
	 * @throws InvalidSettingsException thrown only if the input does not match the required format
	 */
	public UniqueID(String numeric_seq_id) throws InvalidSettingsException {
		Matcher m = p.matcher(numeric_seq_id);
		if (!m.find())
			throw new InvalidSettingsException("Expected numeric id of the form: <letters><digit>+<other stuff>!");
		m_id = Integer.valueOf(m.group(1));
	}

	private static synchronized void restart() {
		cur = 1;
	}
	
	public String toString() {
		return "S"+m_id;
	}

	public boolean equals(Object o) {
		if (o instanceof UniqueID) {
			return ((UniqueID)o).m_id == this.m_id;
		} else if (o instanceof String) {
			return o.toString().equals(this.toString());
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	public static boolean smellsOk(String str) {
		return str.startsWith("S");
	}
}
