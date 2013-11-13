package au.edu.unimelb.plantcell.io.ws.biomart;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;



public class BiomartAccessorNodeDialog extends DefaultNodeSettingsPane {

	public BiomartAccessorNodeDialog() 	{
		addDialogComponent(
				new DialogComponentString(
						new SettingsModelString(BiomartAccessorNodeModel.CFGKEY_URL, ""), "Biomart Mart URL")
				);
		
	}
	
}
