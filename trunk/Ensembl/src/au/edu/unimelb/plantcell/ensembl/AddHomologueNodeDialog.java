package au.edu.unimelb.plantcell.ensembl;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.data.StringValue;

/**
 * <code>NodeDialog</code> for the "EnsembleAddHomologue" Node.
 * Adds homologues for the input data to the output table
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AddHomologueNodeDialog extends DefaultNodeSettingsPane {
	private static String[] species = null;
	
    /**
     * New pane for configuring the EnsembleAddHomologue node.
     */
    protected AddHomologueNodeDialog() {
    	if (species == null)
    		species = AddHomologueNodeModel.getGenomeSpecies();
    	
    	addDialogComponent(new DialogComponentStringSelection(
    			new SettingsModelString(AddHomologueNodeModel.CFGKEY_SPECIES, species[0]), 
    			"Species defining IDs", species));
    	
    	addDialogComponent(new DialogComponentColumnNameSelection(
    			new SettingsModelColumnName(AddHomologueNodeModel.CFGKEY_SEQUENCE_ID, ""), 
    			"Sequence IDs from... ", 0, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return (colSpec.getType().isCompatible(StringValue.class));
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable columns for sequence ID selection";
					}
    				
    			}));
    }
}

