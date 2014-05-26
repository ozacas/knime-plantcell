package au.edu.unimelb.plantcell.io.ws.mascot.msms;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.ws.Service;

import org.apache.log4j.Logger;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * <code>NodeDialog</code> for the "MSMSSearch" Node.
 * Permits search and download of MS/MS Ion Searches using a MascotEE interface.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MSMSSearchNodeDialog extends DefaultNodeSettingsPane implements ChangeListener {
	private final SettingsModelString url = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_MASCOTEE_URL, MSMSSearchNodeModel.DEFAULT_MASCOTEE_URL);
	private final SettingsModelString database    = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_DATABASE, "");
	private final SettingsModelString enzyme      = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_ENZYME, "");
	private final SettingsModelString fixed_mods  = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_FIXED_MODS, "");
	private final SettingsModelString variable_mods= new SettingsModelString(MSMSSearchNodeModel.CFGKEY_VARIABLE_MODS, "");
	private final SettingsModelString instrument = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_INSTRUMENT, "");
	private final SettingsModelString taxon = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_TAXONOMY, "");
	private final DialogComponentStringSelection available_databases = new DialogComponentStringSelection(database, "Database to search", new String[] { "No databases available." });
	private final DialogComponentStringSelection available_enzymes   = new DialogComponentStringSelection(enzyme, "Proteolytic enzyme", new String[] { "No enzymes available." });
	private final DialogComponentStringSelection fixed_modifications = new DialogComponentStringSelection(fixed_mods, "Fixed modifications", new String[] { "No modifications available." });
	private final DialogComponentStringSelection variable_modifications = new DialogComponentStringSelection(variable_mods, "Variable modifications", new String[] { "No modifications available." });
	private final DialogComponentStringSelection instruments = new DialogComponentStringSelection(instrument, "Instrument", new String[] { "No instruments available." });
	private final DialogComponentStringSelection taxa = new DialogComponentStringSelection(taxon, "Taxonomy", new String[] { "No taxonomies available." });

	
	@SuppressWarnings("unchecked")
	public MSMSSearchNodeDialog() {
		createNewGroup("MascotEE Server URL");
		addDialogComponent(new DialogComponentString(url, "MascotEE URL", true, 60));
		DialogComponentButton refresh_button = new DialogComponentButton("Refresh");
		refresh_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				stateChanged(new ChangeEvent(url));
			}
			
		});
		addDialogComponent(refresh_button);
		
		createNewGroup("Input Data");
		addDialogComponent(new DialogComponentButtonGroup(
				new SettingsModelString(MSMSSearchNodeModel.CFGKEY_DATA_SOURCE, MSMSSearchNodeModel.DATA_SOURCES[0]), 
				"Input MGF from?", true, MSMSSearchNodeModel.DATA_SOURCES, MSMSSearchNodeModel.DATA_SOURCES));
		addDialogComponent(new DialogComponentColumnNameSelection(
				new SettingsModelString(MSMSSearchNodeModel.CFGKEY_DATA_COLUMN, ""), 
				"Column with data", 0, StringValue.class
				));
		
		createNewGroup("Output Data");
		addDialogComponent(new DialogComponentString(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_OUT_MGF, ""), "Save peaklists to..."));
		addDialogComponent(new DialogComponentString(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_OUT_DAT, ""), "Save DAT files to..."));
		
		createNewTab("Identification");
		addDialogComponent(new DialogComponentString(
				new SettingsModelString(MSMSSearchNodeModel.CFGKEY_USERNAME, ""), "Username"
		));
		addDialogComponent(new DialogComponentString(
				new SettingsModelString(MSMSSearchNodeModel.CFGKEY_EMAIL, ""), "E-Mail address eg. user@unimelb.edu.au"
		));
		addDialogComponent(new DialogComponentString(
				new SettingsModelString(MSMSSearchNodeModel.CFGKEY_TITLE, ""), "Job Title (optional)"
		));
		
		createNewTab("Key Settings");
		addDialogComponent(available_databases);
		addDialogComponent(available_enzymes);
		addDialogComponent(fixed_modifications);
		addDialogComponent(variable_modifications);
		final String[] mass_types = new String[] { "Monoisotopic", "Average" };
		addDialogComponent(new DialogComponentButtonGroup(
				new SettingsModelString(MSMSSearchNodeModel.CFGKEY_MASSTYPE, ""), "Mass measurements are...", 
							true, mass_types, mass_types));
		addDialogComponent(instruments);
		addDialogComponent(taxa);
		
		createNewTab("Search Constraints");
		addDialogComponent(
				new DialogComponentNumber(new SettingsModelIntegerBounded(MSMSSearchNodeModel.CFGKEY_MISSED_CLEAVAGES, 0, 0, 9), 
						"Missed cleavages", 1));
		addDialogComponent(
				new DialogComponentString(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_ALLOWED_PROTEIN_MASS, ""), 
						"Allowed protein mass (16 kDa max.)"));
		String[] peptide_charges = new String[] { "Mr", "1+", "1+, 2+ and 3+", "2+", "2+ and 3+", "3+", "4+", "5+", "6+", "7+", "8+" };
		addDialogComponent(new DialogComponentString(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_PEPTIDE_CHARGE, peptide_charges[0]), "Peptide charge"));
		
		this.setHorizontalPlacement(true);
		createNewGroup("Peptide Tolerance");
		addDialogComponent(new DialogComponentString(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_PEPTIDE_TOL_VALUE, ""), ""));
		String[] peptide_units = new String[] { "Da", "mmu", "%", "ppm" };
		addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_PEPTIDE_TOL_UNITS, ""), "", peptide_units));
		this.setHorizontalPlacement(false);
		
		createNewGroup("MS/MS Tolerance");
		addDialogComponent(new DialogComponentString(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_MSMS_TOL_VALUE, ""), ""));
		String[] msms_units = new String[] { "Da", "mmu" };
		addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_MSMS_TOL_UNITS, ""), "", msms_units));
		
		createNewTab("Misc.");
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(MSMSSearchNodeModel.CFGKEY_REPORT_OVERVIEW, true), "Report overview?"));
		String[] report_top_items = new String[] { "AUTO" };
		addDialogComponent(new DialogComponentStringSelection(
				new SettingsModelString(MSMSSearchNodeModel.CFGKEY_REPORT_TOP, ""), "Report top... ", report_top_items));
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(MSMSSearchNodeModel.CFGKEY_QUANT_ICAT, false), "ICAT Quantitation?"));
		addDialogComponent(new DialogComponentString(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_PRECURSOR, ""), "Precursor"));
	}
	
	private List<String> fixDownloadedArray(final String[] newArray, final String default_item_if_none_available) {
		assert(default_item_if_none_available != null);
		
		ArrayList<String> ret = new ArrayList<String>();
		if (newArray != null) {
			for (String s : newArray) {
				ret.add(s);
			}
		}
		if (ret.size() == 0) {
			ret.add(default_item_if_none_available);
		}
		return ret;
	}
	
	/**
	 * Responds to a change in the MascotEE server by updating the relevant form items
	 */
	@Override
	public void stateChanged(ChangeEvent arg0) {
		if (arg0.getSource().equals(url)) {
			try {
				String s = url.getStringValue();
				if (s.length() >= 1) {
					if (!s.endsWith("/")) {
						s += "/";
					}
					s += "ConfigService?wsdl";
					URL u = new URL(s);
					Logger logger = Logger.getLogger("MS/MS Search");
					logger.info("Contacting "+s+" to update search parameters...");
					Service srv = Service.create(u, MSMSSearchNodeModel.CONFIG_NAMESPACE);
					/*ConfigService configService = srv.getPort(ConfigService.class);
					List<String> newDatabases = fixDownloadedArray(configService.availableDatabases(), "No available databases.");
					available_databases.replaceListItems(newDatabases, null);
					List<String> newEnzymes = fixDownloadedArray(configService.availableEnzymes(), "No available enzymes.");
					available_enzymes.replaceListItems(newEnzymes, null);
					List<String> mods = fixDownloadedArray(configService.availableModifications(), "No modifications available.");
					fixed_modifications.replaceListItems(mods, null);
					variable_modifications.replaceListItems(mods, null);
					List<String> taxonomies = fixDownloadedArray(configService.availableTaxa(), "No taxonomy entries available.");
					taxa.replaceListItems(taxonomies, null);
					logger.info("Done!");*/
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
	}
}