package au.edu.unimelb.plantcell.cluster.management;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jclouds.providers.ProviderMetadata;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "ListNodes" Node.
 * Lists the nodes available via the chosen provider
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ListNodesNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring ListNodes node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected ListNodesNodeDialog() {
        super();
        
        final SettingsModelString sms = new SettingsModelString(ListNodesNodeModel.CFGKEY_PROVIDER, "");
        final SettingsModelString sms_endpoint = new SettingsModelString(ListNodesNodeModel.CFGKEY_ENDPOINT, "");
        addDialogComponent(new DialogComponentStringSelection(
        			sms, "Cloud Provider", ListNodesNodeModel.getCloudList()
        ));           
        addDialogComponent(new DialogComponentString(
        		sms_endpoint,
        		"Endpoint (advanced users only)", true, 60
        		));
        sms.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Object o = ListNodesNodeModel.getMetadata(sms.getStringValue());
				if (o != null && o instanceof ProviderMetadata) {
					sms_endpoint.setStringValue(((ProviderMetadata)o).getEndpoint());
				}
			}
        	
        });
 
        createNewGroup("Authentication");
        addDialogComponent(new DialogComponentString(
        			new SettingsModelString(ListNodesNodeModel.CFGKEY_IDENTITY, "username"), "Identity", true, 40
        		));
        addDialogComponent(new DialogComponentPasswordField(
    			new SettingsModelString(ListNodesNodeModel.CFGKEY_PASSWD, ""), "Password", 20
    		));
        
        init();
    }
    
    /**
     * hook to provide additional widgets for the dialog (subclasses override)
     */
    protected void init() {
    	
    }
}

