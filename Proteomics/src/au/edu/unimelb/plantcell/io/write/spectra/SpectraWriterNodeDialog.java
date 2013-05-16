package au.edu.unimelb.plantcell.io.write.spectra;

import javax.swing.JFileChooser;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;

/**
 * <code>NodeDialog</code> for the "SpectraWriter" Node.
 * Writes a spectra column out to disk for processing with other Mass Spec. software. Supports MGF format but does not guarantee that all input data will be preserved in the created file.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class SpectraWriterNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring SpectraWriter node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    @SuppressWarnings("unchecked")
	protected SpectraWriterNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentFileChooser(
        		new SettingsModelString(SpectraWriterNodeModel.CFGKEY_FILE, ""), 
        		"file-history", JFileChooser.SAVE_DIALOG, false));
        
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean(SpectraWriterNodeModel.CFGKEY_OVERWRITE, false), "Overwrite?"));
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(SpectraWriterNodeModel.CFGKEY_COLUMN, ""), "Column to save: ", 0, SpectraValue.class));
        
        addDialogComponent(new DialogComponentStringSelection(
        		new SettingsModelString(SpectraWriterNodeModel.CFGKEY_FORMAT, "Mascot Generic Format"), "Output format", "Mascot Generic Format"));
   
        addDialogComponent(new DialogComponentColumnNameSelection (
        		new SettingsModelString(SpectraWriterNodeModel.CFGKEY_FILENAME_SUFFIX, ""), "Suffix for save to multiple files: ", 0, false, true,
        		new ColumnFilter() {

					@Override
					public String allFilteredMsg() {
						return "No suitable suffix columns available (must contain no more than 40 suffixes approx.)!";
					}

					@Override
					public boolean includeColumn(DataColumnSpec cs) {
						if ((cs.getType().isCompatible(StringValue.class) && cs.getDomain().hasValues()) ||
								cs.getType().isCompatible(IntValue.class)) {
							return true;
						}
						return false;
					}
        			
        		}));
        	
    }
}

