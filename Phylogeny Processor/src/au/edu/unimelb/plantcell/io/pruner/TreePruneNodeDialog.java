package au.edu.unimelb.plantcell.io.pruner;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
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
        
     
        addDialogComponent(
        			new DialogComponentBoolean(
        					new SettingsModelBoolean(TreePruneNodeModel.CFGKEY_OVERWRITE, Boolean.FALSE), "Overwrite existing output file?"));
        
        createNewGroup("What tree do you want to prune?");
        addDialogComponent(new DialogComponentFileChooser(
        					new SettingsModelString(TreePruneNodeModel.CFGKEY_INFILE, ""), "Input tree file (eg. Newick/PhyloXML)"
        		));
        
        createNewGroup("Save pruned tree? (always PhyloXML)");
        addDialogComponent(new DialogComponentFileChooser(
				new SettingsModelString(TreePruneNodeModel.CFGKEY_OUTFILE, ""), "Output phyloxml file"
        ));
        
        createNewGroup("Tree labels should be matched against ... column?");
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(TreePruneNodeModel.CFGKEY_TAXA, ""), "Taxa names (ie. sequences) from... ",
        		0, true, true, SequenceValue.class
        		));
       
        createNewGroup("Match taxa names using... (advanced users only)");
        addDialogComponent(new DialogComponentString(
        		new SettingsModelString(TreePruneNodeModel.CFGKEY_TAXA_REGEXP, "(.*)"), "Taxa regexp (first group used to match)", true, 20));
        
        createNewTab("Match taxa species to ... column?");
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(TreePruneNodeModel.CFGKEY_SPECIES, ""), "Scientific species names from... ", 
        		0, false, true, StringValue.class
        		));
        
        createNewGroup("How to prune nodes from the tree?");
        addDialogComponent(new DialogComponentButtonGroup(
        		new SettingsModelString(TreePruneNodeModel.CFGKEY_STRATEGY, TreePruneNodeModel.PRUNING_STRATEGYS[0]), 
        		"Strategy", true, TreePruneNodeModel.PRUNING_STRATEGYS, TreePruneNodeModel.PRUNING_STRATEGYS
        		));
       
    }
}

