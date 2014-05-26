package au.edu.unimelb.plantcell.io.ws.mascot.msms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.io.read.mascot.MascotReaderNodeModel;


/**
 * This is the model implementation of DatFileDownload.
 * Permits downloading of Mascot DAT files via a JAX-WS web service and will load each dat file into the output table as per the mascot reader. The node also saves the DAT files to the user-specified folder.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MSMSSearchNodeModel extends MascotReaderNodeModel {
	private static final NodeLogger logger = NodeLogger.getLogger("MS/MS Mascot Search");
	
	public final static QName SEARCH_NAMESPACE = 
			new QName("http://www.plantcell.unimelb.edu.au/bioinformatics/wsdl", "SearchService");
	public final static QName CONFIG_NAMESPACE = 
			new QName("http://www.plantcell.unimelb.edu.au/bioinformatics/wsdl", "ConfigService");
	
	public final static String CFGKEY_MASCOTEE_URL = "mascotee-url";
	public final static String CFGKEY_DATA_SOURCE  = "data-source";
	public final static String CFGKEY_DATA_COLUMN  = "input-data-column";
	// mascot ms/ms ion search parameters
	public final static String CFGKEY_USERNAME     = "mascot-username";
	public final static String CFGKEY_EMAIL        = "mascot-email";
	public final static String CFGKEY_TITLE        = "mascot-job-title";
	public final static String CFGKEY_DATABASE     = "mascot-database";
	public final static String CFGKEY_FIXED_MODS   = "mascot-fixed-modifications";
	public final static String CFGKEY_VARIABLE_MODS= "mascot-variable-modifications";
	public final static String CFGKEY_MASSTYPE     = "mascot-mass-measurement-type";
	public final static String CFGKEY_TAXONOMY     = "mascot-taxonomy";
	public final static String CFGKEY_ENZYME       = "mascot-enzyme";
	public final static String CFGKEY_MISSED_CLEAVAGES = "mascot-missed-cleavages";
	public final static String CFGKEY_ALLOWED_PROTEIN_MASS = "mascot-protein-mass-allowed";
	public final static String CFGKEY_PEPTIDE_TOL_VALUE = "mascot-peptide-tolerance-value";
	public final static String CFGKEY_PEPTIDE_TOL_UNITS = "mascot-peptide-tolerance-unit";
	public final static String CFGKEY_MSMS_TOL_VALUE = "mascot-msms-tolerance-value";
	public final static String CFGKEY_MSMS_TOL_UNITS = "mascot-msms-tolerance-unit";
	public final static String CFGKEY_PEPTIDE_CHARGE = "mascot-peptide-charge";
	public final static String CFGKEY_REPORT_OVERVIEW= "mascot-report-overview";
	public final static String CFGKEY_REPORT_TOP     = "mascot-report-top";
	public final static String CFGKEY_QUANT_ICAT     = "mascot-quant-icat";
	public final static String CFGKEY_INSTRUMENT     = "mascot-instrument";
	public final static String CFGKEY_PRECURSOR      = "mascot-precursor";
	public static final String CFGKEY_OUT_MGF = "output-mgf-folder";
	public static final String CFGKEY_OUT_DAT = "output-mascot-dat-file-folder";
	
	public final static String   DEFAULT_MASCOTEE_URL = "http://mascot.plantcell.unimelb.edu.au:8080/mascot/";
	public final static String[] DATA_SOURCES = new String[] { "from input file (select column)", "aggregated input files (select column)", "from input MS/MS spectra (select column)" };

	
	
	private final SettingsModelString m_url = new SettingsModelString(CFGKEY_MASCOTEE_URL, DEFAULT_MASCOTEE_URL);
	private final SettingsModelString m_data_source = new SettingsModelString(CFGKEY_DATA_SOURCE, DATA_SOURCES[0]);
	private final SettingsModelString m_column = new SettingsModelString(CFGKEY_DATA_COLUMN, "");
	private final SettingsModelString m_out_mgf = new SettingsModelString(CFGKEY_OUT_MGF, "");
	private final SettingsModelString m_out_dat = new SettingsModelString(CFGKEY_OUT_DAT, "");
	
	// mascot ms/ms ion search persisted state
	private final SettingsModelString m_user        = new SettingsModelString(CFGKEY_USERNAME, "");
	private final SettingsModelString m_email       = new SettingsModelString(CFGKEY_EMAIL, "");
	private final SettingsModelString m_job_title   = new SettingsModelString(CFGKEY_TITLE, "");
	private final SettingsModelString m_database    = new SettingsModelString(CFGKEY_DATABASE, "");
	private final SettingsModelStringArray m_fixed_mods = new SettingsModelStringArray(CFGKEY_FIXED_MODS, new String[] {});
	private final SettingsModelStringArray m_variable_mods = new SettingsModelStringArray(CFGKEY_VARIABLE_MODS, new String[] {});
	private final SettingsModelString m_mass_type = new SettingsModelString(CFGKEY_MASSTYPE, "Monoisotopic");
	private final SettingsModelString m_taxonomy  = new SettingsModelString(CFGKEY_TAXONOMY, "");
	private final SettingsModelString m_enzyme    = new SettingsModelString(CFGKEY_ENZYME, "");
	private final SettingsModelIntegerBounded m_missed_cleavages = new SettingsModelIntegerBounded(CFGKEY_MISSED_CLEAVAGES, 0, 0, 9);
	private final SettingsModelString m_protein_mass = new SettingsModelString(CFGKEY_ALLOWED_PROTEIN_MASS, "");
	private final SettingsModelString m_peptide_tolerance = new SettingsModelString(CFGKEY_PEPTIDE_TOL_VALUE, "");
	private final SettingsModelString m_peptide_tolerance_unit = new SettingsModelString(CFGKEY_PEPTIDE_TOL_UNITS, "Da");
	private final SettingsModelString m_msms_tolerance = new SettingsModelString(CFGKEY_MSMS_TOL_VALUE, "");
	private final SettingsModelString m_msms_tolerance_unit = new SettingsModelString(CFGKEY_MSMS_TOL_UNITS, "Da");
	private final SettingsModelString m_peptide_charge = new SettingsModelString(CFGKEY_PEPTIDE_CHARGE, "");
	private final SettingsModelBoolean m_report_overview = new SettingsModelBoolean(CFGKEY_REPORT_OVERVIEW, true);
	private final SettingsModelString  m_report_top = new SettingsModelString(CFGKEY_REPORT_TOP, "AUTO");
	private final SettingsModelBoolean m_quant_icat = new SettingsModelBoolean(CFGKEY_QUANT_ICAT, false);
	private final SettingsModelString  m_instrument = new SettingsModelString(CFGKEY_INSTRUMENT, "");
	private final SettingsModelString  m_precursor  = new SettingsModelString(CFGKEY_PRECURSOR, "");
			
	public MSMSSearchNodeModel() {
		// same output ports as superclass but this node has an input port to get either a column of files
		// or a column of spectra to search with...
		super(1, 2);
	}
	
	/**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] 
    		execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
    	logger.info("Performing ms/ms ion search using MascotEE: "+m_url.getStringValue());
    	
    	// 1. queue the searches with the specified mascotee installation
    	List<File> input_mgf_files = new ArrayList<File>();
    	
    	// 2. wait for the jobs to complete
    	
    	// 3. download the results
    	
    	// 4. load the results into the output table by filename
    	List<File> downloaded_files = new ArrayList<File>();
		if (downloaded_files.size() < 1) {
		  	throw new InvalidSettingsException("No downloaded files available! Nothing to load.");
		}
		  
		// now that the files are downloaded we need to initialise the superclass with the chosen files...
		super.setFiles(downloaded_files);
		  
		// now process the downloaded dat files as per the mascot reader node
		return super.execute(inData, exec);
    }
    
	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		super.saveSettingsTo(settings);
		m_url.saveSettingsTo(settings);
		m_column.saveSettingsTo(settings);
		m_data_source.saveSettingsTo(settings);
		// mascot search state
		m_user.saveSettingsTo(settings);
		m_email.saveSettingsTo(settings);
		m_job_title.saveSettingsTo(settings);
		m_database.saveSettingsTo(settings);
		m_fixed_mods.saveSettingsTo(settings);
		m_variable_mods.saveSettingsTo(settings);
		m_mass_type.saveSettingsTo(settings);
		m_taxonomy.saveSettingsTo(settings);
		m_enzyme.saveSettingsTo(settings);
		m_missed_cleavages.saveSettingsTo(settings);
		m_protein_mass.saveSettingsTo(settings);
		m_peptide_tolerance.saveSettingsTo(settings);
		m_peptide_tolerance_unit.saveSettingsTo(settings);
		m_msms_tolerance.saveSettingsTo(settings);
		m_msms_tolerance_unit.saveSettingsTo(settings);
		m_peptide_charge.saveSettingsTo(settings);
		m_report_overview.saveSettingsTo(settings);
		m_report_top.saveSettingsTo(settings);
		m_quant_icat.saveSettingsTo(settings);
		m_instrument.saveSettingsTo(settings);
		m_precursor.saveSettingsTo(settings);
		
		m_out_mgf.saveSettingsTo(settings);
		m_out_dat.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.validateSettings(settings);
		m_url.validateSettings(settings);
		m_column.validateSettings(settings);
		m_data_source.validateSettings(settings);
		// mascot search state
		m_user.validateSettings(settings);
		m_email.validateSettings(settings);
		m_job_title.validateSettings(settings);
		m_database.validateSettings(settings);
		m_fixed_mods.validateSettings(settings);
		m_variable_mods.validateSettings(settings);
		m_mass_type.validateSettings(settings);
		m_taxonomy.validateSettings(settings);
		m_enzyme.validateSettings(settings);
		m_missed_cleavages.validateSettings(settings);
		m_protein_mass.validateSettings(settings);
		m_peptide_tolerance.validateSettings(settings);
		m_peptide_tolerance_unit.validateSettings(settings);
		m_msms_tolerance.validateSettings(settings);
		m_msms_tolerance_unit.validateSettings(settings);
		m_peptide_charge.validateSettings(settings);
		m_report_overview.validateSettings(settings);
		m_report_top.validateSettings(settings);
		m_quant_icat.validateSettings(settings);
		m_instrument.validateSettings(settings);
		m_precursor.validateSettings(settings);
		
		m_out_mgf.validateSettings(settings);
		m_out_dat.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.loadValidatedSettingsFrom(settings);
		m_url.loadSettingsFrom(settings);
		m_column.loadSettingsFrom(settings);
		m_data_source.loadSettingsFrom(settings);
		// mascot search state
		m_user.loadSettingsFrom(settings);
		m_email.loadSettingsFrom(settings);
		m_job_title.loadSettingsFrom(settings);
		m_database.loadSettingsFrom(settings);
		m_fixed_mods.loadSettingsFrom(settings);
		m_variable_mods.loadSettingsFrom(settings);
		m_mass_type.loadSettingsFrom(settings);
		m_taxonomy.loadSettingsFrom(settings);
		m_enzyme.loadSettingsFrom(settings);
		m_missed_cleavages.loadSettingsFrom(settings);
		m_protein_mass.loadSettingsFrom(settings);
		m_peptide_tolerance.loadSettingsFrom(settings);
		m_peptide_tolerance_unit.loadSettingsFrom(settings);
		m_msms_tolerance.loadSettingsFrom(settings);
		m_msms_tolerance_unit.loadSettingsFrom(settings);
		m_peptide_charge.loadSettingsFrom(settings);
		m_report_overview.loadSettingsFrom(settings);
		m_report_top.loadSettingsFrom(settings);
		m_quant_icat.loadSettingsFrom(settings);
		m_instrument.loadSettingsFrom(settings);
		m_precursor.loadSettingsFrom(settings);
		
		m_out_mgf.loadSettingsFrom(settings);
		m_out_dat.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
	}
	
	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
	}

}