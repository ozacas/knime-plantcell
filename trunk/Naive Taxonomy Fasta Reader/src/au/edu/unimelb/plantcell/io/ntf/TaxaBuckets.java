package au.edu.unimelb.plantcell.io.ntf;

import java.util.HashSet;
import java.util.Set;

public class TaxaBuckets {
	private final Set<String> m_bucket = new HashSet<String>();
	
	public TaxaBuckets() {
	}
	
	public void add(String taxa) {
		m_bucket.add(taxa.toLowerCase());
	}
	
	public boolean matches(String taxa) {
		for (String s : m_bucket) {
			if (taxa.toLowerCase().startsWith(s)) {
				return true;
			}
		}
		return false;
	}
}
