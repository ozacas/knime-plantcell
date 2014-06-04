package au.edu.unimelb.plantcell.io.ws.mascot.msms;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.io.read.mascot.MascotReaderNodeModel;


/**
 * <code>NodeDialog</code> for the "AdvMSMSSearch" Node.
 * Permits search and download of MS/MS Ion Searches using a MascotEE interface.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AdvMSMSSearchNodeDialog extends DefaultNodeSettingsPane {
	private final SettingsModelString m_url = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_MASCOTEE_URL, MSMSSearchNodeModel.DEFAULT_MASCOTEE_URL);
	private final SettingsModelDoubleBounded ci = new SettingsModelDoubleBounded(MascotReaderNodeModel.CFGKEY_CONFIDENCE, 
			MascotReaderNodeModel.DEFAULT_CONFIDENCE, 0.0, 1.0);
	private final SettingsModelString m_database          = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_DATABASE, "");
	private final SettingsModelString m_mgf               = new SettingsModelString(AdvMSMSSearchNodeModel.CFGKEY_MGF_INPUT, "");
	private final SettingsModelString m_enzyme            = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_ENZYME, "");
	private final SettingsModelString m_fixed_mods        = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_FIXED_MODS, "");
	private final SettingsModelString m_variable_mods     = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_VARIABLE_MODS, "");
	private final SettingsModelString m_mass_type         = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_MASSTYPE, "");
	private final SettingsModelString m_instrument        = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_INSTRUMENT, "");
	private final SettingsModelString m_taxon             = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_TAXONOMY, "");
	private final SettingsModelString m_peptide_charge    = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_PEPTIDE_CHARGE, "");
	private final SettingsModelString m_allowed_prot_mass = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_ALLOWED_PROTEIN_MASS, "");
	private final SettingsModelString m_missed_cleavages  = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_MISSED_CLEAVAGES, "");
	private final SettingsModelString m_username          = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_USERNAME, "");
	private final SettingsModelString m_email             = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_EMAIL, "");
	private final SettingsModelString m_job_title         = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_TITLE, "");
	private final SettingsModelString m_report_overview   = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_REPORT_OVERVIEW, "");
	private final SettingsModelString m_report_top        = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_REPORT_TOP, "");
	private final SettingsModelString m_quant_icat        = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_QUANT_ICAT, "");
	private final SettingsModelString m_precursor         = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_PRECURSOR, "");
	private final SettingsModelString m_peptide_tolerance = new SettingsModelString(AdvMSMSSearchNodeModel.CFGKEY_PEPTIDE_TOLERANCE, "");
	private final SettingsModelString m_msms_tolerance    = new SettingsModelString(AdvMSMSSearchNodeModel.CFGKEY_MSMS_TOLERANCE, "");
	
	
	
	@SuppressWarnings("unchecked")
	public AdvMSMSSearchNodeDialog() {
		createNewGroup("MascotEE Server URL");
		this.setHorizontalPlacement(true);
		addDialogComponent(new DialogComponentString(m_url, "MascotEE URL", true, 60));
		this.setHorizontalPlacement(false);
		
    	addDialogComponent(new DialogComponentString(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_MASCOTEE_USER, ""), "Username"));
    	addDialogComponent(new DialogComponentPasswordField(new SettingsModelString(MSMSSearchNodeModel.CFGKEY_MASCOTEE_PASSWD, ""), "Password"));
    	
    	createNewGroup("Column with data files");
    	addDialogComponent(new DialogComponentColumnNameSelection(m_mgf, "Column with MGF file locations\n(eg. Location column from List Files node)", 0, StringValue.class));
    	
		createNewGroup("Store mascot result files into...");
		addDialogComponent(new DialogComponentFileChooser(
				new SettingsModelString(MSMSSearchNodeModel.CFGKEY_OUT_DAT, ""), "mascot-results-folder", JFileChooser.OPEN_DIALOG, true));
		
		createNewTab("Key Settings");
		addDialogComponent(new DialogComponentColumnNameSelection(m_database,   "Search Database", 0, StringValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(m_enzyme,     "Enzyme", 0, StringValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(m_fixed_mods, "Fixed Modifications", 0, false, true, StringValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(m_variable_mods, "Variable Modifications", 0, false, true, StringValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(m_instrument, "Instrument", 0, StringValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(m_mass_type, "Mass Type", 0, StringValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(m_allowed_prot_mass, "Allowed protein mass", 0, StringValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(m_peptide_charge, "Peptide charge", 0, StringValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(m_taxon, "Taxonomy", 0, StringValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(m_missed_cleavages, "Missed Cleavages", 0, IntValue.class));
		
		createNewGroup("Tolerance");
		addDialogComponent(new DialogComponentColumnNameSelection(m_peptide_tolerance, "Peptide tolerance", 0, StringValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(m_msms_tolerance, "MS/MS tolerance", 0, StringValue.class));
		
		createNewTab("Non-critical settings");
		addDialogComponent(new DialogComponentColumnNameSelection(m_report_overview, "Report overview?", 0, false, true, IntValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(m_report_top, "Report top", 0, false, true, StringValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(m_quant_icat, "ICAT quantitation?", 0, false, true, IntValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(m_precursor, "Precursor", 0, false, true, StringValue.class));
		
		createNewGroup("Identification");
		addDialogComponent(new DialogComponentColumnNameSelection(m_username, "Username", 0, false, true, StringValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(m_email, "Email address", 0, false, true, StringValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(m_job_title, "Job title", 0, false, true, StringValue.class));
		
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
	
	}
	
	protected void updateConfidenceField(String sv) {
		if (sv == null) {
			sv = MascotReaderNodeModel.DEFAULT_RESULTTYPE;
		}
		ci.setEnabled(sv.trim().toLowerCase().startsWith("confident"));
	}
	
	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] inSpec) {
		try {
			m_url.setStringValue(settings.getString(MSMSSearchNodeModel.CFGKEY_MASCOTEE_URL));

			// look at configured result type and decide what widgets to enable
			String result_type = settings.getString(MascotReaderNodeModel.CFGKEY_RESULTTYPE);
			updateConfidenceField(result_type);
		} catch (InvalidSettingsException e) {
			e.printStackTrace();
		}
	}
	
	@Override
    public void saveAdditionalSettingsTo(NodeSettingsWO settings) {
    	// since this node doesnt support files but the superclass of the nodemodel implementation does, we
    	// must fake dialog values for those settings...
    	settings.addStringArray(MascotReaderNodeModel.CFGKEY_FILES, new String[0]);
    }
}