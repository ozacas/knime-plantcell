package au.edu.unimelb.plantcell.io.ws.signalp_nectar;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;



public class SignalPAccessorNodeDialog extends DefaultNodeSettingsPane {

	public SignalPAccessorNodeDialog() 	{
		addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(SignalPAccessorNodeModel.CFGKEY_SEQUENCE, "Sequence"), 
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
		
		addDialogComponent(new DialogComponentButtonGroup(
	        		new SettingsModelString(SignalPAccessorNodeModel.CFGKEY_ORGANISM_TYPE, SignalPAccessorNodeModel.ORGANISM_TYPES[0]),
	        		false, "Organism type", SignalPAccessorNodeModel.ORGANISM_TYPES
	        		));
		
        createNewGroup("Advanced");
      
        addDialogComponent(new DialogComponentButtonGroup(
        		new SettingsModelString(SignalPAccessorNodeModel.CFGKEY_METHOD, "best"), false, "SignalP algorithm", SignalPAccessorNodeModel.METHODS
        		));
        
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelIntegerBounded(SignalPAccessorNodeModel.CFGKEY_LENGTH_CUTOFF, 70, 0, 10000), 
        		"Analyse ... N-terminal residues (0 for no limit)", 70 
        		));
      
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDoubleBounded(SignalPAccessorNodeModel.CFGKEY_TM_CUTOFF, -1, -1.0, 1.0),
        		"TM Cutoff (-1 for default cutoff)", 0.1
        		));
        
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDoubleBounded(SignalPAccessorNodeModel.CFGKEY_NOTM_CUTOFF, -1, -1.0, 1.0),
        		"no-TM Cutoff (-1 for default cutoff)", 0.1
        		));
        
        addDialogComponent(new DialogComponentString(
				new SettingsModelString(SignalPAccessorNodeModel.CFGKEY_URL, 
						SignalPAccessorNodeModel.getDefaultEndpoint()), 
						"Endpoint address"));

	}
	
}
