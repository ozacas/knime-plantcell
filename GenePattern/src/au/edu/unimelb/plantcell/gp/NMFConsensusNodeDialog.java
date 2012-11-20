package au.edu.unimelb.plantcell.gp;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public class NMFConsensusNodeDialog extends DefaultNodeSettingsPane {

	public NMFConsensusNodeDialog() {
		addDialogComponent(new DialogComponentString(
    			new SettingsModelString(SOMClusteringNodeModel.CFGKEY_GP_URL, "http://genepattern.broadinstitute.org/gp"), "URL of GenePattern server"));
    	
    	addDialogComponent(new DialogComponentString(
    			new SettingsModelString(NMFConsensusNodeModel.CFGKEY_GP_USER, ""), "Username (may be blank)"));
    	
    	addDialogComponent(new DialogComponentPasswordField(
    			new SettingsModelString(NMFConsensusNodeModel.CFGKEY_GP_PASSWD,""), "Password (if any)"));
	    
    	addDialogComponent(new DialogComponentNumber(
    			new SettingsModelIntegerBounded(NMFConsensusNodeModel.CFGKEY_K_INIT, 2, 2, 50), "Initial number of clusters (k)", 1));
    	
    	addDialogComponent(new DialogComponentNumber(
    			new SettingsModelIntegerBounded(NMFConsensusNodeModel.CFGKEY_K_FINAL, 5, 2, 50), "Final number of clusters", 1));
    	
    	addDialogComponent(new DialogComponentNumber(
    			new SettingsModelIntegerBounded(NMFConsensusNodeModel.CFGKEY_MAX_ITERATIONS, 2000, 100, 100*1000), "Maximum iterations", 1));
    	
    	addDialogComponent(new DialogComponentNumber(
    			new SettingsModelIntegerBounded(NMFConsensusNodeModel.CFGKEY_CLUSTERINGS, 20, 5, 100), "Number of clusterings", 1));
    	
    	addDialogComponent(new DialogComponentString(
    			new SettingsModelString(NMFConsensusNodeModel.CFGKEY_SEED, "123456789"), "RNG Seed"));
    	
    	addDialogComponent(new DialogComponentButtonGroup(
    			new SettingsModelString(NMFConsensusNodeModel.CFGKEY_ERROR_FN, NMFConsensusNodeModel.ERROR_FUNCTIONS[0]), 
    			false, "Error function", NMFConsensusNodeModel.ERROR_FUNCTIONS));
    	
    	addDialogComponent(new DialogComponentString(
    			new SettingsModelString(NMFConsensusNodeModel.CFGKEY_STOP_CONV, "40"), "Stop convergence"));
    	
    	addDialogComponent(new DialogComponentString(
    			new SettingsModelString(NMFConsensusNodeModel.CFGKEY_STOP_FREQ, "10"), "Stop frequency"));
    }

}
