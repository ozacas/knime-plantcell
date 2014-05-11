package au.edu.unimelb.plantcell.io.pruner;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelOptionalString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;


/**
 * Creates a phyloxml document from the input data, decorated with data from the input table.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class TreePruneNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	protected TreePruneNodeDialog() {
        super();
        
        createNewGroup("What tree do you want to prune?");
        addDialogComponent(new DialogComponentFileChooser(
        					new SettingsModelString(TreePruneNodeModel.CFGKEY_INFILE, ""), "Input tree file (eg. Newick/PhyloXML)"
        		));
        
        createNewGroup("Save pruned tree (if not empty)? (always PhyloXML)");
        addDialogComponent(new DialogComponentFileChooser(
				new SettingsModelOptionalString(TreePruneNodeModel.CFGKEY_OUTFILE, "", true), "Output file"
        ));
        addDialogComponent(
    			new DialogComponentBoolean(
    					new SettingsModelBoolean(TreePruneNodeModel.CFGKEY_OVERWRITE, Boolean.FALSE), "Overwrite existing?"));
    
        
        createNewGroup("Taxa");
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(TreePruneNodeModel.CFGKEY_TAXA, ""), "Sequences from... ",
        		0, false, true, SequenceValue.class
        		));
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(TreePruneNodeModel.CFGKEY_SPECIES, ""), "Scientific species names from... ", 
        		0, false, true, StringValue.class
        		));
        addDialogComponent(new DialogComponentString(
        		new SettingsModelString(TreePruneNodeModel.CFGKEY_TAXA_REGEXP, "(.*)"), "Taxa regexp (advanced users: first capturing group)", true, 20));
        
        createNewTab("Tree Pruning");
        createNewGroup("Select a method...");
        addDialogComponent(new DialogComponentButtonGroup(
        		new SettingsModelString(TreePruneNodeModel.CFGKEY_STRATEGY, TreePruneNodeModel.PRUNING_STRATEGYS[0]), 
        		"Strategy", true, TreePruneNodeModel.PRUNING_STRATEGYS, TreePruneNodeModel.PRUNING_STRATEGYS
        		));
       
    }
}

