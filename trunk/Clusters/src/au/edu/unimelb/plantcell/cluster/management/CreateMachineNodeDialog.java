package au.edu.unimelb.plantcell.cluster.management;

import java.util.ArrayList;
import java.util.Collection;

import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;


/**
 * <code>NodeDialog</code> for the "CreateMachine" Node.
 * Node which can create several machines and then run a chef wizard to initialise the nodes with a particular config.
 *
 * Inherits common stuff from the list node to improve code reuse.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class CreateMachineNodeDialog extends ListNodesNodeDialog {

    protected CreateMachineNodeDialog() {
    	super();
    	
    	createNewGroup("Machine configuration");
    	addDialogComponent(new DialogComponentNumber(
    			new SettingsModelIntegerBounded(CreateMachineNodeModel.CFGKEY_NUM_MACHINES, 4, 1, 100),
    			 "Number of nodes to create", 4));
    	
    	addDialogComponent(new DialogComponentStringListSelection(
    			new SettingsModelStringArray(CreateMachineNodeModel.CFGKEY_CONSTRAINTS, new String[] {}), 
    			"Minimum requirements for every machine", getMinimumRequirements(), true, 10
    			));
    	
    	addDialogComponent(
    			new DialogComponentString(
    					new SettingsModelString(CreateMachineNodeModel.CFGKEY_GROUP_NAME, "MyCluster"), 
    					"Machine group name (recommended)"));
    	
    	addDialogComponent(
    			new DialogComponentString(
    					new SettingsModelString(CreateMachineNodeModel.CFGKEY_LOCATION, ""),
    					"Machine Location (optional)"));
    }
    
    
    private Collection<String> getMinimumRequirements() {
		ArrayList<String> ret = new ArrayList<String>();
		
		ret.add("Architecture: 64-bit");
		ret.add("Minimum RAM: 1GB");
		ret.add("Minimum RAM: 4GB");
		ret.add("Minimum CPU Cores: 2");
		ret.add("Minimum CPU Cores: 4");
		ret.add("Minimum Disk: 10GB");
		ret.add("Minimum Disk: 30GB");
		ret.add("Image: Ubuntu (latest stable)");
		ret.add("Image: RedHat (latest stable)");
		return ret;
	}


	/**
     * {@InheritDoc}
     */ 
    @Override
    protected void init() {
    	
    }
}

