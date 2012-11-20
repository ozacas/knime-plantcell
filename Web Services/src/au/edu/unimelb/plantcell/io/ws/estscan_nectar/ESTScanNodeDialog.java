package au.edu.unimelb.plantcell.io.ws.estscan_nectar;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;



public class ESTScanNodeDialog extends DefaultNodeSettingsPane {

	public ESTScanNodeDialog() 	{
		addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(ESTScanNodeModel.CFGKEY_SEQUENCE, "Sequence"), 
				"Nucleotide sequences from...", 0, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						if (colSpec.getType().isCompatible(SequenceValue.class)) {
							return true;
						}
						
						return false;
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable Sequence columns!";
					}
			
		}));
		
		String[] models = ESTScanNodeModel.getAvailableModels();
		addDialogComponent(new DialogComponentStringSelection(
        		new SettingsModelString(ESTScanNodeModel.CFGKEY_PROGRAM, models[0]),
        		"Available scoring models", models
        		));
       
	
        createNewGroup("Advanced");
        addDialogComponent(new DialogComponentString(
				new SettingsModelString(ESTScanNodeModel.CFGKEY_URL, 
						ESTScanNodeModel.getDefaultEndpoint()), 
						"Endpoint address (advanced users only)"));

	}
	
}
