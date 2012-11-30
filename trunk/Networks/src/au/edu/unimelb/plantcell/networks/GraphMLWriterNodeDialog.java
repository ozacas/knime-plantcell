package au.edu.unimelb.plantcell.networks;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.networks.cells.NetworkValue;

/**
 * <code>NodeDialog</code> for the "Creator" Node.
 * Using JUNG, this node creates a network cell for the input data correlated above a chosen threshold for each target variable.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class GraphMLWriterNodeDialog extends DefaultNodeSettingsPane {

    @SuppressWarnings("unchecked")
	protected GraphMLWriterNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(GraphMLWriterNodeModel.CFGKEY_NETWORKS, ""), "Source node", 0, NetworkValue.class));
        addDialogComponent(new DialogComponentFileChooser(
        		new SettingsModelString(GraphMLWriterNodeModel.CFGKEY_FILENAME, ""), "graphml-folder", JFileChooser.SAVE_DIALOG, true, ""));
    }
}

