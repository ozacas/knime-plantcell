package au.edu.unimelb.plantcell.cluster.management;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * <code>NodeDialog</code> for the "MakeCluster" Node.
 * Support for jClouds API (and maybe apache whirr too) from within KNIME for analysis requiring a cluster. Intention is to support things like Hadoop, BLAST+ etc. on the cloud
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MakeClusterNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring MakeCluster node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected MakeClusterNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                    MakeClusterNodeModel.CFGKEY_COUNT,
                    MakeClusterNodeModel.DEFAULT_COUNT,
                    Integer.MIN_VALUE, Integer.MAX_VALUE),
                    "Counter:", /*step*/ 1, /*componentwidth*/ 5));
                    
    }
}

