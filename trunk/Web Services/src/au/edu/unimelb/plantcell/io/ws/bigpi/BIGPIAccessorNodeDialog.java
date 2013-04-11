package au.edu.unimelb.plantcell.io.ws.bigpi;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;



public class BIGPIAccessorNodeDialog extends DefaultNodeSettingsPane {

	public BIGPIAccessorNodeDialog() 	{
		addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(BIGPIAccessorNodeModel.CFGKEY_SEQUENCE, "Sequence"), 
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
						return "No available Sequence column!";
					}
			
		}));
		
		addDialogComponent(new DialogComponentButtonGroup(
				new SettingsModelString(BIGPIAccessorNodeModel.CFGKEY_ORGANISM_TYPE, 
				BIGPIAccessorNodeModel.ORGANISM_TYPES[0]), true, "Organism type", BIGPIAccessorNodeModel.ORGANISM_TYPES));
		
		addDialogComponent(new DialogComponentString(
				new SettingsModelString(BIGPIAccessorNodeModel.CFGKEY_URL, 
						BIGPIAccessorNodeModel.getDefaultEndpoint()), 
						"Endpoint address (advanced users only)"));
	}
	
}
