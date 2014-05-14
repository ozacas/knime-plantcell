package au.edu.unimelb.plantcell.io.ws.netnglyc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.edu.unimelb.plantcell.core.UniqueID;


/**
 * Represents a set of results from NetNGlyc (all positive results within a batch)
 * 
 * @author acassin
 *
 */
public class GlycResults {
	private final static Pattern p = Pattern.compile("^(S\\d+)\\s+(\\d+)\\s+([A-Z]+)\\s+([\\d\\.]+)\\s+\\((.*)\\)\\s+(\\S+)$");
	private final static Map<String,GlycResults> results_map = new HashMap<String,GlycResults>();

	private String        id;
	private List<Integer> positive_sites  = new ArrayList<Integer>();
	private List<String>  positive_sites_aa= new ArrayList<String>();
	private List<Double>  potential        = new ArrayList<Double>();
	private List<String>  jury_agreement   = new ArrayList<String>();
	private List<String>  result           = new ArrayList<String>();
	
	public GlycResults(String id) {
		assert(id != null && id.startsWith("S") && id.length() > 0);
		this.id = id;
	}

	public static GlycResults grokLine(final String line) {
		Matcher m = p.matcher(line);
		if (!m.matches()) {
			return null;
		}
		String      id = m.group(1);
		GlycResults gr = results_map.get(id);
		if (gr == null) {
			gr = new GlycResults(id);
			gr.addHit(m);
			results_map.put(id, gr);
		} else {
			gr.addHit(m);
		}
		return gr;
	}

	public int getNumGlycosylationSites() {
		return positive_sites.size();
	}
	
	// first AA is position 1...
	public List<Integer> getSites() {
		return positive_sites;
	}
	
	public List<Double> getPotentials() {
		return potential;
	}
	
	public List<String> getJuryAgreement() {
		return jury_agreement;
	}
	
	private void addHit(final Matcher cols) {
		assert(cols != null);
		positive_sites.add(Integer.valueOf(cols.group(2)));
		positive_sites_aa.add(cols.group(3));
		potential.add(Double.valueOf(cols.group(4)));
		jury_agreement.add(cols.group(5));
		result.add(cols.group(6));
	}

	public static GlycResults lookup(final UniqueID uid) {
		return results_map.get(uid.toString());
	}

	public static void reset() {
		results_map.clear();
	}
}
