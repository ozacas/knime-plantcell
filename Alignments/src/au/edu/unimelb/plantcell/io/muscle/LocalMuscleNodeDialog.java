package au.edu.unimelb.plantcell.io.muscle;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * <code>NodeDialog</code> for the "LocalMuscle" Node.
 * Supports running a local muscle executable and integration into the KNIME-PlantCell platform
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class LocalMuscleNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the LocalMuscle node.
     */
    protected LocalMuscleNodeDialog() {
    	addDialogComponent(new DialogComponentFileChooser(new SettingsModelString(LocalMuscleNodeModel.CFGKEY_EXE, ""), 
    			"MUSCLE Executable Program", 0, ".exe"
    			));
    	
    	addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(LocalMuscleNodeModel.CFGKEY_SEQUENCES, ""), 
    			"Collection column of sequences", 0, true, new ColumnFilter() {

					@Override
					public String allFilteredMsg() {
						return "No suitable list or set of sequences available!";
					}

					@Override
					public boolean includeColumn(DataColumnSpec arg0) {
						if (arg0.getType().isCollectionType() && arg0.getType().getCollectionElementType().isCompatible(SequenceValue.class))
							return true;
						return false;
					}
    		
    	}
    			));
    	
    	addDialogComponent(new DialogComponentButtonGroup(
    			new SettingsModelString(LocalMuscleNodeModel.CFGKEY_TRADEOFF, LocalMuscleNodeModel.TRADEOFFS[0]), 
    			false, "Performance versus accuracy?", LocalMuscleNodeModel.TRADEOFFS
    			));
    	
    	addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(LocalMuscleNodeModel.CFGKEY_LOG_STDERR, Boolean.FALSE), "Report MUSCLE messages to KNIME console?"));
    }
}

