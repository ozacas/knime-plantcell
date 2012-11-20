package au.edu.unimelb.plantcell.io.ntf;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;

public class AcceptableTaxa {
	private final Map<String,TaxaBuckets> m_set = new HashMap<String,TaxaBuckets>(37 * 19 * 21);
	
	public AcceptableTaxa() {
		
	}
	
	public void add(String taxa) throws InvalidSettingsException {
		String[] words = taxa.split("\\s+");
		if (words[0] == null || words[0].length() < 1) 
			throw new InvalidSettingsException("Invalid taxa: "+taxa);
		TaxaBuckets tb = m_set.get(words[0].toLowerCase());
		if (tb == null) {
			tb = new TaxaBuckets();
			m_set.put(words[0].toLowerCase(), tb);
			// FALLTHRU
		}
		tb.add(taxa);
	}
	
	public boolean match(String find_me) {
		if (find_me == null || find_me.length() < 1)
			return false;
		String[] words = find_me.split("\\s+");
		TaxaBuckets tb = m_set.get(words[0]);
		if (tb == null)
			return false;
		return tb.matches(find_me);
	}
}
