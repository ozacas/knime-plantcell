package au.edu.unimelb.plantcell.io.jemboss.settings;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;

import au.edu.unimelb.plantcell.io.jemboss.local.ProgramSettingsListener;

public class DummySetting extends ProgramSetting {

	public DummySetting(HashMap<String,String> attrs) {
		super(attrs);
	}
	
	@Override
	public String getColumnName() {
		return null;
	}

	@Override
	public void getArguments(ProgramSettingsListener l) throws Exception {
	}

	@Override
	public void marshal(String id, DataCell c, PrintWriter fw)
			throws IOException, InvalidSettingsException {
	}

	@Override
	public JComponent make_widget(DataTableSpec dt) {
		return new JLabel(getName()+": "+getType());
	}
	
	public static boolean canEmboss(String acd_type) {
		return true;
	}

}
