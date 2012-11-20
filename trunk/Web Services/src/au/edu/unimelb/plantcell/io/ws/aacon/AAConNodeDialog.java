package au.edu.unimelb.plantcell.io.ws.aacon;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue;

public class AAConNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	public AAConNodeDialog() {
		addDialogComponent(new DialogComponentString(
				new SettingsModelString(AAConNodeModel.CFGKEY_URL, "http://proteomics.bio21.unimelb.edu.au/jabaws"),
				"JabaWS URL"
				));
		
		addDialogComponent(new DialogComponentColumnNameSelection(
				new SettingsModelString(AAConNodeModel.CFGKEY_SEQUENCE_COL, "http://proteomics.bio21.unimelb.edu.au/jabaws"),
				"Protein Sequence", 0, AlignmentValue.class
				));
	}
}
