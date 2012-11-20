package au.edu.unimelb.plantcell.io.jemboss.settings;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;

import au.edu.unimelb.plantcell.io.jemboss.local.ProgramSettingsListener;

/**
 * Represents a series of characters on the command-line (text-box) for an emboss program invocation
 * 
 * @author andrew.cassin
 *
 */
public class StringSetting extends ProgramSetting {
	private String m_val;
	
	public StringSetting(HashMap<String,String> attrs) {
		super(attrs);
		if (hasAttribute("value"))
			m_val = attrs.get("value");
		else 
			m_val = getDefaultValue();
	}

	protected String getValue() {
		return m_val;
	}
	
	protected void setValue(String new_val) {
		m_val = new_val;
	}

	@Override
	public String getColumnName() {
		return null;
	}

	@Override
	public JComponent make_widget(DataTableSpec dt) {
		JFormattedTextField tf = new JFormattedTextField(getDefaultValue());
		tf.setMinimumSize(new Dimension(100,20));
		tf.setText(getValue());
		tf.setColumns(30);
		tf.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				setValue(((JTextField)arg0.getSource()).getText());
			}
			
		});
		return tf;
	}

	@Override
	public void copy_attributes(HashMap<String,String> attrs) {
		super.copy_attributes(attrs);
		attrs.put("value", getValue());
	}

	@Override
	public void getArguments(ProgramSettingsListener l) throws Exception {
		String val = getValue();
		if (val.length() < 1) {			// pass empty argument correctly eg. 'exclude' argument for jaspscan
			val = "\"\"";
		}
		l.addArgument(this, new String[] {"-"+getName(), val});
	}

	@Override
	public void marshal(String id, DataCell c, PrintWriter fw)
			throws IOException, InvalidSettingsException {
		// TODO Auto-generated method stub

	}

	public static boolean canEmboss(String acd_type) {
		if (acd_type.equals("string"))
			return true;
		return false;
	}

}
