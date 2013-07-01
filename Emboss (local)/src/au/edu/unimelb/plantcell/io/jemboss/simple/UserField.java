package au.edu.unimelb.plantcell.io.jemboss.simple;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileFilter;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.InvalidSettingsException;

import au.edu.unimelb.plantcell.core.CorePlugin;
import au.edu.unimelb.plantcell.core.Preferences;

/**
 * Implements a widget usable from the simple dialog for advanced settings for the user
 * 
 * @author andrew.cassin
 *
 */
public class UserField extends ACDField implements ActionListener {
	private JComponent m_widget;
	private Map<String,String> m_values = new HashMap<String,String>();	// only for list items

	
	public UserField(ACDField f) {
		super(f);
	}

	public JPanel getWidget() {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		p.add(Box.createRigidArea(new Dimension(2,2)));
		JPanel label_panel = new JPanel();
		label_panel.setLayout(new BoxLayout(label_panel, BoxLayout.Y_AXIS));
		JLabel lbl = new JLabel(getName());
		lbl.setFont(Font.decode(Font.SANS_SERIF+"-bold"));
		label_panel.add(lbl);
		if (hasProperty("information")) {
			label_panel.add(new JLabel(getProperty("information")));
		}
		
		p.add(label_panel);

		p.add(Box.createHorizontalGlue());
		
		if (isIntegerWidget()) {
			m_widget = new JSpinner();
		} else if (isListWidget()) {
			m_widget = new JComboBox(getListItems());
		} else if (isBooleanWidget()) {
			m_widget = new JCheckBox();
		} else if (isFileWidget()) {
			m_widget = new JTextField();
		} else {
			// let user enter string value (for command line arg) directly
			m_widget = new JTextField();
		}
		p.add(m_widget);
		
		if (isFileWidget()) {
			JButton b = new JButton("Browse...");
			b.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					IPreferenceStore ps = CorePlugin.getDefault().getPreferenceStore();
					String emboss_dir = ps.getString(Preferences.PREFS_JEMBOSS_FOLDER);
					File data_dir = new File(emboss_dir, "data");
					if (isCodonFileWidget()) {
						data_dir = new File(data_dir, "CODONS");
					}
					if (data_dir.isDirectory()) {
						JFileChooser jfc = new JFileChooser(data_dir);
						int ok = jfc.showOpenDialog(null);
						if (ok == JFileChooser.APPROVE_OPTION) {
							((JTextField)m_widget).setText(jfc.getSelectedFile().getName());
						}
						String default_value = getProperty("default");
						int idx = -1;
						if (default_value != null) {
							idx = default_value.lastIndexOf('.');
						}
						final String ext = (idx >= 0) ? default_value.substring(idx) : null;
						jfc.setFileFilter(new FileFilter() {

							@Override
							public boolean accept(File arg0) {
								if (arg0.isDirectory())
									return true;
								if (!arg0.canRead() || !arg0.isFile())
									return false;
								if (ext == null)
									return true;
								return arg0.getName().toLowerCase().endsWith(ext);
							}

							@Override
							public String getDescription() {
								if (ext == null)
									return "All files";
								return ext.toUpperCase() + " files";
							}
							
						});
					} else {
						JOptionPane.showMessageDialog(null, "Cannot locate EMBOSS directory - please set PlantCell preferences!", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
				
			});
			p.add(b);
		}
		
		loadFieldFrom(null);
		
		JPanel ret = new JPanel();
		ret.setLayout(new BoxLayout(ret, BoxLayout.Y_AXIS));
		ret.add(p);
		ret.add(Box.createVerticalGlue());
		return ret;
	}
	
	private boolean isCodonFileWidget() {
		return getName().equals("cfile") || getType().equals("codon");
	}
	
	private boolean isFileWidget() {
		return isCodonFileWidget() || getName().equals("datafile");
	}

	private boolean isBooleanWidget() {
		String type = getType();
		return type.equals("boolean");
	}

	private String[] getListItems() {
		String[] split = getProperty("values").split(getProperty("delimiter"));
		String code_delim = getProperty("codedelimiter");
		if (code_delim == null)
			code_delim = ":";
		ArrayList<String> ret = new ArrayList<String>();
		for (String s : split) {
			int idx = s.indexOf(code_delim);
			if (idx < 0) {
				String ts = s.trim().replaceAll("\\s+", " ");
				ret.add(ts);
				m_values.put(ts, ts);
			} else {
				String val = s.substring(0, idx).trim();
				String item= s.substring(idx+code_delim.length()).replaceAll("\\s+", " ");
				ret.add(item);
				m_values.put(item, val);
			}
		}
		return ret.toArray(new String[0]);
	}

	private boolean isIntegerWidget() {
		String type = getType();
		return type.equals("integer");
	}
	
	private boolean isListWidget() {
		String type = getType();
		return type.equals("list");
	}

	
	public String getValue() {
		if (m_widget instanceof JTextField) {
			return ((JTextField)m_widget).getText();
		} else if (m_widget instanceof JSpinner) {
			return ((JSpinner) m_widget).getValue().toString();
		} else if (m_widget instanceof JComboBox) {
			return ((JComboBox) m_widget).getSelectedItem().toString();
		} else if (m_widget instanceof JCheckBox) {
			return ((JCheckBox) m_widget).isSelected() ? "Y" : "N";
		}
		
		return null;
	}

	public void loadFieldFrom(Map<String,String> props) {
		if (props == null)
			props = getProperties();
		String val = props.get(getName());
		if (val == null)
			val = getDefaultValue();
		
		if (m_widget instanceof JTextField) {
			((JTextField) m_widget).setText(val);
		} else if (m_widget instanceof JSpinner) {
			JSpinner sp = ((JSpinner)m_widget);
	
			initSpinnerModel(sp, val);
		} else if (m_widget instanceof JComboBox) {
			((JComboBox) m_widget).setSelectedItem(val);
		} else if (m_widget instanceof JCheckBox) {
			((JCheckBox) m_widget).setSelected(val.equals("Y"));
		}
	}
	
	/**
	 * Called when the widget is being set, this changes the number model
	 * of the spinner
	 */
	private void initSpinnerModel(JSpinner sp, String val) {
		assert(sp != null);
		String type = getType();
		SpinnerNumberModel snm = null;
		String s_min = getProperty("minimum");
		String s_max = getProperty("maximum");
		if (type.startsWith("int")) {
			int v = Integer.parseInt(val.trim());
			int min = 0;
			if (hasProperty("minimum"))
				min = Integer.parseInt(s_min);
			int max = Integer.MAX_VALUE;
			if (hasProperty("maximum"))
				max = Integer.parseInt(s_max);
			snm = new SpinnerNumberModel(v, min, max, (int)((max - min) / 100));
		} else {
			double v= Double.parseDouble(val.trim());
			double min = 0.0;
			if (hasProperty("minimum"))
				min = Double.parseDouble(s_min);
			double max = Double.MAX_VALUE;
			if (hasProperty("maximum"))
				max = Double.parseDouble(s_max);
			snm = new SpinnerNumberModel(v, min, max, (double)((max - min) / 100));
		}
			
		sp.setModel(snm);
	}

	private String getDefaultValue() {
		String def = getProperty("default");
		if (isListWidget() && m_values != null && m_values.size() > 0) {
			// map value to item
			for (String key : m_values.keySet()) {
				if (def.equals(m_values.get(key))) 
						return key;
			}
			// fallthru...
		} 
		if (def == null)
			def = "";
		return def;
	}
	

	public void setValue(String val) throws InvalidSettingsException {
		if (m_widget == null)
			return;
		if (m_widget instanceof JTextField) {
			((JTextField)m_widget).setText(val);
		} else if (m_widget instanceof JCheckBox) {
			((JCheckBox)m_widget).setSelected(val.startsWith("Y"));
		} else if (m_widget instanceof JSpinner) {
			SpinnerNumberModel snm = (SpinnerNumberModel) ((JSpinner)m_widget).getModel();
			if (snm.getValue() instanceof Integer) {
				snm.setValue(Integer.valueOf(val));
			} else {
				snm.setValue(Double.valueOf(val));
			}
		} else if (m_widget instanceof JComboBox) {
			// the value will be from the m_values hash, so we look it up to find the list item...
			for (String key : m_values.keySet()) {
				String v = m_values.get(key);
				if (v.equals(val)) {
					((JComboBox)m_widget).setSelectedItem(val);
					return;
				}
			}
			throw new InvalidSettingsException("Failed to find value: "+val+" for "+getName());
		} else {
			throw new InvalidSettingsException("No setValue() implementation for widget!");
		}
	}
	
	/*********** WIDGET EVENT LISTENER ****************/
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	
	
	
}
