package au.edu.unimelb.plantcell.gp;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "GenePattern" Node.
 * Nodes to support remote invocation of a GenePattern instance for key analyses (heatmap, clustering etc.)
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class HeatmapNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the GenePattern node.
     */
    @SuppressWarnings("unchecked")
	protected HeatmapNodeDialog() {
    	addDialogComponent(new DialogComponentString(
    			new SettingsModelString(HeatmapNodeModel.CFGKEY_GP_URL, "http://genepattern.broadinstitute.org/gp"), "URL of GenePattern web site"));
    	
    	addDialogComponent(new DialogComponentString(
    			new SettingsModelString(HeatmapNodeModel.CFGKEY_GP_USER, ""), "Username (may be blank)"));
    	
    	addDialogComponent(new DialogComponentPasswordField(
    			new SettingsModelString(HeatmapNodeModel.CFGKEY_GP_PASSWD,""), "Password (if any)"));
    	
    	addDialogComponent(new DialogComponentColumnNameSelection(
    			new SettingsModelColumnName(HeatmapNodeModel.CFGKEY_DESCR_COL,""),
    			"Column to use for description of each row (gene)", 0, false, true, StringValue.class));
    }
}

