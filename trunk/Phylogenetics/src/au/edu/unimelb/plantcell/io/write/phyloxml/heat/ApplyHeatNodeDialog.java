package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * Creates a phyloxml document from the input data, decorated with data from the input table.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ApplyHeatNodeDialog extends DefaultNodeSettingsPane {
	private final SettingsModelString numericHeatColumn = new SettingsModelString(ApplyHeatNodeModel.CFGKEY_HEAT, "");
	private final SettingsModelString methodColumn = new SettingsModelString(ApplyHeatNodeModel.CFGKEY_METHOD, ApplyHeatNodeModel.DEFAULT_METHODS[0]);
	private final SettingsModelString propagation_fn = new SettingsModelString(ApplyHeatNodeModel.CFGKEY_PROPAGATION_FN, ApplyHeatNodeModel.HEAT_STRATEGY[0]);
	
	@SuppressWarnings("unchecked")
	protected ApplyHeatNodeDialog() {
        super();
        
        methodColumn.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				resetEnabledWidgets();
			}
        	
        });
        
        createNewGroup("What tree do you want to process?");
        addDialogComponent(new DialogComponentFileChooser(
        					new SettingsModelString(ApplyHeatNodeModel.CFGKEY_IN_TREE, ""), "Input tree file (eg. Newick/PhyloXML)"
        		));
    
        createNewGroup("Where should the heatmapped tree be saved? (always PhyloXML)");
        addDialogComponent(new DialogComponentFileChooser(
				new SettingsModelString(ApplyHeatNodeModel.CFGKEY_OUT_TREE, ""), "Output phyloxml file"
        ));
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean(ApplyHeatNodeModel.CFGKEY_OVERWRITE, false), "Overwrite output file?"
        		));
        		
        createNewTab("Heat Settings");
        String[] meth = ApplyHeatNodeModel.DEFAULT_METHODS;
        
        addDialogComponent(new DialogComponentButtonGroup(
        		methodColumn, 
        		"Get colours from?" , true, meth , meth));
        addDialogComponent(
        		new DialogComponentColumnNameSelection(
        				new SettingsModelString(ApplyHeatNodeModel.CFGKEY_A, ""), 
        				"Taxa (leaf node) name", 0, true, false, StringValue.class)
        		);
        addDialogComponent(
        		new DialogComponentColumnNameSelection(
        				numericHeatColumn, 
        				"Heat (scaled to maximum of column)", 0, true, false, DoubleValue.class)
        		);
        
        createNewGroup("Heat Propagation");
        addDialogComponent(
        		new DialogComponentStringSelection(propagation_fn, "", ApplyHeatNodeModel.HEAT_STRATEGY));
        
        createNewGroup("Branch width");
        addDialogComponent(
        		new DialogComponentStringSelection(new SettingsModelString(ApplyHeatNodeModel.CFGKEY_WIDTH_FN, 
        				ApplyHeatNodeModel.WIDTH_STRATEGY[0]), "", ApplyHeatNodeModel.WIDTH_STRATEGY));
    }

	public void resetEnabledWidgets() {
		boolean yes_or_no = methodColumn.getStringValue().equals(ApplyHeatNodeModel.DEFAULT_METHODS[0]);
		numericHeatColumn.setEnabled(yes_or_no);
		propagation_fn.setEnabled(yes_or_no);
	}
	
	@Override
	public void onOpen() {
		resetEnabledWidgets();
	}
}

