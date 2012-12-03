package au.edu.unimelb.plantcell.networks;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
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
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(CreatorNodeModel.CFGKEY_SOURCE, ""), "Source node", 0, StringValue.class));
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(CreatorNodeModel.CFGKEY_DESTINATION, ""), "Destination node", 0, StringValue.class));
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelColumnName(CreatorNodeModel.CFGKEY_DISTANCE, "<None>"), 
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
        
        createNewGroup("Annotate each source node with ... columns?");
        addDialogComponent(new DialogComponentColumnFilter(new SettingsModelFilterString(CreatorNodeModel.CFGKEY_ANNOTATE_VERTEX), 0, false));
        
        createNewGroup("Annotate each edge with ... columns?");
        addDialogComponent(new DialogComponentColumnFilter(new SettingsModelFilterString(CreatorNodeModel.CFGKEY_ANNOTATE_EDGE), 0, false));
    }
}

