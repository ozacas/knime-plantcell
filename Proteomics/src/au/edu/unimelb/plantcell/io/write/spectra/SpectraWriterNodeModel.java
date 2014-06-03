package au.edu.unimelb.plantcell.io.write.spectra;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.io.read.spectra.BasicSpectraCell;
import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;


/**
 * This is the model implementation of SpectraWriter.
 * Writes a spectra column out to disk for processing with other Mass Spec. software. Supports MGF format but does not guarantee that all input data will be preserved in the created file.
 *
 * @author Andrew Cassin
 */
public class SpectraWriterNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Write Peak List");
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_FILE            = "output-file";
	static final String CFGKEY_OVERWRITE       = "overwrite";
	static final String CFGKEY_FORMAT          = "file-format";
	static final String CFGKEY_COLUMN          = "spectra";
	static final String CFGKEY_FILENAME_SUFFIX = "suffix";
	static final String CFGKEY_SAVE_EMPTY_PEAKLISTS = "save-empty-peaklists?";
	
    private static final String DEFAULT_FILE = "c:/temp/spectra.mgf";
    private static final boolean DEFAULT_OVERWRITE = false;
    private static final String DEFAULT_FORMAT = "Mascot Generic Format";
    private static final String DEFAULT_COLUMN = "Spectra";

    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels".
    private final SettingsModelString m_file       = new SettingsModelString(CFGKEY_FILE, DEFAULT_FILE);
    private final SettingsModelBoolean m_overwrite = new SettingsModelBoolean(CFGKEY_OVERWRITE, DEFAULT_OVERWRITE);
    private final SettingsModelString m_format     = new SettingsModelString(CFGKEY_FORMAT, DEFAULT_FORMAT);
    private final SettingsModelString m_col        = new SettingsModelString(CFGKEY_COLUMN, DEFAULT_COLUMN);
    private final SettingsModelString m_suffix     = new SettingsModelString(CFGKEY_FILENAME_SUFFIX, "");
    private final SettingsModelBoolean m_save_empty= new SettingsModelBoolean(CFGKEY_SAVE_EMPTY_PEAKLISTS, Boolean.FALSE);

    /**
     * Constructor for the node model.
     */
    protected SpectraWriterNodeModel() {
        super(1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	int col_idx = inData[0].getDataTableSpec().findColumnIndex(m_col.getStringValue());
    	if (col_idx < 0) {
    		throw new Exception("Cannot find column: "+m_col.getStringValue()+"... bug?");
    	}
    	RowIterator it = inData[0].iterator();
    	
    	int done = 0;
    	int skipped = 0;
    	int todo = inData[0].getRowCount();
    	
    	HashMap<String,PrintWriter> file_map = new HashMap<String,PrintWriter>();
    	
    	String val = m_suffix.getStringValue();
    	boolean use_suffix = (val != null && val.length() > 0 && !val.equalsIgnoreCase("<none>"));
    	File basename = new File(m_file.getStringValue());
    	int suffix_idx = -1;
    	if (!use_suffix) {
    		file_map.put(basename.getName(), new PrintWriter(new FileWriter(basename)));
    	} else {
    		suffix_idx = inData[0].getSpec().findColumnIndex(m_suffix.getStringValue());
    		if (suffix_idx < 0)
    			throw new InvalidSettingsException("Cannot locate suffix column... aborting! "+m_suffix.getStringValue());
    	}
    	
    	while (it.hasNext()) {
    		DataRow r = it.next();
    		SpectraValue sdi = (SpectraValue) r.getCell(col_idx);
    		
    		// never fails (unless exception thrown) and the writer is ready for use...
    		PrintWriter pw = get_pw(use_suffix, suffix_idx, r, file_map, basename);
    		
    		if (savePeakListasMGF(pw, sdi, new EmptyPeakListHandler(false, logger, 0))) {
    			done++;
    		} else {
    			skipped++;
    		}
    	
    		if (done % 100 == 0) {
    			exec.checkCanceled();
    			exec.setProgress(((double) done)/todo, "Processing spectra "+done);
    		}
    		
    		done++;
    	}
    	
    	// close all files
    	logger.info("Saved spectra to "+file_map.size()+" files.");
    	for (String s : file_map.keySet()) {
    		file_map.get(s).close();
    	}
    	logger.info("Wrote "+done+" spectra, "+skipped+" input spectra were not saved.");
    	
    	// done!
        return new BufferedDataTable[]{};
    }

    /**
     * Ugh... this is an ugly API... need to use a design pattern for this... but unfortunately that requires
     * upgrading everything to mzJava rather than the obsolete javaprotlib. This method is used when code to save a peaklist, so it
     * is used not just in this node.
     * 
     * @param pw  the writer to save to
     * @param sdi the peaklist to be saved
     * @return true if the peaklist is saved, false if it is rejected by the specified plh
     * @throws IOException
     */
    public static boolean savePeakListasMGF(final PrintWriter pw, final SpectraValue sdi, final PeakListHandler plh) throws IOException {
    	if (sdi == null || pw == null) {
    		throw new IOException("Bogus input parameters!");
    	}
		String title = sdi.getID();
		
    	// HACK TODO: get charge and pepmass via SpectraValue interface?
		String charge = "";
		String pepmass= null;
		String scan = null;
		String rt = null;
		if (sdi instanceof BasicSpectraCell) {
			BasicSpectraCell mgf = (BasicSpectraCell) sdi;
			charge = mgf.getCharge();
			pepmass= mgf.getPepmass();
			if (pepmass != null && pepmass.trim().length() == 0)
				pepmass = null;
			scan = mgf.getScan();
			if (scan.equals(""))
				scan = null;
			rt   = mgf.getRT();
			if (rt.equals(""))
				rt = null;
		}
		
		if (plh != null && !plh.accept(sdi)) {
			return false;
		}
		
		// write the spectra to the output file
		pw.println("BEGIN IONS");
		// some mascot implementations require PEPMASS right after the BEGIN IONS so...
		if (pepmass != null)
			pw.println("PEPMASS="+pepmass);
		pw.println("TITLE="+title);
		String term = "";
		if (charge.indexOf("+") < 0) 
			term = "+";
		pw.println("CHARGE="+charge+term);
		if (scan != null) {
			pw.println("SCANS="+scan);
		}
		if (rt != null) {
			pw.println("RTINSECONDS="+rt);
		}
		// any peaks?
		double[] mz = sdi.getMZ();
		double[] intensity = sdi.getIntensity();
		if (mz != null && mz.length > 0) {
    		for (int i=0; i<mz.length; i++) {
    			pw.print(mz[i]);
    			pw.print(' ');
    			pw.println(intensity[i]);
    		}
		}
		
		pw.println("END IONS");
		return true;
	}

	/**
     * Create a print writer which is ready for use based on node configuration and the current row (containing the peak list to be saved).
     * The map is updated to include the PrintWriter for further use if needed
     * 
     * @param use_suffix
     * @param suffix_idx
     * @param r
     * @param file_map
     * @return
     * @throws IOException
     * @throws InvalidSettingsException if an invalid suffix is encountered during execute() or if a file already exists and the node configuration does not permit overwrite
     */
    private PrintWriter get_pw(boolean use_suffix, int suffix_idx, DataRow r,
			HashMap<String, PrintWriter> file_map, File basename) throws InvalidSettingsException, IOException {
		if (!use_suffix) {
			return file_map.get(basename.getName());
		} else {
			String bn = basename.getName();
			Pattern p = Pattern.compile("^(.*)(\\.[a-zA-Z0-9]+)$");
			Matcher m = p.matcher(bn);
			DataCell suffix_cell =  r.getCell(suffix_idx);
			if (suffix_cell == null || suffix_cell.isMissing())
				throw new InvalidSettingsException("Suffix cannot be missing!");
			String suffix_to_use = suffix_cell.toString();
			if (suffix_to_use.indexOf(File.separatorChar) >= 0)
				throw new InvalidSettingsException("Suffix cannot contain: "+File.separatorChar);
			
			File f = null;
			if (m.matches()) {
				String sans_ext = m.group(1);
				f = new File(basename.getParent(), sans_ext+suffix_cell.toString()+m.group(2));
			} else {
				f = new File(basename.getParent(), bn+suffix_cell.toString());
			}
			
			if (file_map.containsKey(f.getName())) {
				return file_map.get(f.getName());
			} else {
				if (f.exists() && !m_overwrite.getBooleanValue())
					throw new InvalidSettingsException("Will not overwrite existing: "+f.getAbsolutePath());
				
				logger.info("Saving spectra to "+f.getAbsolutePath());
				PrintWriter pw = new PrintWriter(new FileWriter(f));
				file_map.put(f.getName(), pw);
				return pw;
			}
		}
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
         return new DataTableSpec[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_col.saveSettingsTo(settings);
    	m_file.saveSettingsTo(settings);
    	m_format.saveSettingsTo(settings);
    	m_overwrite.saveSettingsTo(settings);
    	m_suffix.saveSettingsTo(settings);
    	m_save_empty.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
     	m_col.loadSettingsFrom(settings);
    	m_file.loadSettingsFrom(settings);
    	m_format.loadSettingsFrom(settings);
    	m_overwrite.loadSettingsFrom(settings);
    	
    	// backwards compatibility
    	if (settings.containsKey(CFGKEY_FILENAME_SUFFIX)) {
    		m_suffix.loadSettingsFrom(settings);
    	} else {
    		m_suffix.setStringValue("");
    	}
    	if (settings.containsKey(CFGKEY_SAVE_EMPTY_PEAKLISTS)) {
    		m_save_empty.loadSettingsFrom(settings);
    	} else {
    		m_save_empty.setBooleanValue(Boolean.FALSE);
    	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_col.validateSettings(settings);
    	m_file.validateSettings(settings);
    	m_format.validateSettings(settings);
    	m_overwrite.validateSettings(settings);
    	if (settings.containsKey(CFGKEY_FILENAME_SUFFIX)) {
    		m_suffix.validateSettings(settings);
    	}
    	if (settings.containsKey(CFGKEY_SAVE_EMPTY_PEAKLISTS)) {
    		m_save_empty.validateSettings(settings);
    	}
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

}

