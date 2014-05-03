package au.edu.unimelb.plantcell.io.mafft;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.io.muscle.SequenceColumn4AlignmentFilter;

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
public class LocalMafftNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the LocalMuscle node.
     */
    protected LocalMafftNodeDialog() {
    	createNewGroup("Which folder contains the mafft program?");
    	addDialogComponent(new DialogComponentFileChooser(new SettingsModelString(LocalMafftNodeModel.CFGKEY_ROOT, ""), 
    			"MAFFT Root Folder", JFileChooser.OPEN_DIALOG, true, ""
    			));
    	closeCurrentGroup();
    	
    	addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(LocalMafftNodeModel.CFGKEY_SEQUENCES, ""), 
    			"Sequences to align...", 0, true, new SequenceColumn4AlignmentFilter()));
    	
    	final SettingsModelString sms = new SettingsModelString(LocalMafftNodeModel.CFGKEY_ALGO, LocalMafftNodeModel.TRADEOFFS[0]);
    	final SettingsModelString udef= new SettingsModelString(LocalMafftNodeModel.CFGKEY_USER_DEFINED, "");
    	final DialogComponentButtonGroup bg = new DialogComponentButtonGroup(
    			sms, false, "Algorithm?", LocalMafftNodeModel.TRADEOFFS);
    	sms.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				udef.setEnabled(sms.getStringValue().equals(LocalMafftNodeModel.TRADEOFFS[1]));
			}
    		
    	});
    	addDialogComponent(bg);
    	
    	addDialogComponent(new DialogComponentString(udef, "Custom arguments (advanced users only)", false, 80
    			));
    	
    	addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(LocalMafftNodeModel.CFGKEY_LOG_STDERR, Boolean.FALSE), "Report MAFFT messages to KNIME console?"));
    }
}

