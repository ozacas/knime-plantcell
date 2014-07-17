package au.edu.unimelb.plantcell.proteomics.proteowizard.filter;

import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


public class ThresholdFilterNodeDialog extends MSLevelsFilterNodeDialog {	
	protected ThresholdFilterNodeDialog() {
		super();
		
	}

	@Override
	protected String getAdditionalSettingsTabName() {
		return "Threshold Settings";
	}
	
	/**
	 * Settings for each proteowizard filter node are added here so they appear on a separate tab in the configure dialog
	 */
	protected void addFilterSettings() {
		createNewTab(getAdditionalSettingsTabName());
		addDialogComponent(new DialogComponentStringSelection(
				new SettingsModelString(ThresholdFilterNodeModel.CFGKEY_METHOD, ThresholdFilterNodeModel.METHODS[0]), 
				"Peak filter method", 
				ThresholdFilterNodeModel.METHODS));
		
		addDialogComponent(new DialogComponentNumber(new SettingsModelDouble(ThresholdFilterNodeModel.CFGKEY_THRESHOLD, 0.0d),
				"Threshold", 1.0));
	
		addDialogComponent(new DialogComponentButtonGroup(
				new SettingsModelString(ThresholdFilterNodeModel.CFGKEY_ORIENTATION, ""), true, "Retain those peaks which... ", 
				ThresholdFilterNodeModel.THRESHOLD_ORIENTATIONS));
		
		addMSLevelsComponent("Select the scan levels to threshold", 3);
	}
}
