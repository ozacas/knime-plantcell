package au.edu.unimelb.plantcell.io.ws.mascot.msms;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPBinding;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.io.read.mascot.MascotReaderNodeModel;
import au.edu.unimelb.plantcell.servers.mascotee.endpoints.SearchService;
import au.edu.unimelb.plantcell.servers.mascotee.jaxb.Constraints;
import au.edu.unimelb.plantcell.servers.mascotee.jaxb.Data;
import au.edu.unimelb.plantcell.servers.mascotee.jaxb.Identification;
import au.edu.unimelb.plantcell.servers.mascotee.jaxb.KeyParameters;
import au.edu.unimelb.plantcell.servers.mascotee.jaxb.MSMSTolerance;
import au.edu.unimelb.plantcell.servers.mascotee.jaxb.MsMsIonSearch;
import au.edu.unimelb.plantcell.servers.mascotee.jaxb.ObjectFactory;
import au.edu.unimelb.plantcell.servers.mascotee.jaxb.PeptideTolerance;
import au.edu.unimelb.plantcell.servers.mascotee.jaxb.Quantitation;
import au.edu.unimelb.plantcell.servers.mascotee.jaxb.Reporting;
import au.edu.unimelb.plantcell.servers.mascotee.jaxb.Search;


/**
 * This node uses input columns for the mascot search parameters, enabling you to 
 * customise, on a per-search basis, the settings used. Each search parameter has a corresponding
 * column in the input table
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class AdvMSMSSearchNodeModel extends MascotReaderNodeModel {
	private final NodeLogger logger = NodeLogger.getLogger("Advanced MS/MS Ion Search");
	
	/*
	 * these are not the same as for MSMSSearchNodeModel, so we dont reuse that code unfortunately
	 */
	public final static String CFGKEY_PEPTIDE_TOLERANCE = "mascot-peptide-tolerance";
	public final static String CFGKEY_MSMS_TOLERANCE = "mascot-msms-tolerance";
	public static final String CFGKEY_MGF_INPUT = "mgf-input-file-column";
	
	private final SettingsModelString m_mgf               = new SettingsModelString(AdvMSMSSearchNodeModel.CFGKEY_MGF_INPUT, "");
	private final SettingsModelString m_url               = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_MASCOTEE_URL, "");
	private final SettingsModelString m_database          = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_DATABASE, "");
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
	private final SettingsModelString m_out_dat           = new SettingsModelString(MSMSSearchNodeModel.CFGKEY_OUT_DAT, "");
	
	
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
    
    	SearchService ss = makeSearchService();
    	if (ss == null)
    		throw new InvalidSettingsException("Cannot connect to MascotEE: "+m_url.getStringValue());
    	int mgf_idx = inData[0].getSpec().findColumnIndex(m_mgf.getStringValue());
    	if (mgf_idx < 0) {
    		throw new InvalidSettingsException("Unknown MGF input data column: "+m_mgf.getStringValue()+" - reconfigure?");
    	}
    	// 1. For the user: we validate all search queries before starting to submit them. In this way they can be sure
    	// their parameters are correct before they go home for the night/weekend
    	logger.info("*** Validating each search before Mascot run.");
    	for (DataRow r : inData[0]) {
    		Search s = makeSearchQuery(r, inData[0].getSpec());
    		logger.info("Validating search settings for row: "+r.getKey().getString());
    		exec.checkCanceled();
    		ss.validateParameters(s);
    	}
    	logger.info("*** Validation completed successfully.");
    	
    	// 2. process each row in turn, creating the search query and running the search to completion
    	List<String> result_files = new ArrayList<String>();
    	int done = 0;
    	int total_rows = inData[0].getRowCount();
    	for (DataRow r : inData[0]) {
    		logger.info("Creating search for row: "+r.getKey().getString());
    		Search s = makeSearchQuery(r, inData[0].getSpec());
    		addDataFile(s, r.getCell(mgf_idx));
    		exec.checkCanceled();
    		logger.info("Running search for row: "+r.getKey().toString());
    		String jobID = ss.validateAndSearch(s);
    		ArrayList<String> job_ids = new ArrayList<String>();
    		job_ids.add(jobID);
    		exec.checkCanceled();
    		List<String> result = new JobCompletionManager(logger).waitForAllJobsCompleted(ss, job_ids);
    		result_files.addAll(result);
    		
    		exec.setProgress(((double)done)/total_rows);
    	}
    	
    	logger.info("Got "+result_files.size()+" mascot .DAT files for processing. Now downloading...");
    	List<File> dat_files = new DatDownloadManager(logger, m_url.getStringValue(), m_out_dat.getStringValue()).downloadDatFiles(result_files);
    	
    	logger.info("Successfully downloaded "+dat_files.size()+" mascot .DAT files");
    	super.setFiles(dat_files);
    	logger.info("Loading mascot .DAT files into node output");
    	
    	return super.execute(inData, exec);
    }
    
    private void addDataFile(final Search s, final DataCell cell) throws InvalidSettingsException, MalformedURLException {
		assert(s != null && cell != null);
		if (cell.isMissing()) {
			throw new InvalidSettingsException("Missing data file for MS/MS Ion Search!");
		}
		Data d = s.getMsMsIonSearch().getData();
		d.setFormat("Mascot generic");
		File f = new File(cell.toString());
		if (!f.exists() || !f.canRead()) {
			throw new InvalidSettingsException("Cannot read MGF file: "+f.getAbsolutePath());
		}
		d.setSuggestedFileName(f.getName());
		d.setFile(new DataHandler(f.toURI().toURL()));
	}

	private Search makeSearchQuery(final DataRow r, final DataTableSpec inSpec) throws InvalidSettingsException {
		assert(r != null);
		
		ObjectFactory   of = new ObjectFactory();
		MsMsIonSearch  mss = of.createMsMsIonSearch();
		
		Identification id = of.createIdentification();
		id.setUsername(finaliseUsername(getValue(r, inSpec, m_username.getStringValue())));
		id.setEmail(finaliseEmail(getValue(r, inSpec, m_email.getStringValue())));
		id.setTitle(finaliseTitle(getValue(r, inSpec, m_job_title.getStringValue())));
		mss.setIdentification(id);
		KeyParameters p = of.createKeyParameters();
		p.setDatabase(finaliseDatabase(getValue(r, inSpec, m_database.getStringValue())));
		p.getFixedMod().addAll(finaliseModifications(getValue(r, inSpec, m_fixed_mods.getStringValue())));
		p.getVariableMod().addAll(finaliseModifications(getValue(r, inSpec, m_variable_mods.getStringValue())));
		
		p.setMassType(finaliseMassType(getValue(r, inSpec, m_mass_type.getStringValue())));
		mss.setParameters(p);
		
		Constraints c = of.createConstraints();
		c.setAllowedTaxa(finaliseTaxonomy(getValue(r, inSpec, m_taxon.getStringValue())));
		c.setEnzyme(finaliseEnzyme(getValue(r, inSpec, m_enzyme.getStringValue())));
		c.setAllowXMissedCleavages(finaliseMissedCleavages(getValue(r, inSpec, m_missed_cleavages.getStringValue())));
		c.setAllowedProteinMass(finaliseAllowedProteinMass(getValue(r, inSpec, m_allowed_prot_mass.getStringValue())));  // all protein masses allowed
		
		PeptideTolerance pt = (PeptideTolerance) finaliseTolerance(new PeptideTolerance(), getValue(r, inSpec, m_peptide_tolerance.getStringValue()));
		MSMSTolerance    mt = (MSMSTolerance) finaliseTolerance(new MSMSTolerance(), getValue(r, inSpec, m_msms_tolerance.getStringValue()));
		
		c.setPeptideCharge(finalisePeptideCharge(getValue(r, inSpec, m_peptide_charge.getStringValue())));
		c.setPeptideTolerance(pt);
		c.setMsmsTolerance(mt);
		mss.setConstraints(c);
		
		Reporting rep = of.createReporting();
		rep.setOverview(finaliseReportOverview(getValue(r, inSpec, m_report_overview.getStringValue())));
		rep.setTop(finaliseReportTop(getValue(r, inSpec, m_report_top.getStringValue())));
		mss.setReporting(rep);
		
		Quantitation q = of.createQuantitation();
		q.setIcat(finaliseQuantICAT(getValue(r, inSpec, m_quant_icat.getStringValue())));
		mss.setQuant(q);
		
		Data d = of.createData();
		d.setFormat("Mascot generic");	// HACK TODO FIXME... currently hardcoded
		d.setInstrument(finaliseInstrument(getValue(r, inSpec, m_instrument.getStringValue())));
		d.setPrecursor(finalisePrecursor(getValue(r, inSpec, m_precursor.getStringValue())));
		
		// the suggested filename and data itself (d.setFile() and d.setSuggestedFileName()) are set for each search much later ...
		// but we set them here so that validation of search parameters does not fail
		d.setFile(new DataHandler(new ByteArrayDataSource(new byte[0], "")));
		d.setSuggestedFileName("temp.mgf");
		mss.setData(d);
		Search s = of.createSearch();
		s.setMsMsIonSearch(mss);
		return s;
	}

	private String finalisePrecursor(final Object o) throws InvalidSettingsException {
		if (o == null) {
			return "";
		}
		if (o instanceof String) {
			return (String) o;
		}
		throw new InvalidSettingsException("Expected string value for precursor");
	}

	private String finaliseInstrument(final Object o) throws InvalidSettingsException {
		if (o == null) {
			logger.warn("Missing instrument: assuming default instrument");
			return "Default";
		}
		if (o instanceof String) {
			return (String) o;
		}
		throw new InvalidSettingsException("Missing instrument: expected string like MALDI-TOF, Default or similar");
	}

	private boolean finaliseQuantICAT(final Object o) throws InvalidSettingsException {
		if (o == null) {
			logger.warn("Missing ICAT quantitation: assuming off");
			return false;
		}
		if (o instanceof Integer) {
			return ((Integer)o).intValue() != 0;
		}
		throw new InvalidSettingsException("Expected ICAT quantitation: zero (off) or non-zero (enabled)");
	}

	private String finaliseReportTop(Object o) throws InvalidSettingsException {
		if (o == null) {
			logger.warn("Got missing value for report top: assuming AUTO");
			return "AUTO";
		}
		if (o instanceof String || o instanceof Integer) {
			return o.toString();
		}
		throw new InvalidSettingsException("Expected either AUTO or a number for report top!");
	}

	private boolean finaliseReportOverview(Object o) throws InvalidSettingsException {
		if (o == null) {
			logger.warn("Missing report overview: assuming off");
			return false;
		}
		if (o instanceof Integer) {
			Integer i = (Integer)o;
			return (i.intValue() != 0);
		}
		throw new InvalidSettingsException("Report overview must be zero (off) or non-zero (enabled)");
	}

	private Object finaliseTolerance(final Object tolerance, final Object value) throws InvalidSettingsException {
		if (tolerance == null) {
			throw new InvalidSettingsException("Programmer error: no tolerance!");
		}
		if (value == null) {
			throw new InvalidSettingsException("Tolerance must be specified eg. 0.15Da");
		}
		if (value instanceof String) {
			Pattern p = Pattern.compile("^([\\d\\.]+)\\s*(.*)$");
			Matcher m = p.matcher((String) value);
			if (m.matches()) {
				if (tolerance instanceof MSMSTolerance) {
					MSMSTolerance msms = (MSMSTolerance) tolerance;
					msms.setValue(m.group(1));
					msms.setUnit(m.group(2));
				} else if (tolerance instanceof PeptideTolerance) {
					PeptideTolerance tol = (PeptideTolerance) tolerance;
					tol.setValue(m.group(1));
					tol.setUnit(m.group(2));
				} else {
					throw new InvalidSettingsException("Programmer error: unknown tolerance!");
				}
				return tolerance;
			}
			// else fallthru....
		}
		throw new InvalidSettingsException("Tolerance must be a string value with unit eg. 0.15Da or 20ppm");
	}

	private String finalisePeptideCharge(final Object o) throws InvalidSettingsException {
		if (o == null) {
			logger.warn("Missing peptide charge: assuming +1 and +2");
			return "+1 and +2";
		}
		if (o instanceof String) {
			return (String) o;
		}
		throw new InvalidSettingsException("Peptide charge must be a string value eg. +1 and +2");
	}

	private String finaliseAllowedProteinMass(final Object o) throws InvalidSettingsException {
		if (o == null) {
			return "";
		}
		if (o instanceof String) {
			return (String) o;
		}
		throw new InvalidSettingsException("String value expected for allowed protein mass!");
	}

	private int finaliseMissedCleavages(final Object o) throws InvalidSettingsException {
		if (o == null) {
			logger.warn("Missed cleavages value missing, assuming 0");
			return 0;
		}
		if (o instanceof Integer) {
			int i = ((Integer)o).intValue();
			return i;
		}
		throw new InvalidSettingsException("Missed cleavages must be a integer value between 0 and 9 inclusive!");
	}

	private String finaliseEnzyme(final Object o) throws InvalidSettingsException {
		if (o == null) {
			logger.warn("Missing enzyme: assuming no-enzyme search");
			return "None";
		}
		if (o instanceof String) {
			return (String)o;
		}
		throw new InvalidSettingsException("Expected string value for enzyme eg. Trypsin");
	}

	private String finaliseTaxonomy(final Object o) throws InvalidSettingsException {
		if (o == null) {
			return "All Entries";
		}
		if (o instanceof String) {
			return (String) o;
		}
		throw new InvalidSettingsException("Taxonomy expected to be string!");
	}

	private String finaliseMassType(final Object o) throws InvalidSettingsException {
		if (o == null) {
			logger.warn("Mass type missing: assuming monoisotopic");
			return "Monoisotopic";
		}
		if (o instanceof String) {
			return (String)o;
		}
		throw new InvalidSettingsException("String expected for mass type: either Monoisotopic or Average");
	}

	private List<String> finaliseModifications(final Object o) throws InvalidSettingsException {
		if (o == null) {
			return new ArrayList<String>();
		}
		if (o instanceof String) {
			ArrayList<String> ret = new ArrayList<String>();
			String[] s = ((String)o).split(",\\s*");
			for (String str : s) {
				ret.add(str.trim());
			}
			return ret;
		}
		throw new InvalidSettingsException("Modifications required to be a comma separated string (or empty/missing if none)");
	}

	private String finaliseDatabase(final Object o) throws InvalidSettingsException {
		if (o == null) {
			throw new InvalidSettingsException("Database is required!");
		}
		if (o instanceof String) {
			return (String) o;
		}
		throw new InvalidSettingsException("Invalid database: must be string data!");
	}

	private String finaliseTitle(final Object o) throws InvalidSettingsException {
		if (o == null)
			return "";
		if (o instanceof String) {
			return (String) o;
		}
		throw new InvalidSettingsException("Invalid email: must be string data!");
	}

	private String finaliseEmail(final Object o) throws InvalidSettingsException {
		if (o == null)
			return "";
		if (o instanceof String) {
			return (String) o;
		}
		throw new InvalidSettingsException("Invalid email: must be string data!");
	}

	private String finaliseUsername(final Object o) throws InvalidSettingsException {
		if (o == null)
			return "";
		if (o instanceof String) {
			return (String) o;
		}
		throw new InvalidSettingsException("Invalid username: must be string data!");
	}

	/**
     * unpacks a KNIME cell an primitive type suitable for the specified column name and value contained in the specified row with all
     * 
     * @param r
     * @param columnName
     * @return null if the value is missing, otherwise a string, Integer, Boolean etc. as appropriate for the column type
     */
	private Object getValue(final DataRow r, final DataTableSpec inSpec, final String columnName) throws InvalidSettingsException {
		if (r == null || columnName == null || columnName.length() < 1) {
			return null;
		}
		if (inSpec == null) {
			throw new InvalidSettingsException("No column types available!");
		}
		int col_idx = inSpec.findColumnIndex(columnName);
		if (col_idx < 0) {
			throw new InvalidSettingsException("No such column: "+columnName);
		}
		DataType dt = inSpec.getColumnSpec(col_idx).getType();
		DataCell dc = r.getCell(col_idx);
		if (dc.isMissing()) {
			return null;
		}
		
		if (dt.isCompatible(StringValue.class)) {
			return dc.toString();
		} else if (dt.isCompatible(IntValue.class)) {
			return Integer.valueOf(((IntValue)dc).getIntValue());
		} else if (dt.isCompatible(BooleanValue.class)) {
			return ((BooleanValue)dc).getBooleanValue() ? Boolean.TRUE : Boolean.FALSE;
		} else {
			throw new InvalidSettingsException("Unsupported data in column: "+columnName);
		}
	}

	private SearchService makeSearchService() throws MalformedURLException {
		String url = m_url.getStringValue();
		if (url.endsWith("/")) {
			url += "SearchService?wsdl";
		}
		Service      srv = Service.create(new URL(url), MSMSSearchNodeModel.SEARCH_NAMESPACE);
		// MTOM will make an attachment greater than 1MB otherwise inline
		SearchService ss = srv.getPort(SearchService.class, new MTOMFeature(1024 * 1024));
		BindingProvider bp = (BindingProvider) ss;
        SOAPBinding binding = (SOAPBinding) bp.getBinding();
        if (!binding.isMTOMEnabled()) {
        	logger.warn("MTOM support is unavailable: may run out of java heap space");
        }
       
		return ss;
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
    	m_report_top.saveSettingsTo(settings);
    	m_peptide_charge.saveSettingsTo(settings);
    	m_allowed_prot_mass.saveSettingsTo(settings);
    	m_username.saveSettingsTo(settings);
    	m_email.saveSettingsTo(settings);
    	m_job_title.saveSettingsTo(settings);
    	m_report_overview.saveSettingsTo(settings);
    	m_report_top.saveSettingsTo(settings);
    	m_quant_icat.saveSettingsTo(settings);
    	m_precursor.saveSettingsTo(settings);
    	m_peptide_tolerance.saveSettingsTo(settings);
    	m_msms_tolerance.saveSettingsTo(settings);
    	m_out_dat.saveSettingsTo(settings);
    	m_mgf.saveSettingsTo(settings);
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
    	m_peptide_charge.validateSettings(settings);
    	m_allowed_prot_mass.validateSettings(settings);
    	m_username.validateSettings(settings);
    	m_email.validateSettings(settings);
    	m_job_title.validateSettings(settings);
    	m_report_overview.validateSettings(settings);
    	m_report_top.validateSettings(settings);
    	m_quant_icat.validateSettings(settings);
    	m_precursor.validateSettings(settings);
    	m_peptide_tolerance.validateSettings(settings);
    	m_msms_tolerance.validateSettings(settings);
    	m_out_dat.validateSettings(settings);
    	m_mgf.validateSettings(settings);
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
    	m_peptide_charge.loadSettingsFrom(settings);
    	m_allowed_prot_mass.loadSettingsFrom(settings);
    	m_username.loadSettingsFrom(settings);
    	m_email.loadSettingsFrom(settings);
    	m_job_title.loadSettingsFrom(settings);
    	m_report_overview.loadSettingsFrom(settings);
    	m_report_top.loadSettingsFrom(settings);
    	m_quant_icat.loadSettingsFrom(settings);
    	m_precursor.loadSettingsFrom(settings);
    	m_peptide_tolerance.loadSettingsFrom(settings);
    	m_msms_tolerance.loadSettingsFrom(settings);
    	m_out_dat.loadSettingsFrom(settings);
    	m_mgf.loadSettingsFrom(settings);
    }
}
