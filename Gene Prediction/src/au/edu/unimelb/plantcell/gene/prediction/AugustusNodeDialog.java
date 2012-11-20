package au.edu.unimelb.plantcell.gene.prediction;

import java.util.ArrayList;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * <code>NodeDialog</code> for the "AugustusNodeModel" Node.
 * Runs augustus (http://augustus.gobics.de) on the local computer and loads its predictions into a KNIME table.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AugustusNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the AugustusNodeModel node.
     */
    protected AugustusNodeDialog() {
    	super();
    	
    	addDialogComponent(new DialogComponentColumnNameSelection(
    			new SettingsModelString(AugustusNodeModel.CFGKEY_SEQUENCE, "c:/temp/crap.fasta"), 
    			"Sequences from... ", 0, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return colSpec.getType().isCompatible(SequenceValue.class);
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable sequence columns available!";
					}
    				
    			}));
    	
    	String[] strands = new String[] { "both", "forward", "backward" };
    	addDialogComponent(new DialogComponentButtonGroup(new SettingsModelString(AugustusNodeModel.CFGKEY_STRAND, "both"), "Predict on which strands?", false, strands, strands));
    	
    	String[] species = AugustusNodeModel.getGeneModels();
    	addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(AugustusNodeModel.CFGKEY_GENEMODEL, "arabidopsis"), 
    			"Gene Model (species)", species));
    	
    	addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(AugustusNodeModel.CFGKEY_SINGLESTRAND, Boolean.TRUE),
    			"Predict strands independently? (permits overlapping genes)"
    			));
    	
    	addDialogComponent(new DialogComponentString(new SettingsModelString(AugustusNodeModel.CFGKEY_OTHER, ""), "Other arguments (advanced)"));
   
    	
    	String[] features_of_interest = new String[] { "All", "Gene", "Transcript", "Intron", "CDS" };
    	ArrayList<String> list = new ArrayList<String>();
    	for (String foi : features_of_interest) {
    		list.add(foi);
    	}
    	
    	createNewGroup("What predicted features to annotate?");
    	addDialogComponent(new DialogComponentStringListSelection(
    			new SettingsModelStringArray(AugustusNodeModel.CFGKEY_WANTED, new String[] { "All" }), "Features to report (if selected)", list, true, 5));
    }
}

