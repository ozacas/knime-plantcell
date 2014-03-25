package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;


/**
 * Creates a phyloxml document from the input data, decorated with data from the input table.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ApplyHeatNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	protected ApplyHeatNodeDialog() {
        super();
        
        createNewGroup("What tree do you want to decorate?");
        addDialogComponent(new DialogComponentFileChooser(
        					new SettingsModelString(ApplyHeatNodeModel.CFGKEY_IN_TREE, ""), "Input tree file (eg. Newick/PhyloXML)"
        		));
    
        createNewGroup("Where should the decorated tree be saved? (always PhyloXML)");
        addDialogComponent(new DialogComponentFileChooser(
				new SettingsModelString(ApplyHeatNodeModel.CFGKEY_OUT_TREE, ""), "Output phyloxml file"
        ));
        
        createNewGroup("Heat data columns (from input, pairwise)");
        addDialogComponent(
        		new DialogComponentColumnNameSelection(
        				new SettingsModelString(ApplyHeatNodeModel.CFGKEY_A, ""), 
        				"A", 0, true, false, StringValue.class)
        		);
        addDialogComponent(
        		new DialogComponentColumnNameSelection(
        				new SettingsModelString(ApplyHeatNodeModel.CFGKEY_B, ""), 
        				"B", 0, true, false, StringValue.class)
        		);
        addDialogComponent(
        		new DialogComponentColumnNameSelection(
        				new SettingsModelString(ApplyHeatNodeModel.CFGKEY_A, ""), 
        				"Heat (scaled to maximum of column)", 0, true, false, DoubleValue.class)
        		);
        
        createNewTab("Heat Settings");
        
    }
}

