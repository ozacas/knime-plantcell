package au.edu.unimelb.plantcell.io.ws.psort;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;



public class PSortNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	public PSortNodeDialog() 	{
		addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(PSortNodeModel.CFGKEY_SEQUENCE, "Sequence"), 
				"Protein sequences column", 0, SequenceValue.class));
		
		addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(PSortNodeModel.CFGKEY_ORGANISM, "Plant"), 
				"Model to use for prediction", new String[] { "Plant", "Animal", "Fungi"}));
		
		addDialogComponent(new DialogComponentString(new SettingsModelString(PSortNodeModel.CFGKEY_URL, PSortNodeModel.getDefaultEndpoint()), "Endpoint address (advanced users only)"));
	}
	
}
