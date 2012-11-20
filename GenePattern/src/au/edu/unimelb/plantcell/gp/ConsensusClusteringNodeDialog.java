package au.edu.unimelb.plantcell.gp;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public class ConsensusClusteringNodeDialog extends DefaultNodeSettingsPane {

	public ConsensusClusteringNodeDialog() {
		addDialogComponent(new DialogComponentString(
    			new SettingsModelString(ConsensusClusteringNodeModel.CFGKEY_GP_URL, "http://genepattern.broadinstitute.org/gp"), "URL of GenePattern server"));
    	
    	addDialogComponent(new DialogComponentString(
    			new SettingsModelString(ConsensusClusteringNodeModel.CFGKEY_GP_USER, ""), "Username (may be blank)"));
    	
    	addDialogComponent(new DialogComponentPasswordField(
    			new SettingsModelString(ConsensusClusteringNodeModel.CFGKEY_GP_PASSWD,""), "Password (if any)"));
	
    	addDialogComponent(new DialogComponentButtonGroup(
    			new SettingsModelString(ConsensusClusteringNodeModel.CFGKEY_CLUSTERBY, ConsensusClusteringNodeModel.CLUSTERBY[0]), 
    			false, "Cluster by... ", ConsensusClusteringNodeModel.CLUSTERBY));
    	
    	addDialogComponent(new DialogComponentString(
    			new SettingsModelString(ConsensusClusteringNodeModel.CFGKEY_SEED, "12345"),
    			"Seed"
    			));
    	
    	addDialogComponent(new DialogComponentButtonGroup(
    			new SettingsModelString(ConsensusClusteringNodeModel.CFGKEY_MERGE, ConsensusClusteringNodeModel.MERGE[0]), 
    			false, "Merge method", ConsensusClusteringNodeModel.MERGE));
    	
    	addDialogComponent(new DialogComponentButtonGroup(
    			new SettingsModelString(ConsensusClusteringNodeModel.CFGKEY_RESAMPLE, ConsensusClusteringNodeModel.RESAMPLE_BY[0]), 
    			false, "Resample method", ConsensusClusteringNodeModel.RESAMPLE_BY));
    	
    	addDialogComponent(new DialogComponentButtonGroup(
    			new SettingsModelString(ConsensusClusteringNodeModel.CFGKEY_DISTANCE, ConsensusClusteringNodeModel.DISTANCE_MEASURES[0]), 
    			false, "Distance measure", ConsensusClusteringNodeModel.DISTANCE_MEASURES));
    	
    	addDialogComponent(new DialogComponentButtonGroup(
    			new SettingsModelString(ConsensusClusteringNodeModel.CFGKEY_ALGORITHM, ConsensusClusteringNodeModel.ALGORITHMS[0]), 
    			false, "Clustering method", ConsensusClusteringNodeModel.ALGORITHMS));
    	
    	addDialogComponent(new DialogComponentNumber(
    			new SettingsModelIntegerBounded(ConsensusClusteringNodeModel.CFGKEY_KMAX, 5, 2, 20),
    			"KMax (maximum number of clusters)", 1));    	
    	
    	addDialogComponent(new DialogComponentButtonGroup(
    			new SettingsModelString(ConsensusClusteringNodeModel.CFGKEY_NORMALISATION, ConsensusClusteringNodeModel.NORMALISATIONS[0]), 
    			false, "Normalise...", ConsensusClusteringNodeModel.NORMALISATIONS));
	}

}
