package au.edu.unimelb.plantcell.io.usearch;

import javax.swing.JFileChooser;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * <code>NodeDialog</code> for the sequence clustering Node.
 * Supports running a local usearch executable and integration into the KNIME-PlantCell platform
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class SeqClusterNodeDialog extends DefaultNodeSettingsPane {

    protected SeqClusterNodeDialog() {
    	createNewGroup("Where is the uSearch executable program?");
    	addDialogComponent(new DialogComponentFileChooser(new SettingsModelString(SeqClusterNodeModel.CFGKEY_EXE, ""), 
    			"uSearch executable", JFileChooser.OPEN_DIALOG, false, ""
    			));
    	this.closeCurrentGroup();
    	
    	createNewGroup("What sequences to cluster?");
    	addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(SeqClusterNodeModel.CFGKEY_SEQUENCES, ""), 
    			"Sequences", 0, true, new ColumnFilter() {

					@Override
					public String allFilteredMsg() {
						return "No sequence column in input table!";
					}

					@Override
					public boolean includeColumn(DataColumnSpec arg0) {
						if (arg0.getType().isCompatible(SequenceValue.class))
							return true;
						return false;
					}
    		
    	}));
    	
    	addDialogComponent(new DialogComponentNumber(
    			new SettingsModelDoubleBounded(SeqClusterNodeModel.CFGKEY_THRESHOLD, 95.0, 0.0, 100.0), 
    			"Identity Threshold (%)", 5.0
    			));
    	
    	createNewGroup("Advanced users only");
    	addDialogComponent(new DialogComponentButtonGroup(
    			new SettingsModelString(SeqClusterNodeModel.CFGKEY_ALGO, SeqClusterNodeModel.ALGO[0]), false, "Algorithm", SeqClusterNodeModel.ALGO
    			));
    	final SettingsModelString udef= new SettingsModelString(SeqClusterNodeModel.CFGKEY_USER_DEFINED, "");
    	addDialogComponent(new DialogComponentString(udef, "Custom arguments", false, 80
    			));
    	
    	addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(SeqClusterNodeModel.CFGKEY_LOG_STDERR, Boolean.FALSE), "Report uSearch messages to KNIME console?"));
    }
}

