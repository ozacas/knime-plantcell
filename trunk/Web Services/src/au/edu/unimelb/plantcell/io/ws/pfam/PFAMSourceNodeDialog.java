package au.edu.unimelb.plantcell.io.ws.pfam;


import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * <code>NodeDialog</code> for the PFAM Node.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class PFAMSourceNodeDialog extends DefaultNodeSettingsPane {
	
 
	@SuppressWarnings("unchecked")
	protected PFAMSourceNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentButtonGroup(new SettingsModelString(PFAMSourceNodeModel.CFGKEY_TYPE, ""), 
        		"I want to... ", true, PFAMSourceNodeModel.PFAM_Task_Labels, PFAMSourceNodeModel.PFAM_Task_Labels
        		));
        
        addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(PFAMSourceNodeModel.CFGKEY_COLUMN, ""),
        		"Input column", 0, StringValue.class));
        
        addDialogComponent(new DialogComponentString(
        		new SettingsModelString(PFAMSourceNodeModel.CFGKEY_URL, PFAMSourceNodeModel.DEFAULT_URL),
        		"Endpoint address (advanced)"));
    }
}



