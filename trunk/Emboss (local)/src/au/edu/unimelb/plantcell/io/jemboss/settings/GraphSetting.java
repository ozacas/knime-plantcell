package au.edu.unimelb.plantcell.io.jemboss.settings;

import java.util.HashMap;

import au.edu.unimelb.plantcell.io.jemboss.local.ProgramSettingsListener;

/**
 * We will only support a subset of EMBOSS formats at this stage (many of them are not in widespread use anymore)
 * @author andrew.cassin
 *
 */
public class GraphSetting extends ListSetting {

	public GraphSetting(HashMap<String,String> attrs) {
		super(attrs);
		attrs.put("list-items", "PNG: Portable Network Graphics;"+
				"PDF: Adobe Portable Document Format;"+
				"SVG: Scalable Vector Graphics");
		setListItems(attrs);
	}
	
	/**
	 * Returns a three-letter code denote the file format requested for this setting (in lower-case)
	 * @return
	 */
	public String getGraphFormat() {
		return getValue().substring(0,3).toLowerCase();
	}
	
	public boolean isPNG() {
		return (getGraphFormat().equals("png"));
	}
	
	public boolean isSVG() {
		return (getGraphFormat().equals("svg"));
	}
	
	@Override
	public void getArguments(ProgramSettingsListener l) {
		l.addArgument(this, new String[] { "-"+getName(),  getGraphFormat() }, true);
	}
	
	public static boolean canEmboss(String acd_type) {
		if (acd_type.equals("graph") || acd_type.equals("xgraph") || acd_type.equals("xygraph")) {
			return true;
		}
		return false;
	}
}
