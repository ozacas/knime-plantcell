package au.edu.unimelb.plantcell.proteomics.proteowizard.filter;

import java.util.ArrayList;

import javax.swing.ListSelectionModel;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

public class MSLevelsFilterNodeDialog extends DefaultNodeSettingsPane {
	private final static String[] FORMATS = new String[] { "mzML", "mzXML", "mz5", "MGF" };
	
	@SuppressWarnings("unchecked")
	protected MSLevelsFilterNodeDialog() {
		createNewGroup("MSConvertEE URL");
		addDialogComponent(new DialogComponentString(
					new SettingsModelString(MSLevelsFilterNodeModel.CFGKEY_URL, "http://localhost:8080/msconvertee/webservices/MSConvertImpl?wsdl"),
					"MSConvertEE URL"
				));
		addDialogComponent(new DialogComponentString(
				new SettingsModelString(MSLevelsFilterNodeModel.CFGKEY_USERNAME, ""), "Username"
				));
		addDialogComponent(new DialogComponentString(
				new SettingsModelString(MSLevelsFilterNodeModel.CFGKEY_PASSWORD, ""), "Password"
				));
		this.closeCurrentGroup();
		
		createNewGroup("Data Handling");
		addDialogComponent(new DialogComponentStringSelection(
				new SettingsModelString(MSLevelsFilterNodeModel.CFGKEY_OUTPUT_DATA_FORMAT, FORMATS[0]), "Output file format", FORMATS
				));
		addDialogComponent(new DialogComponentFileChooser(
				new SettingsModelString(MSLevelsFilterNodeModel.CFGKEY_SAVETO, ""), "msconvert-output", 0, true));
		this.closeCurrentGroup();
		
		createNewGroup("Output table should contain... ");
		String[] outputs = MSLevelsFilterNodeModel.TABLE_OUTPUT_DESIRED;
		addDialogComponent(new DialogComponentStringSelection(
				new SettingsModelString(MSLevelsFilterNodeModel.CFGKEY_TABLE_DESIRED, outputs[0]), "Output file format", outputs
				));
		this.closeCurrentGroup();
		
		createNewGroup("Read list of files to process from...");
		addDialogComponent(new DialogComponentColumnNameSelection(
				new SettingsModelString(MSLevelsFilterNodeModel.CFGKEY_INPUT_FILE_COLUMN, ""), 
				"", 0, true, StringValue.class
		));
		
		createNewTab("Filter Settings");
		addFilterSettings();
	}

	/**
	 * Settings for each proteowizard filter node are added here so they appear on a separate tab in the configure dialog
	 */
	private void addFilterSettings() {
		ArrayList<String> levels = new ArrayList<String>();
		for (int i=1; i<10; i++) {
			levels.add(String.valueOf(i));
		}
		addDialogComponent(new DialogComponentStringListSelection(
				new SettingsModelStringArray(MSLevelsFilterNodeModel.CFGKEY_ACCEPTED_MSLEVELS, new String[] {}),
				"Select the scan levels to accept", levels, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, true, 10
				));
	}
}
