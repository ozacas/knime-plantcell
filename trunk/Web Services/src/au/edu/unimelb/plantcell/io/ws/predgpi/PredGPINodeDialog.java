package au.edu.unimelb.plantcell.io.ws.predgpi;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;



public class PredGPINodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	public PredGPINodeDialog() 	{
		addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(PredGPINodeModel.CFGKEY_SEQUENCE, "Sequence"), 
				"Protein sequences to predict", 0, SequenceValue.class));
		
		addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(PredGPINodeModel.CFGKEY_MODEL, "General"), 
				"omega-site prediction model to use", new String[] { "General", "Conservative"}));
	}
		

	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
		// NO-OP: dont load any settings the dialog doesnt need...
	}
	
	@Override
	public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		settings.addString(PredGPINodeModel.CFGKEY_URL, PredGPINodeModel.DEFAULT_URL);
	}
}
