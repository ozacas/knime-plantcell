package au.edu.unimelb.plantcell.phylogenetics.rename;

import javax.swing.JFileChooser;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "PhylogenyProcessor" Node.
 * Using the PAL library, as exported from MUSCLE node, this tree takes input data and performs tree construction, bootstrapping and other phylogenetic analyses as configured by the user.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class RenameNodeDialog extends DefaultNodeSettingsPane {
	
    /**
     * New pane for configuring PhylogenyProcessor node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    @SuppressWarnings("unchecked")
	protected RenameNodeDialog() {
        super();

        createNewGroup("Input tree file (PhyloXML, Newick, NEXUS)");
        addDialogComponent(new DialogComponentFileChooser(
        		new SettingsModelString(RenameNodeModel.CFGKEY_INPUT_FILE, ""), "phyloxml-tree", JFileChooser.OPEN_DIALOG, 
        		false, ".phyloxml|.xml|.nexus|.newick|.nw|.nhx"
        		));
        
        createNewGroup("Output tree file (always PhyloXML)");
        addDialogComponent(new DialogComponentFileChooser(
        		new SettingsModelString(RenameNodeModel.CFGKEY_OUTPUT_FILE, ""), "saved-phyloxml-tree", JFileChooser.SAVE_DIALOG, 
        		false, ".phyloxml"
        		));
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean(RenameNodeModel.CFGKEY_OVERWRITE, Boolean.FALSE), "overwrite existing files?"
        		));
        
        createNewGroup("Rename nodes where old and replace with new where...");
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(RenameNodeModel.CFGKEY_OLD_NAME, ""), "Old node name from... ", 0, false, true, StringValue.class
        		));
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(RenameNodeModel.CFGKEY_NEW_NAME, ""), "New node name from... ", 0, false, true, StringValue.class
        		));
    }
    
    
}

