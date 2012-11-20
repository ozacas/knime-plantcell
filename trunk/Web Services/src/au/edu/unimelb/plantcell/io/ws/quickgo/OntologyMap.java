package au.edu.unimelb.plantcell.io.ws.quickgo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds the entries from an OBO-formatted GO entry in naive format
 * 
 * @author andrew.cassin
 *
 */
public class OntologyMap {
	private final HashMap<String,List<String>> m_om = new HashMap<String,List<String>>();
	private static Pattern m_build_pattern = Pattern.compile("^(\\w+):\\s+(.*)$");
	
	public OntologyMap() {
		
	}
	
	public void build(String line) {
		Matcher m = m_build_pattern.matcher(line);
		if (m.matches()) {
			String key = m.group(1).toLowerCase();
			String val = m.group(2);
			List<String> terms;
			if (!m_om.containsKey(key)) {
				terms = new ArrayList<String>();
				m_om.put(key, terms);
			} else {
				terms = m_om.get(key);
			}
			terms.add(val);
		}
	}

	public String getDefinition() {
		if (has("def")) {
			return m_om.get("def").get(0);
		}
		return null;
	}

	private boolean has(String s) {
		return m_om.containsKey(s);
	}

	public String getName() {
		if (has("name")) {
			return m_om.get("name").get(0);
		}
		return null;
	}

	public List<String> getXrefs() {
		return m_om.get("xref");
	}

	public List<String> getSynonyms() {
		return m_om.get("synonym");
	}

	public List<String> getISArelations() {
		return m_om.get("is_a");
	}

}
