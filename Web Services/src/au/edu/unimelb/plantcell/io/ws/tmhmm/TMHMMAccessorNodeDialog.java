package au.edu.unimelb.plantcell.io.ws.tmhmm;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;



public class TMHMMAccessorNodeDialog extends DefaultNodeSettingsPane {

	public TMHMMAccessorNodeDialog() 	{
		addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(TMHMMAccessorNodeModel.CFGKEY_SEQUENCE, "Sequence"), 
				"Protein sequences to predict", 0, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						if (colSpec.getType().isCompatible(SequenceValue.class)) {
							return true;
						}
						
						return false;
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable String or Sequence columns available!";
					}
			
		}));
		
		addDialogComponent(new DialogComponentString(
				new SettingsModelString(TMHMMAccessorNodeModel.CFGKEY_URL, 
						TMHMMAccessorNodeModel.getDefaultEndpoint()), 
						"Endpoint address (advanced users only)"));

	}
	
}
