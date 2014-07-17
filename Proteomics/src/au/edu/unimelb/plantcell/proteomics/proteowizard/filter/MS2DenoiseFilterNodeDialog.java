package au.edu.unimelb.plantcell.proteomics.proteowizard.filter;

import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;



public class MS2DenoiseFilterNodeDialog extends MSLevelsFilterNodeDialog {	
	protected MS2DenoiseFilterNodeDialog() {
		super();
	}

	@Override
	protected String getAdditionalSettingsTabName() {
		return "MS/MS Denoise Settings";
	}
	
	/**
	 * Settings for each proteowizard filter node are added here so they appear on a separate tab in the configure dialog
	 */
	protected void addFilterSettings() {
		createNewTab(getAdditionalSettingsTabName());
		addDialogComponent(new DialogComponentNumber(
				new SettingsModelIntegerBounded(MS2DenoiseFilterNodeModel.CFGKEY_N, 6, 1, 1000), 
				"Number of peaks to keep per window", 1));
		
		addDialogComponent(new DialogComponentNumber(
				new SettingsModelDoubleBounded(MS2DenoiseFilterNodeModel.CFGKEY_WINDOW, 30.0, 0.0, 10000.0),
				"Window width (Daltons)", 5));
		
		addDialogComponent(new DialogComponentBoolean(
				new SettingsModelBoolean(MS2DenoiseFilterNodeModel.CFGKEY_RELAX, Boolean.TRUE), "Allow more data below multicharge precursors"
				));
	}
}
