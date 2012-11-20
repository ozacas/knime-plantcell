package au.edu.unimelb.plantcell.io.ws.netphos_nectar;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;



public class NetPhosAccessorNodeDialog extends DefaultNodeSettingsPane {

	public NetPhosAccessorNodeDialog() 	{
		addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(NetPhosAccessorNodeModel.CFGKEY_SEQUENCE, "Sequence"), 
				"Protein sequences from...", 0, new ColumnFilter() {

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
		
		addDialogComponent(new DialogComponentString(
				new SettingsModelString(NetPhosAccessorNodeModel.CFGKEY_URL, 
						NetPhosAccessorNodeModel.getDefaultEndpoint()), 
						"Endpoint address (advanced users only)"));

		addDialogComponent(new DialogComponentButtonGroup(
        		new SettingsModelString(NetPhosAccessorNodeModel.CFGKEY_PROGRAM, NetPhosAccessorNodeModel.METHODS[0]),
        		false,
        		"Algorithm to use",
        		NetPhosAccessorNodeModel.METHODS
        		));
        
        createNewGroup("Advanced");
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(NetPhosAccessorNodeModel.CFGKEY_BEST_ONLY, false), "Best score only"));
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(NetPhosAccessorNodeModel.CFGKEY_GENERIC, false), "Generic predictions only"));
        addDialogComponent(new DialogComponentNumber(new SettingsModelDoubleBounded(NetPhosAccessorNodeModel.CFGKEY_CUTOFF, 0.5, 0.0, 1.0), "Cutoff", 0.1));
        
	}
	
}
