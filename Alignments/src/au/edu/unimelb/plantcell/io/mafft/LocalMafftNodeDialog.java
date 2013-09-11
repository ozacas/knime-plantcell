package au.edu.unimelb.plantcell.io.mafft;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
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
public class LocalMafftNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the LocalMuscle node.
     */
    protected LocalMafftNodeDialog() {
    	addDialogComponent(new DialogComponentFileChooser(new SettingsModelString(LocalMafftNodeModel.CFGKEY_ROOT, ""), 
    			"MAFFT Root Folder", JFileChooser.OPEN_DIALOG, true, ""
    			));
    	
    	addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(LocalMafftNodeModel.CFGKEY_SEQUENCES, ""), 
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

