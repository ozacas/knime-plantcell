package au.edu.unimelb.plantcell.gp;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public class SOMClusteringNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	public SOMClusteringNodeDialog() {
		addDialogComponent(new DialogComponentString(
    			new SettingsModelString(SOMClusteringNodeModel.CFGKEY_GP_URL, "http://genepattern.broadinstitute.org/gp"), "URL of GenePattern server"));
    	
    	addDialogComponent(new DialogComponentString(
    			new SettingsModelString(SOMClusteringNodeModel.CFGKEY_GP_USER, ""), "Username (may be blank)"));
    	
    	addDialogComponent(new DialogComponentPasswordField(
    			new SettingsModelString(SOMClusteringNodeModel.CFGKEY_GP_PASSWD,""), "Password (if any)"));
	
    	addDialogComponent(new DialogComponentColumnNameSelection(
    			new SettingsModelColumnName(SOMClusteringNodeModel.CFGKEY_GENE_DESCR,""),
    			"Column to use for description of each row (gene)", 0, false, true, StringValue.class));
	
    	addDialogComponent(new DialogComponentButtonGroup(
    			new SettingsModelString(SOMClusteringNodeModel.CFGKEY_CLUSTER_BY, SOMClusteringNodeModel.CLUSTER_BY[0]), 
    			false, "Cluster by... ", SOMClusteringNodeModel.CLUSTER_BY));
    	
    	addDialogComponent(new DialogComponentString(
    			new SettingsModelString(SOMClusteringNodeModel.CFGKEY_CLUSTER_RANGE, "2-3"), "Cluster range"));
    	
    	
    	addDialogComponent(new DialogComponentNumber(
    			new SettingsModelIntegerBounded(SOMClusteringNodeModel.CFGKEY_ITERATIONS, 50000, 1000, 1000 * 1000),
    			"Iterations (eg. 500000 slow, 5000 fast and inaccurate)", 5000
    			));
    	
    	addDialogComponent(new DialogComponentNumber(
    			new SettingsModelIntegerBounded(SOMClusteringNodeModel.CFGKEY_SEED, 42, 1, 100 * 1000),
    			"Seed", 10
    			));
    	
    	addDialogComponent(new DialogComponentButtonGroup(
    			new SettingsModelString(SOMClusteringNodeModel.CFGKEY_SOM_INIT, SOMClusteringNodeModel.SOM_INIT[0]), 
    			false, "Initialization method", SOMClusteringNodeModel.SOM_INIT));
    	
    	addDialogComponent(new DialogComponentButtonGroup(
    			new SettingsModelString(SOMClusteringNodeModel.CFGKEY_SOM_NHOOD, SOMClusteringNodeModel.SOM_NHOOD[0]), 
    			false, "Neighbourhood method", SOMClusteringNodeModel.SOM_NHOOD));
    	
	}

}
