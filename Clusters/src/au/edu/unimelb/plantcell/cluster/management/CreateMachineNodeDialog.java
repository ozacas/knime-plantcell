package au.edu.unimelb.plantcell.cluster.management;

import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;


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
    			new SettingsModelIntegerBounded(CreateMachineNodeModel.CFGKEY_NUM_MACHINES, 1, 100, 1),
    			 "Number of nodes to create", 5));
    	
    	
    	
    }
    
    
    /**
     * {@InheritDoc}
     */ 
    @Override
    protected void init() {
    	
    }
}

