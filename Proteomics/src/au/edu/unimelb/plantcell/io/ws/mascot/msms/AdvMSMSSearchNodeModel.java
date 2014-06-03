package au.edu.unimelb.plantcell.io.ws.mascot.msms;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.io.read.mascot.MascotReaderNodeModel;


/**
 * This node uses input columns for the mascot search parameters, enabling you to 
 * customise, on a per-search basis, the settings used. Each search parameter has a corresponding
 * column in the input table
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class AdvMSMSSearchNodeModel extends MascotReaderNodeModel {
	public final static String CFGKEY_MASCOTEE_URL = "mascotee-url";
	public final static String CFGKEY_USERNAME     = "mascot-username";
	public final static String CFGKEY_EMAIL        = "mascot-email";
	public final static String CFGKEY_TITLE        = "mascot-job-title";
	public final static String CFGKEY_DATABASE     = "mascot-database";
	public final static String CFGKEY_FIXED_MODS   = "mascot-fixed-modifications";
	public final static String CFGKEY_VARIABLE_MODS= "mascot-variable-modifications";
	public final static String CFGKEY_MASS_TYPE     = "mascot-mass-measurement-type";
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
	
	private final SettingsModelString m_url = new SettingsModelString(AdvMSMSSearchNodeModel.CFGKEY_MASCOTEE_URL, "");
	private final SettingsModelDoubleBounded ci = new SettingsModelDoubleBounded(MascotReaderNodeModel.CFGKEY_CONFIDENCE, 
			MascotReaderNodeModel.DEFAULT_CONFIDENCE, 0.0, 1.0);
	private final SettingsModelString m_database          = new SettingsModelString(AdvMSMSSearchNodeModel.CFGKEY_DATABASE, "");
	private final SettingsModelString m_enzyme            = new SettingsModelString(AdvMSMSSearchNodeModel.CFGKEY_ENZYME, "");
	private final SettingsModelString m_fixed_mods        = new SettingsModelString(AdvMSMSSearchNodeModel.CFGKEY_FIXED_MODS, "");
	private final SettingsModelString m_variable_mods     = new SettingsModelString(AdvMSMSSearchNodeModel.CFGKEY_VARIABLE_MODS, "");
	private final SettingsModelString m_mass_type         = new SettingsModelString(AdvMSMSSearchNodeModel.CFGKEY_MASS_TYPE, "");
	private final SettingsModelString m_instrument        = new SettingsModelString(AdvMSMSSearchNodeModel.CFGKEY_INSTRUMENT, "");
	private final SettingsModelString m_taxon             = new SettingsModelString(AdvMSMSSearchNodeModel.CFGKEY_TAXONOMY, "");
	private final SettingsModelString m_top_hits          = new SettingsModelString(AdvMSMSSearchNodeModel.CFGKEY_REPORT_TOP, "");
	private final SettingsModelString m_peptide_charge    = new SettingsModelString(AdvMSMSSearchNodeModel.CFGKEY_PEPTIDE_CHARGE, "");
	private final SettingsModelString m_allowed_prot_mass = new SettingsModelString(AdvMSMSSearchNodeModel.CFGKEY_ALLOWED_PROTEIN_MASS, "");
	
	public AdvMSMSSearchNodeModel() {
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
    
    	
    	return super.execute(inData, exec);
    }
    
    @Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
    	super.saveSettingsTo(settings);
    	m_url.saveSettingsTo(settings);
    	m_database.saveSettingsTo(settings);
    	m_enzyme.saveSettingsTo(settings);
    	m_fixed_mods.saveSettingsTo(settings);
    	m_variable_mods.saveSettingsTo(settings);
    	m_mass_type.saveSettingsTo(settings);
    	m_instrument.saveSettingsTo(settings);
    	m_taxon.saveSettingsTo(settings);
    	m_top_hits.saveSettingsTo(settings);
    	m_peptide_charge.saveSettingsTo(settings);
    	m_allowed_prot_mass.saveSettingsTo(settings);
    }
    
    @Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
    	super.validateSettings(settings);
    	m_url.validateSettings(settings);
    	m_database.validateSettings(settings);
    	m_enzyme.validateSettings(settings);
    	m_fixed_mods.validateSettings(settings);
    	m_variable_mods.validateSettings(settings);
    	m_mass_type.validateSettings(settings);
    	m_instrument.validateSettings(settings);
    	m_taxon.validateSettings(settings);
    	m_top_hits.validateSettings(settings);
    	m_peptide_charge.validateSettings(settings);
    	m_allowed_prot_mass.validateSettings(settings);
    }
    
    @Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
    	super.loadValidatedSettingsFrom(settings);
    	m_url.loadSettingsFrom(settings);
    	m_database.loadSettingsFrom(settings);
    	m_enzyme.loadSettingsFrom(settings);
    	m_fixed_mods.loadSettingsFrom(settings);
    	m_variable_mods.loadSettingsFrom(settings);
    	m_mass_type.loadSettingsFrom(settings);
    	m_instrument.loadSettingsFrom(settings);
    	m_taxon.loadSettingsFrom(settings);
    	m_top_hits.loadSettingsFrom(settings);
    	m_peptide_charge.loadSettingsFrom(settings);
    	m_allowed_prot_mass.loadSettingsFrom(settings);
    }
}
