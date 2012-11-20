package au.edu.unimelb.plantcell.io.ws.suba;

import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;



public class SubaNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	public SubaNodeDialog() 	{
		addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(SubaNodeModel.CFGKEY_ACCSN, ""), 
				"AGI column (eg. AT1g04430.1)", 0, StringValue.class));
		
	}
		

	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
		// NO-OP: dont load any settings the dialog doesnt need...
	}
	
	@Override
	public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		// not in the UI for now, hardcoded
		settings.addString(SubaNodeModel.CFGKEY_URL, SubaNodeModel.DEFAULT_SUBA_URL);
	}
}
