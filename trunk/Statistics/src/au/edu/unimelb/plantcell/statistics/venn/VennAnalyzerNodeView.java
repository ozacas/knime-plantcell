package au.edu.unimelb.plantcell.statistics.venn;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.JSVGScrollPane;
import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeView;
import org.osgi.framework.Bundle;

/**
 * Renders the node model state (venn summary results) using Apache Batik
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class VennAnalyzerNodeView extends NodeView<VennAnalyzerNodeModel> {
	private final JSVGCanvas canvas = new JSVGCanvas();
	
	protected VennAnalyzerNodeView(VennAnalyzerNodeModel nodeModel) {
		super(nodeModel);
		JSVGScrollPane sp = new JSVGScrollPane(canvas);
		
		sp.setPreferredSize(new Dimension(600,600));
		this.setComponent(sp);
	}
	
	@Override
	protected void onClose() {
		// NO-OP: maybe cleanup batik?
	}

	@Override
	protected void onOpen() {
		modelChanged();
	}

	private int countPiesInMap(final Map<List<String>,Integer> map) {
		int n = 0;
		for (List<String> key : map.keySet()) {
			if (key.size() == 1) {
				n++;
			}
		}
		return n;
	}
	
	@Override
	protected void modelChanged() {
		VennAnalyzerNodeModel mdl = getNodeModel();
		
		if (mdl != null) {
			VennModel vmdl = mdl.getVennModel();
			if (vmdl != null) {
				setShowNODATALabel(false);
				Map<List<String>,Integer> venn = vmdl.getVennMap();
				int n_pies = countPiesInMap(venn);
				Map<String,String> substitution_map = makeVennMap(venn);
				
				/*Logger.getAnonymousLogger().info("****** Venn modelChanged()");
				for (String key : venn.keySet()) {
					Logger.getAnonymousLogger().info(key + " = " + String.valueOf(venn.get(key)));
				}*/
				Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.statistics");
				URL u = FileLocator.find(bundle, new Path("/svg/"+n_pies+"-way-venn.svg"), null);
				if (u != null) {			// is there a template for this size of venn diagram?
					File venn_svg = saveSVGTemplateWithMap(u, substitution_map);
					Logger.getAnonymousLogger().info("Loading SVG template: "+venn_svg.getAbsolutePath());
					canvas.setURI(venn_svg.toURI().toString());
					venn_svg.deleteOnExit();
					return;
				}
			}
		}
		
		// if we get here, then we have no suitable data to display on the canvas...
		setShowNODATALabel(true);
	}

	/**
	 * Compute the substitution map values to make on the specified venn results. So this will
	 * construct a map like:
	 * { "A" => CategoryA, "B" => CategoryB, "CNT_A" => 23, "CNT_AB" => 40, "CNT_B" => 1, "AB" => "Category A AND B" }
	 * 
	 * for a two-way venn diagram which has been computed during execute()
	 * 
	 * @param venn
	 * @return map to substitute into the SVG template (must not be null). The map values will be safe for insertion into XML
	 */
	private Map<String, String> makeVennMap(final Map<List<String>, Integer> venn) {
		Map<String,String> subst_map = new HashMap<String,String>();
		
		// 1. put wedges into map { "A" => "Category A", "B" => "Category B"} only excludes single category maps eg. AB is excluded
		char letter = 'A';
		for (List<String> key : venn.keySet()) {
			if (key.size() == 1) {
				subst_map.put(""+letter, StringEscapeUtils.escapeXml(key.get(0)));
				letter++;
			}
		}
		
		// 2. now since all the wedges are in subst_map we can put the counts into the subsitution map using the correct key
		for (List<String> key : venn.keySet()) {
			String letter_key = key2letter(key, subst_map);
			subst_map.put("CNT_"+letter_key, String.valueOf(venn.get(key)));
			subst_map.put(letter_key, StringEscapeUtils.escapeXml(key2string(key)));
		}
		return subst_map;
	}
	
	private String key2string(List<String> key) {
		StringBuilder sb = new StringBuilder();
		int n = key.size();
		for (int i=0; i<n; i++) {
			if (i > 0) {
				sb.append(" AND ");
			}
			sb.append(key.get(0));
		}
		return sb.toString();
	}

	private String key2letter(final List<String> k, final Map<String,String> subst_map) {
		List<String> letters = new ArrayList<String>(k.size());
		
		// 1. invert subst_map into rev
		HashMap<String,String> rev = new HashMap<String,String>();
		for (String r : subst_map.keySet()) {
			rev.put(subst_map.get(r), r);
		}
		
		// 2. build letter vector from inverted map
		for (String j : k) {
			String letter = rev.get(j);
			if (letter == null)	// must not happen: something wrong with model!
				return null;
			letters.add(rev.get(j));
		}
		// the returned letter vector must be sorted alphabetically or it wont match the template
		// eg. CNT_BA does not exist in the SVG template, but CNT_AB does
		Collections.sort(letters);
		
		// 3. return final result
		StringBuilder ret = new StringBuilder();
		for (String j : letters) {
			ret.append(j);
		}
		return ret.toString();
	}

	/**
	 * This code relies on the fact that the SVG contains @KEY@ only where a substitution from the map is
	 * required. Be careful with the template creation to ensure this.
	 * 
	 * @param u SVG XML template to read and perform substitutions from the map
	 * @param substitution_map
	 * @return null if an error occurs (a stacktrace is printed). Otherwise a File is returned with the substituted SVG. Caller must delete.
	 */
	private File saveSVGTemplateWithMap(final URL u, final Map<String, String> substitution_map) {
		BufferedReader rdr = null;
		PrintWriter pw = null;
		try {
			rdr = new BufferedReader(new InputStreamReader(u.openConnection().getInputStream()));
			File ret = File.createTempFile("input_venn", "_substituted.svg");
			pw = new PrintWriter(new FileWriter(ret));
			String line;
			while ((line = rdr.readLine()) != null) {
				line = makeLineWithSafeSubstitutions(substitution_map, line);
				pw.println(line);
			}
			pw.close();
			return ret;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		} finally {
			if (rdr != null || pw != null) {
				try {
					if (rdr != null)
						rdr.close();
					if (pw != null)
						pw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Replace the markers in the SVG input line (as specified by line) with the
	 * corresponding data points (and titles) from the model as specified by the <code>substitution_map</code>
	 * 
	 * @param substitution_map must not be NULL and should not be empty
	 * @param line must not be null
	 * @return the substituted line (if any are present on the line)
	 */
	private String makeLineWithSafeSubstitutions(
			Map<String, String> substitution_map, String line) {
		assert(substitution_map != null && line != null);
		
		// performance optimisation
		if (line.indexOf('@') < 0)
			return line;
		
		// otherwise we regex...
		Pattern p = Pattern.compile("@(\\w+)@");
		Matcher m = p.matcher(line);
		String ret = new String(line);
		while (m.find()) {
			String key = m.group(1);
			if (substitution_map.containsKey(key)) {
				ret = ret.replaceAll("@"+key+"@", StringEscapeUtils.escapeXml(substitution_map.get(key)));
			} else {
				// KEY not in map... we could throw but for now we are silent in case the SVG has @'s in them...
			}
		}
		return ret;
	}
	
}