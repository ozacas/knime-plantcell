package au.edu.unimelb.plantcell.io.ws.mascot.msms;

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
	
	
}
