package au.edu.unimelb.plantcell.io.seg;

import javax.swing.JFileChooser;

import org.knime.core.data.DataColumnSpec;
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
 * <code>NodeDialog</code> for the "LocalSEG" Node.
 * Supports running a local SEG executable and integration into the KNIME-PlantCell platform
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class LocalSEGNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the LocalMuscle node.
     */
    protected LocalSEGNodeDialog() {
    	createNewGroup("Select the executable SEG program on your computer...");
    	addDialogComponent(new DialogComponentFileChooser(new SettingsModelString(LocalSEGNodeModel.CFGKEY_EXE, ""), 
    			"SEG Executable", JFileChooser.OPEN_DIALOG, false, ""
    			));
    	closeCurrentGroup();
    	
    	addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(LocalSEGNodeModel.CFGKEY_SEQUENCES, ""), 
    			"Sequences", 0, true, new ColumnFilter() {

					@Override
					public String allFilteredMsg() {
						return "No suitable sequences available!";
					}

					@Override
					public boolean includeColumn(DataColumnSpec arg0) {
						if (arg0.getType().isCompatible(SequenceValue.class))
							return true;
						return false;
					}
    		
    	}));
    	
    	createNewGroup("Advanced users only");
    	
    	final SettingsModelString udef= new SettingsModelString(LocalSEGNodeModel.CFGKEY_USER_DEFINED, "");
    	addDialogComponent(new DialogComponentString(udef, "Custom command line parameters", false, 80
    			));
    	
    	addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(LocalSEGNodeModel.CFGKEY_LOG_STDERR, Boolean.FALSE), "Report messages to console?"));
    }
}

