package au.edu.unimelb.plantcell.io.ws.golgip;

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



public class GolgiPNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	public GolgiPNodeDialog() 	{
		addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(GolgiPNodeModel.CFGKEY_SEQUENCE, "Sequence"), 
				"Protein sequences to predict", 0, SequenceValue.class));
		
		addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(GolgiPNodeModel.CFGKEY_MODEL, "Comprehensive"), 
				"Model to use for prediction", new String[] { "Comprehensive", "Functional domain", "Transmembrane domain", "Dipeptide composition"}));
	}
		

	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
		// NO-OP: dont load any settings the dialog doesnt need...
	}
	
	@Override
	public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		// not in the UI for now, hardcoded
		settings.addString(GolgiPNodeModel.CFGKEY_URL, "http://csbl1.bmb.uga.edu/GolgiP/GolgiP_run.php");
		settings.addString(GolgiPNodeModel.CFGKEY_ORGANISM, "plant");
	}
}
