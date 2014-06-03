package au.edu.unimelb.plantcell.io.ws.mascot.msms;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.soap.SOAPException;
import javax.xml.ws.Service;

import org.apache.log4j.Logger;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;
import au.edu.unimelb.plantcell.io.read.mascot.MascotReaderNodeModel;
import au.edu.unimelb.plantcell.servers.mascotee.endpoints.ConfigService;


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
	private final int VISIBLE_MODIFICATION_ROWS = 15;		// display 13 mods on screen at once
	private final static List<String> initial_list = new ArrayList<String>();
	static {
		initial_list.add("Please enter a valid MascotEE URL and press refresh.");
	}
	private final SettingsModelString url               = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_MASCOTEE_URL, "");
	private final SettingsModelString database          = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_DATABASE, "");
	private final SettingsModelString enzyme            = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_ENZYME, "");
	private final SettingsModelStringArray fixed_mods   = new SettingsModelStringArray(MSMSSearchNodeModel.CFGKEY_FIXED_MODS, new String[] {});
	private final SettingsModelStringArray variable_mods= new SettingsModelStringArray(MSMSSearchNodeModel.CFGKEY_VARIABLE_MODS, new String[] {});
	private final SettingsModelString instrument        = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_INSTRUMENT, "");
	private final SettingsModelString taxon             = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_TAXONOMY, "");
	private final SettingsModelString top_hits          = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_REPORT_TOP, "");
	private final SettingsModelString file_list_field   = new SettingsModelString("__internal_file_list_column", "");
	private final SettingsModelString spectra_list_field= new SettingsModelString("__internal_spectra_list_column", "");
	private final SettingsModelString peptide_charge    = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_PEPTIDE_CHARGE, "");
	private final SettingsModelDoubleBounded ci = new SettingsModelDoubleBounded(MascotReaderNodeModel.CFGKEY_CONFIDENCE, 
			MascotReaderNodeModel.DEFAULT_CONFIDENCE, 0.0, 1.0);
	
	private final DialogComponentStringSelection available_databases = new DialogComponentStringSelection(database, "Database to search", new String[] { "No databases available." });
	private final DialogComponentStringSelection available_enzymes   = new DialogComponentStringSelection(enzyme, "Proteolytic enzyme", new String[] { "No enzymes available." });
	private final DialogComponentStringListSelection fixed_modifications = new DialogComponentStringListSelection(fixed_mods, "Fixed modifications", initial_list, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, false, VISIBLE_MODIFICATION_ROWS);
	private final DialogComponentStringListSelection variable_modifications = new DialogComponentStringListSelection(variable_mods, "Variable modifications", initial_list, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, false, VISIBLE_MODIFICATION_ROWS);
	private final DialogComponentStringSelection instruments = new DialogComponentStringSelection(instrument, "Instrument", new String[] { "No instruments available." });
	private final DialogComponentStringSelection taxa = new DialogComponentStringSelection(taxon, "Taxonomy", new String[] { "No taxonomies available." });
	private final DialogComponentStringSelection report_top = new DialogComponentStringSelection(top_hits, "Report top... X hits", new String[] { "No top hits available." });
	private final DialogComponentStringSelection peptide_charges = new DialogComponentStringSelection(peptide_charge, "Peptide charge", new String[] { "No peptide charges available." });
	
	@SuppressWarnings("unchecked")
	public MSMSSearchNodeDialog() {
		createNewGroup("MascotEE Server URL");
		this.setHorizontalPlacement(true);
		addDialogComponent(new DialogComponentString(url, "MascotEE URL", true, 60));
		DialogComponentButton refresh_button = new DialogComponentButton("Refresh");
		refresh_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				stateChanged(new ChangeEvent(url));
			}
			
		});
		addDialogComponent(refresh_button);
		this.setHorizontalPlacement(false);
		
    	addDialogComponent(new DialogComponentString(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_MASCOTEE_USER, ""), "Username"));
    	addDialogComponent(new DialogComponentPasswordField(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_MASCOTEE_PASSWD, ""), "Password"));
    	
		
		createNewGroup("Input Data");
		this.setHorizontalPlacement(true);
		final SettingsModelString sms = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_DATA_SOURCE, MSMSSearchNodeModel.DATA_SOURCES[0]);
		sms.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				updateDataSourceColumnSelectors(sms.getStringValue());
			}
			
		});
		addDialogComponent(new DialogComponentButtonGroup(sms,
				"Input MGF from?", true, MSMSSearchNodeModel.DATA_SOURCES, MSMSSearchNodeModel.DATA_SOURCES));
		addDialogComponent(new DialogComponentColumnNameSelection(file_list_field, "", 0, false, true, StringValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(spectra_list_field, "", 0, false, true, SpectraValue.class));
		this.setHorizontalPlacement(false);
		
		createNewGroup("Save peaklists created into folder...");
		addDialogComponent(new DialogComponentFileChooser(
				new SettingsModelString(MSMSSearchNodeModel.CFGKEY_OUT_MGF, ""), "peaklist-folder", JFileChooser.OPEN_DIALOG, true));
		createNewGroup("Store mascot result files into...");
		addDialogComponent(new DialogComponentFileChooser(
				new SettingsModelString(MSMSSearchNodeModel.CFGKEY_OUT_DAT, ""), "mascot-results-folder", JFileChooser.OPEN_DIALOG, true));
		
		createNewTab("Key Settings");
		addDialogComponent(available_databases);
		addDialogComponent(available_enzymes);
		
		this.createNewGroup("Chemical modifications");
		this.setHorizontalPlacement(true);
		addDialogComponent(fixed_modifications);
		addDialogComponent(variable_modifications);
		this.setHorizontalPlacement(false);
		
		createNewGroup("Instrument settings");
		this.setHorizontalPlacement(true);
		addDialogComponent(instruments);
		final String[] mass_types = new String[] { "Monoisotopic", "Average" };
		addDialogComponent(new DialogComponentButtonGroup(
				new SettingsModelString(MSMSSearchNodeModel.CFGKEY_MASSTYPE, ""), "Masses are... ", 
							true, mass_types, mass_types));
		this.setHorizontalPlacement(false);

		createNewTab("Search Constraints");
		addDialogComponent(
				new DialogComponentNumber(new SettingsModelIntegerBounded(MSMSSearchNodeModel.CFGKEY_MISSED_CLEAVAGES, 0, 0, 9), 
						"Missed cleavages", 1));
		addDialogComponent(
				new DialogComponentString(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_ALLOWED_PROTEIN_MASS, ""), 
						"Allowed protein mass (16 kDa max.)"));
		addDialogComponent(peptide_charges);
		
		createNewGroup("Peptide Tolerance");
		this.setHorizontalPlacement(true);
		addDialogComponent(new DialogComponentString(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_PEPTIDE_TOL_VALUE, ""), ""));
		String[] peptide_units = new String[] { "Da", "mmu", "%", "ppm" };
		addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_PEPTIDE_TOL_UNITS, ""), "", peptide_units));
		this.setHorizontalPlacement(false);
		
		createNewGroup("MS/MS Tolerance");
		this.setHorizontalPlacement(true);
		addDialogComponent(new DialogComponentString(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_MSMS_TOL_VALUE, ""), ""));
		String[] msms_units = new String[] { "Da", "mmu" };
		addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_MSMS_TOL_UNITS, ""), "", msms_units));
		this.setHorizontalPlacement(false);
	
		createNewGroup("Report hits only from which taxa?");
		addDialogComponent(taxa);
		
		createNewTab("Results Processing");
    	final SettingsModelString result_type = new SettingsModelString(MascotReaderNodeModel.CFGKEY_RESULTTYPE, MascotReaderNodeModel.DEFAULT_RESULTTYPE);
    	DialogComponentButtonGroup bg = new DialogComponentButtonGroup(result_type, true, "Report which peptide hits per query?", 
    			MascotReaderNodeModel.RESULT_TYPES);
    	result_type.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				updateConfidenceField(result_type.getStringValue());
			}
    		
    	});
        bg.setToolTipText("Which peptide identifications per spectra do you want to see?");
        addDialogComponent(bg);
        addDialogComponent(new DialogComponentNumberEdit(ci,"Identity Threshold Confidence", 5));
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(MascotReaderNodeModel.CFGKEY_WANT_SPECTRA, true), "Want MS/MS spectra?"));
	

		createNewTab("Misc.");
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(MSMSSearchNodeModel.CFGKEY_REPORT_OVERVIEW, true), "Report overview?"));
		addDialogComponent(report_top);
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(MSMSSearchNodeModel.CFGKEY_QUANT_ICAT, false), "ICAT Quantitation?"));
		addDialogComponent(new DialogComponentString(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_PRECURSOR, ""), "Precursor"));
	
		createNewTab("Identification (optional)");
		addDialogComponent(new DialogComponentString(
				new SettingsModelString(MSMSSearchNodeModel.CFGKEY_USERNAME, ""), "Username"
		));
		addDialogComponent(new DialogComponentString(
				new SettingsModelString(MSMSSearchNodeModel.CFGKEY_EMAIL, ""), "E-Mail address eg. user@unimelb.edu.au"
		));
		addDialogComponent(new DialogComponentString(
				new SettingsModelString(MSMSSearchNodeModel.CFGKEY_TITLE, ""), "Job Title"
		));
	}
	
	protected void updateConfidenceField(String sv) {
		if (sv == null) {
			sv = MascotReaderNodeModel.DEFAULT_RESULTTYPE;
		}
		ci.setEnabled(sv.trim().toLowerCase().startsWith("confident"));
	}

	protected void updateDataSourceColumnSelectors(String cur_value) {
		if (cur_value == null) {
			cur_value = MSMSSearchNodeModel.DATA_SOURCES[0];
		}
		if (cur_value.equals(MSMSSearchNodeModel.DATA_SOURCES[2])) {
			file_list_field.setEnabled(false);
			spectra_list_field.setEnabled(true);
		} else {
			spectra_list_field.setEnabled(false);
			file_list_field.setEnabled(true);
		}
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
	
	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] inSpec) {
		try {
			url.setStringValue(settings.getString(MSMSSearchNodeModel.CFGKEY_MASCOTEE_URL));
			stateChanged(new ChangeEvent(url));		// get mascot config to be loaded from previously configured MascotEE server
			
			// look at configured data source and decide which column selectors to enable
			String ds = settings.getString(MSMSSearchNodeModel.CFGKEY_DATA_SOURCE);
			updateDataSourceColumnSelectors(ds);
			// and setup the column selections appropriately for the data source
			String col = settings.getString(MSMSSearchNodeModel.CFGKEY_DATA_COLUMN);
			if (MSMSSearchNodeModel.DATA_SOURCES[2].equals(ds)) {
				spectra_list_field.setStringValue(col);
				file_list_field.setStringValue("<none>");
			} else {
				spectra_list_field.setStringValue("<none>");
				file_list_field.setStringValue(col);
			}
			
			// look at configured result type and decide what widgets to enable
			String result_type = settings.getString(MascotReaderNodeModel.CFGKEY_RESULTTYPE);
			updateConfidenceField(result_type);
			
			// ensure database field is correctly initialised now that mascot config has been loaded
			database.setStringValue(settings.getString(MSMSSearchNodeModel.CFGKEY_DATABASE));
		} catch (InvalidSettingsException e) {
			e.printStackTrace();
		}
	}
	
	@Override
    public void saveAdditionalSettingsTo(NodeSettingsWO settings) {
    	// since this node doesnt support files but the superclass of the nodemodel implementation does, we
    	// must fake dialog values for those settings...
    	settings.addStringArray(MascotReaderNodeModel.CFGKEY_FILES, new String[0]);
    	
    	// CFGKEY_DATA_COLUMN is actually the column from __internal_* fields which is required by the data source. In this way the
    	// NodeModel only has to deal with one column
    	String new_value = file_list_field.isEnabled() ? file_list_field.getStringValue() : spectra_list_field.getStringValue();
    	settings.addString(MSMSSearchNodeModel.CFGKEY_DATA_COLUMN, new_value);
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
					ConfigService configService = srv.getPort(ConfigService.class);
					List<String> newDatabases = fixDownloadedArray(configService.availableDatabases(), "No available databases.");
					Collections.sort(newDatabases);	// convenience for the user
					available_databases.replaceListItems(newDatabases, null);
					List<String> newEnzymes = fixDownloadedArray(configService.availableEnzymes(), "No available enzymes.");
					available_enzymes.replaceListItems(newEnzymes, null);
					List<String> mods = fixDownloadedArray(configService.availableModifications(), "No modifications available.");
					fixed_modifications.replaceListItems(mods, (String[])null);			// null signifies select previous selection if still available
					variable_modifications.replaceListItems(mods, (String[])null);
					
					List<String> new_instruments = fixDownloadedArray(configService.availableInstruments(), "No instruments available.");
					instruments.replaceListItems(new_instruments, null);
					List<String> taxonomies = fixDownloadedArray(configService.availableTaxa(), "No taxonomy entries available.");
					taxa.replaceListItems(taxonomies, null);
					
					List<String> new_top = fixDownloadedArray(configService.availableTopHits(), "No top hits available.");
					report_top.replaceListItems(new_top, null);
					List<String> new_peptide_charges = fixDownloadedArray(configService.availablePeptideChargeStates(), "No peptide charges available.");
					peptide_charges.replaceListItems(new_peptide_charges, null);
					logger.info("Done!");
				}
			} catch (MalformedURLException|SOAPException e) {
				e.printStackTrace();
			}
		}
	}
}