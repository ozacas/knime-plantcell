package au.edu.unimelb.plantcell.io.jemboss.simple;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * If you this class, do not call the superclass add/remove methods directly. Instead
 * use the methods provided here.
 * 
 * @author andrew.cassin
 *
 */
public class UserSettingsPanel extends JPanel {
	public final static String CFGKEY_USER_FIELDS = "user-defined-fields";
	
	/**
	 * used for serialisation only
	 */
	private static final long serialVersionUID = -8958554007895490696L;
	private Map<String,UserField> m_cur_fields = new HashMap<String,UserField>();
	private ArrayList<UserField> m_cur_fields_ordered = new ArrayList<UserField>();
	
	public UserSettingsPanel() {
		super();
	}
	
	@Override
	public void removeAll() {
		super.removeAll();
		m_cur_fields.clear();
		m_cur_fields_ordered.clear();
	}

	public boolean hasCurrentField(String name) {
		return m_cur_fields.containsKey(name);
	}

	public Component add(UserField c) {
		assert(c != null);
		m_cur_fields_ordered.add(c);
		m_cur_fields.put(c.getName(), c);
		return super.add(c.getWidget());
	}
	
	public void remove(String name) {
		if (!hasCurrentField(name))
			return;
		m_cur_fields.remove(name);
		for (int i=0; i<m_cur_fields_ordered.size(); i++) {
			UserField f = m_cur_fields_ordered.get(i);
			if (f.hasName(name)) {
				super.remove(i);
				m_cur_fields_ordered.remove(i);
				return;
			}
		}
	}

	/**
	 * The format of the <code>CFGKEY_USER_FIELDS</code> must contain line-separated
	 * field=value pairs. Each pair must identify the command line argument (field) and
	 * its value for invoking the current EMBOSS program as the models depend on that.
	 * 
	 * @param settings
	 */
	public void saveAdditionalSettingsTo(NodeSettingsWO settings) {
		StringBuilder sb = new StringBuilder();
		for (UserField uf : m_cur_fields_ordered) {
			sb.append(uf.getName()+" = "+uf.getValue());
			sb.append("\n");
		}
		settings.addString(CFGKEY_USER_FIELDS, sb.toString());
	}
	
	public void loadAdditionalSettingsFrom(NodeSettingsRO settings) {
		String s;
		String progName;
		try {
			s = settings.getString(CFGKEY_USER_FIELDS);
			progName = settings.getString(EmbossPredictorNodeModel.CFGKEY_PROGRAM);
			int idx = progName.indexOf(":");
			if (idx >= 0) 
				progName = progName.substring(0, idx);
			if (s == null)
				return;
		} catch (InvalidSettingsException e) {
			e.printStackTrace();
			return;
		}
		removeAll();
		
		String[] fields = s.split("\n");
		Pattern p = Pattern.compile("^(\\S+) = (.*)$");
		ACDApplication prog = null;
		try {
			prog = ACDApplication.find(progName);
			if (prog == null)
				return;
		} catch (Exception e) {
			e.printStackTrace();
		} 
		for (String field : fields) {
			Matcher m = p.matcher(field);
			if (m.matches()) {
				String     f = m.group(1);
				String    val= m.group(2);
				ACDField acd_field = prog.getField(f);
				if (f != null) {
					try {
						UserField uf = new UserField(acd_field);
						add(uf);
						uf.setValue(val);
					} catch (InvalidSettingsException ise) {
						ise.printStackTrace();
					}
				}
			}
		}
	}
	
}
