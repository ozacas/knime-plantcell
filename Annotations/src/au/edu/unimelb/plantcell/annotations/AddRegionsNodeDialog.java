package au.edu.unimelb.plantcell.annotations;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Converts two columns (accession, sequence) into a sequence cell (dialog implementation)
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AddRegionsNodeDialog extends DefaultNodeSettingsPane {
	
    /**
     * New pane for configuring RegionAnalyzer node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
	protected AddRegionsNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(AddRegionsNodeModel.CFGKEY_SEQUENCE_COL, "Annotated Sequences"), 
        		"Annotated sequences from... ", 0, true, false, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return colSpec.getType().isCompatible(SequenceValue.class);
					}

					@Override
					public String allFilteredMsg() {
						return "No annotated sequence columns available to process!";
					}
        			
        		}
        ));
        
        addDialogComponent(new DialogComponentString(
        		new SettingsModelString(AddRegionsNodeModel.CFGKEY_TRACK_NAME, "BLAST:hits"), 
        		"Track name"
        ));
        
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(AddRegionsNodeModel.CFGKEY_REGION_LABEL_COL, "Label region using..."), 
        		"Annotated sequences from... ", 0, true, false, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return colSpec.getType().isCompatible(StringValue.class);
					}

					@Override
					public String allFilteredMsg() {
						return "No annotated sequence columns available to process!";
					}
        			
        		}
        ));
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(AddRegionsNodeModel.CFGKEY_REGION_START_COL, ""), 
        		"Start of region from...", 0, true, false, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return colSpec.getType().isCompatible(IntValue.class);
					}

					@Override
					public String allFilteredMsg() {
						return "No integer columns available to use to mark the start of a region!";
					}
        			
        		}
        ));
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(AddRegionsNodeModel.CFGKEY_REGION_END_COL, ""), 
        		"End of region from...", 0, true, false, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return colSpec.getType().isCompatible(IntValue.class);
					}

					@Override
					public String allFilteredMsg() {
						return "No integer columns available to use to mark the end of a region!";
					}
        			
        		}
        ));
        
        addDialogComponent(new DialogComponentButtonGroup(
        		new SettingsModelString(AddRegionsNodeModel.CFGKEY_MERGE, AddRegionsNodeModel.MERGE_SEMANTICS[0]), 
        		true, "New regions should... ", AddRegionsNodeModel.MERGE_SEMANTICS
        ));
        
    }
}

