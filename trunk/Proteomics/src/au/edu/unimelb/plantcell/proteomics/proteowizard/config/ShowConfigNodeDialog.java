package au.edu.unimelb.plantcell.proteomics.proteowizard.config;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ShowConfigNodeDialog extends DefaultNodeSettingsPane {

    protected ShowConfigNodeDialog() {
    	   super();
          
    	   addDialogComponent(new DialogComponentString(
    			   new SettingsModelString(ShowConfigNodeModel.CFGKEY_ENDPOINT, ""), "MSConvertEE URL", true, 60
    	   ));
    	   this.setHorizontalPlacement(true);
    	   addDialogComponent(new DialogComponentString(
    			   new SettingsModelString(ShowConfigNodeModel.CFGKEY_USERNAME, ""), "Username", false, 20));
    	   addDialogComponent(new DialogComponentString(
    			   new SettingsModelString(ShowConfigNodeModel.CFGKEY_PASSWD, ""), "Password", false, 20));
    	   this.setHorizontalPlacement(false);
    }
    
}

