package au.edu.unimelb.plantcell.networks;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

/**
 * <code>NodeDialog</code> for the "Creator" Node.
 * Using JUNG, this node creates a network cell for the input data correlated above a chosen threshold for each target variable.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class CreatorNodeDialog extends DefaultNodeSettingsPane {

    @SuppressWarnings("unchecked")
	protected CreatorNodeDialog() {
        super();
        
        createNewGroup("Use input table row colours?");
        String[] items = new String[] { "Nodes coloured by row", "Edges coloured by row", "None" };
        addDialogComponent(new DialogComponentButtonGroup(
        		new SettingsModelString(CreatorNodeModel.CFGKEY_COLOUR_BY, "None"), "Use row colours to...", true, items, items
        		));
        
      
        createNewGroup("Drawing options?");
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(CreatorNodeModel.CFGKEY_EDGE_DISTANCE, Boolean.TRUE), 
        		"Show edge distance?"));
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(CreatorNodeModel.CFGKEY_EDGE_GRADIENT, Boolean.FALSE),
        		"Paint edges as gradient (looks nicer)?"));
        
        createNewTab("Source nodes");
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(CreatorNodeModel.CFGKEY_SOURCE, ""), "Node ID from...", 0, StringValue.class));
        
        createNewGroup("Display numeric vector (eg. timepoints) as node?");
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(CreatorNodeModel.CFGKEY_TIMECOURSE, ""), "Numeric vector columns (eg. list)", 0, false, true, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						if (colSpec.getType().isCollectionType() && 
								colSpec.getType().getCollectionElementType().isCompatible(DoubleValue.class))
							return true;
						return false;
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable numeric vector columns available eg. group-by list";
					}
        			
        		}));
        createNewGroup("Annotate with ...");
        addDialogComponent(new DialogComponentColumnFilter(new SettingsModelFilterString(CreatorNodeModel.CFGKEY_ANNOTATE_VERTEX), 0, false));
        
        
        createNewTab("Destination Nodes");
       
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(CreatorNodeModel.CFGKEY_DESTINATION, ""), "Node ID from...", 0, StringValue.class));
       
        createNewGroup("Annotate with...");
        addDialogComponent(new DialogComponentColumnFilter(new SettingsModelFilterString(CreatorNodeModel.CFGKEY_ANNOTATE_VERTEX_DEST), 0, false));

        createNewTab("Edges");
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(CreatorNodeModel.CFGKEY_DISTANCE, "<None>"), 
        		"Distance between source and destination (optional)", 0, false, true, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return colSpec.getType().isCompatible(DoubleValue.class);
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable distance column (numeric column) available!";
					}
        			
        		}));
      
        createNewGroup("Annotate with...");
        addDialogComponent(new DialogComponentColumnFilter(new SettingsModelFilterString(CreatorNodeModel.CFGKEY_ANNOTATE_EDGE), 0, false));
    }
}

