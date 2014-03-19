package au.edu.unimelb.plantcell.proteomics.itraq;

import java.io.File;

import javax.swing.JFileChooser;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "ITraqAnalyzer" Node.
 * Given a (set of) proteomics runs, with identified peptides, proteins and iTRAQ quantitation values this nodes performs an analysis and provides normalised results for the user in easy-to-read format. Based on method published in the scientific literature.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatic
 */
public class ITraqAnalyzerNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the ITraqAnalyzer node.
     */
    @SuppressWarnings("unchecked")
	protected ITraqAnalyzerNodeDialog() {
    	// which R installation to use?
    	createNewGroup("R installation to use?");
    	addDialogComponent(new DialogComponentFileChooser(
    			new SettingsModelString(ITraqAnalyzerNodeModel.CFGKEY_R_FOLDER_ROOT, getDefaultRLocation()),
    			"r-folder-history", JFileChooser.OPEN_DIALOG, true
    			));
    	
    	// iTRAQ peptide level settings
    	createNewGroup("iTRAQ Settings");
    	addDialogComponent(new DialogComponentColumnFilter(
    			new SettingsModelFilterString(ITraqAnalyzerNodeModel.CFGKEY_SELECTED_CHANNELS), 0, true
    			));
    	addDialogComponent(new DialogComponentColumnNameSelection(
    			new SettingsModelString(ITraqAnalyzerNodeModel.CFGKEY_REP1, ""), "Technical replicate #1 iTRAQ channel", 0, true, false, DoubleValue.class
    			));
    	addDialogComponent(new DialogComponentColumnNameSelection(
    			new SettingsModelString(ITraqAnalyzerNodeModel.CFGKEY_REP2, ""), "Technical replicate #2 iTRAQ channel", 0, true, false, DoubleValue.class
    			));
    }

    /**
     * Responsible for providing a path (not necessarily accessible but preferred) to R on the current platform.
     * Used by the dialog code to initialise the form, so the better choice the better....
     * 
     * @return
     */
	private String getDefaultRLocation() {
		try {
			File Rexe = ITraqAnalyzerNodeModel.locateR();
			
			return Rexe.getAbsolutePath();
		} catch (InvalidSettingsException ise) {
			// FALLTHRU
		}
		
		// nope cant find it... ok so try a default...
		return "/usr/bin/R";
	}
}

