package au.edu.unimelb.plantcell.io.ws.mascot.config;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.xml.ws.Service;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "DatFileDownload" Node.
 * Permits downloading of Mascot DAT files via a JAX-WS web service and will load each dat file into the output table as per the mascot reader. The node also saves the DAT files to the user-specified folder.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ShowConfigNodeDialog extends DefaultNodeSettingsPane {
	private final SettingsModelString url = new SettingsModelString(ShowConfigNodeModel.CFGKEY_MASCOT_SERVICE_URL, ShowConfigNodeModel.DEFAULT_MASCOTEE_SERVICE_URL);

    /**
     * New pane for configuring the DatFileDownload node.
     */
    protected ShowConfigNodeDialog() {
    	final SettingsModelString user = new SettingsModelString(ShowConfigNodeModel.CFGKEY_USERNAME, "");
    	final SettingsModelString passwd = new SettingsModelString(ShowConfigNodeModel.CFGKEY_PASSWORD, "");
    
   
    	createNewGroup("MascotEE Service");
    	
    	addDialogComponent(new DialogComponentString(url, "MascotEE URL"));
    	DialogComponentButton checknow_button = new DialogComponentButton("Check Now...");
    	checknow_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					Service s = ShowConfigNodeModel.getConfigService(url.getStringValue());
					if (s == null)
						throw new InvalidSettingsException("No such service!");
				} catch (Exception e) {
					JOptionPane.showMessageDialog(null, new JLabel(e.getMessage()), e.getMessage(), 0);
				}
			}
    		
    	});
    	
    	this.setHorizontalPlacement(true);
    	addDialogComponent(new DialogComponentString(user, "Username"));
    	addDialogComponent(new DialogComponentPasswordField(passwd, "Password"));
    	this.setHorizontalPlacement(false);
    }
    
    protected String getCurrentMascotEEUrl() {
    	return url.getStringValue();
    }
}

