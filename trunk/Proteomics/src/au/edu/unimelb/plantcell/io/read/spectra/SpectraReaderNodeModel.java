package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jzy3d.maths.Coord3d;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.core.MyDataContainer;


/**
 * This is the model implementation of MzXMLReader.
 * Using the jrap-stax library, this node reads mzXML/mzML
 *
 * @author Andrew Cassin
 */
public class SpectraReaderNodeModel extends NodeModel implements SpectrumListener {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Spectra Reader");
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
    static final String CFGKEY_FILES= "spectra-folder";
    static final String CFGKEY_LOAD_SPECTRA= "load-spectra";
    static final String CFGKEY_LOAD_CHROMATOGRAM = "load-chromatogram";
    static final String CFGKEY_MZML = "load-mzml";			// using jMZML library
    static final String CFGKEY_MZXML= "load-mzxml";			// using JRAP library
    static final String CFGKEY_MGF  = "load-mgf";			// using javaprotlib
    static final String CFGKEY_DTA  = "load-dta";			// also using javaprotlib
    
    /** initial default folder to scan for mzxml */
    static final String DEFAULT_SPECTRA_FOLDER = "c:/temp";
    static final boolean DEFAULT_MZML = true;
    static final boolean DEFAULT_MZXML= true;
    static final boolean DEFAULT_MGF  = true;
    static final boolean DEFAULT_DTA  = false;
    
    // number of columns in scan output
    private final static int NUM_SCAN_COLS = 23;
    // number of columns in file summary output
    private final static int NUM_FILE_COLS = 5;

    // persisted node state
    private final SettingsModelStringArray m_files=new SettingsModelStringArray(CFGKEY_FILES, new String[] { "c:/temp/crap.mgf" });
    private final SettingsModelBoolean m_spectra= new SettingsModelBoolean(CFGKEY_LOAD_SPECTRA, true);
    private final SettingsModelBoolean m_chromatogram = new SettingsModelBoolean(CFGKEY_LOAD_CHROMATOGRAM, false);
    private final SettingsModelBoolean m_mzml = new SettingsModelBoolean(CFGKEY_MZML, DEFAULT_MZML);
    private final SettingsModelBoolean m_mzxml= new SettingsModelBoolean(CFGKEY_MZXML, DEFAULT_MZXML);
    private final SettingsModelBoolean m_mgf  = new SettingsModelBoolean(CFGKEY_MGF, DEFAULT_MGF);
    private final SettingsModelBoolean m_dta  = new SettingsModelBoolean(CFGKEY_DTA, DEFAULT_DTA);
    
    // internal model used by views
    private ArrayList<File>     m_loaded_files = new ArrayList<File>(100);
    private List<Coord3d>        m_ms1_surface = new ArrayList<Coord3d>(100 * 1024);
    private HashMap<File,Integer> m_chromatogram_files = new HashMap<File,Integer>();
    private List<Coord3d>        m_chromatogram_surface = new ArrayList<Coord3d>(100 * 1024);
    
    
    /**
     * Constructor for the node model.
     */
    protected SpectraReaderNodeModel() {
        //  two outgoing ports
        super(0, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	List<File> entries = new ArrayList<File>();
        for (String s : m_files.getStringArrayValue()) {
        	File f = new File(s);
        	if (f.canRead() && f.isFile()) {
        		entries.add(f);
        	}
        }
        logger.info("Found "+entries.size()+" mzXML/mzML/MGF files to process");
        reset();
        
        // make output specs and output containers
        DataTableSpec[] outSpecs = make_output_spec(m_spectra.getBooleanValue());
        MyDataContainer container = new MyDataContainer(exec.createDataContainer(outSpecs[0]), "Scan");
        MyDataContainer file_container = new MyDataContainer(exec.createDataContainer(outSpecs[1]), "File");
       
        // NB: here we dont check with the readers for each filename (maybe take too long with a large number of readers...)
        //     instead, we just hardcode what is supported
        int done = 0;
        
        // instantiate the data processor's for each desired filetype
        ArrayList<AbstractDataProcessor> dp_list = new ArrayList<AbstractDataProcessor>();
        if (m_mzml.getBooleanValue())
        	dp_list.add(new mzMLDataProcessor(logger, this, m_chromatogram.getBooleanValue()));
        if (m_mgf.getBooleanValue())
        	dp_list.add(new MGFDataProcessor(logger));
        if (m_mzxml.getBooleanValue())
        	dp_list.add(new mzXMLDataProcessor(logger));
        if (m_dta.getBooleanValue()) 
        	dp_list.add(new DTADataProcessor(logger));
        if (dp_list.size() == 0) {
        	throw new InvalidSettingsException("No filetypes enabled! Re-configure...");
        }
        
        ArrayList<File> filtered_entries = new ArrayList<File>();
        for (File f : entries) {
        	if (! f.isFile()) {
        		continue;
        	}
        	for (AbstractDataProcessor p : dp_list) {
        		if (p.can(f)) {
        			filtered_entries.add(f);
        		}
        	}
        }
        int cnt = filtered_entries.size();
        
        if (cnt > 0) {
        	logger.info("Found "+cnt+" acceptable files for loading.");
        } else {
        	throw new InvalidSettingsException("No files to load - did you enable the right file formats?");
        }
        
       
        /*
         * For each filtered file we try each processor which can process the file in the order
         * constructed above
         */
        for (File f : filtered_entries) {
	        String filename = f.getName();

    		try {
    			logger.info("Processing file: "+filename);
        		exec.checkCanceled();
        		exec.setProgress(((double)done)/cnt, "Processing file "+f.getName());
            	
	    		for (int i=0; i<dp_list.size(); i++) {
	    			AbstractDataProcessor dp = dp_list.get(i);
	    			if (dp.can(f)) {
	    				dp.setInput(f.getAbsolutePath());
	    				dp.process(m_spectra.getBooleanValue(), exec, container, file_container);
	    				dp.finish();
	    				// short-circuit if successfully processed
	    				break;
	    			}
	    		}
    		} catch (CanceledExecutionException ce) {
    			container.close();
    			file_container.close();
    			throw ce;
    		} catch (Exception e) {
    			e.printStackTrace();
    			logger.warn("Unable to process "+filename+ "... skipping! (file ignored)");
    			logger.warn(e);
    		}
	        
	        done++;
	    	exec.setProgress(((double)done)/cnt, "Completed processing file "+f.getName());
        }
        
        // once we are done, we close the container and return its table
        return new BufferedDataTable[]{container.close(),file_container.close()};
    }

   
    /**
     * HACK TODO FIXME: This must be public static so that the raw file converters can access this code. Really
     * should do an design pattern...
     * 
     * @param want_spectra
     * @return
     */
    public static DataTableSpec[] make_output_spec(boolean want_spectra) {

        // if user requests it we will add columns for spectra/chromatograms
        int extra = 0;
        if (want_spectra) {
        	extra++;
        }
        
    	DataColumnSpec[] allColSpecs = new DataColumnSpec[NUM_SCAN_COLS+extra];
        allColSpecs[0] = new DataColumnSpecCreator("Scan Title", StringCell.TYPE).createSpec();
        allColSpecs[1] = new DataColumnSpecCreator("Scan type & Polarity", StringCell.TYPE).createSpec();
        allColSpecs[2] = new DataColumnSpecCreator("Retention Time", StringCell.TYPE).createSpec();
        allColSpecs[3] = new DataColumnSpecCreator("Base Peak Intensity", DoubleCell.TYPE).createSpec();
        allColSpecs[4] = new DataColumnSpecCreator("Base Peak M/Z", DoubleCell.TYPE).createSpec();
        allColSpecs[5] = new DataColumnSpecCreator("Centroided?", BooleanCell.TYPE).createSpec();
        allColSpecs[6] = new DataColumnSpecCreator("Deisotoped?", BooleanCell.TYPE).createSpec();
        allColSpecs[7] = new DataColumnSpecCreator("Charge Deconvoluted?", BooleanCell.TYPE).createSpec();
        allColSpecs[8] = new DataColumnSpecCreator("MS Level (2=MS/MS)", IntCell.TYPE).createSpec();
        allColSpecs[9] = new DataColumnSpecCreator("Scan ID", StringCell.TYPE).createSpec();
        allColSpecs[10] = new DataColumnSpecCreator("Precursor Charge", IntCell.TYPE).createSpec();
        allColSpecs[11] = new DataColumnSpecCreator("Precursor Scan Number", IntCell.TYPE).createSpec();
        allColSpecs[12] = new DataColumnSpecCreator("Precursor Intensity", DoubleCell.TYPE).createSpec();
        allColSpecs[13] = new DataColumnSpecCreator("Precursor M/Z", DoubleCell.TYPE).createSpec();
        allColSpecs[14] = new DataColumnSpecCreator("Total Ion Current", DoubleCell.TYPE).createSpec();
        allColSpecs[15] = new DataColumnSpecCreator("Collision Energy", DoubleCell.TYPE).createSpec();
        allColSpecs[16] = new DataColumnSpecCreator("Ionisation Energy", DoubleCell.TYPE).createSpec();
        allColSpecs[17] = new DataColumnSpecCreator("Start M/Z", DoubleCell.TYPE).createSpec();
        allColSpecs[18] = new DataColumnSpecCreator("End M/Z", DoubleCell.TYPE).createSpec();
        allColSpecs[19] = new DataColumnSpecCreator("Low M/Z", DoubleCell.TYPE).createSpec();
        allColSpecs[20] = new DataColumnSpecCreator("High M/Z", DoubleCell.TYPE).createSpec();
        allColSpecs[21] = new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
        allColSpecs[22] = new DataColumnSpecCreator("Number of peaks", IntCell.TYPE).createSpec();
        if (extra == 1) {
        	allColSpecs[23] = new DataColumnSpecCreator("Spectra", AbstractSpectraCell.TYPE).createSpec();
        }
        
        DataColumnSpec[] fileSpecs =  new DataColumnSpec[NUM_FILE_COLS];
        fileSpecs[0] = new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
        fileSpecs[1] = new DataColumnSpecCreator("Property", StringCell.TYPE).createSpec();
        fileSpecs[2] = new DataColumnSpecCreator("Value", StringCell.TYPE).createSpec();
        fileSpecs[3] = new DataColumnSpecCreator("Accession (controlled vocabulary ID specific to format)", StringCell.TYPE).createSpec();
        fileSpecs[4] = new DataColumnSpecCreator("Index (order for a given property, from 1)", IntCell.TYPE).createSpec();
        
        return new DataTableSpec[] { new DataTableSpec(allColSpecs), new DataTableSpec(fileSpecs) };
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    	 m_loaded_files.clear();
         m_ms1_surface.clear();
         m_chromatogram_files.clear();
         m_chromatogram_surface.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return make_output_spec(m_spectra.getBooleanValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_files.saveSettingsTo(settings);
        m_spectra.saveSettingsTo(settings);
        m_mgf.saveSettingsTo(settings);
        m_mzml.saveSettingsTo(settings);
        m_mzxml.saveSettingsTo(settings);
        m_dta.saveSettingsTo(settings);
        m_chromatogram.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {   
        m_files.loadSettingsFrom(settings);
        m_spectra.loadSettingsFrom(settings);
        m_mgf.loadSettingsFrom(settings);
        m_mzml.loadSettingsFrom(settings);
        if (settings.containsKey(CFGKEY_MZXML)) {		// backward compatibility
        	m_mzxml.loadSettingsFrom(settings);
        } else {
        	m_mzxml.setBooleanValue(DEFAULT_MZXML);
        }
        if (settings.containsKey(CFGKEY_DTA)) {			// backward compatibility
        	m_dta.loadSettingsFrom(settings);
        } else {
        	m_dta.setBooleanValue(DEFAULT_DTA);
        }
        if (settings.containsKey(CFGKEY_LOAD_CHROMATOGRAM)) {	// backward compatibility
        	m_chromatogram.loadSettingsFrom(settings);
        } else {
        	m_chromatogram.setBooleanValue(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_files.validateSettings(settings);
        m_spectra.validateSettings(settings);
        m_mgf.validateSettings(settings);
        m_mzml.validateSettings(settings);
        if (settings.containsKey(CFGKEY_MZXML)) {		// backward compatibility
        	m_mzxml.validateSettings(settings);
        }
        if (settings.containsKey(CFGKEY_DTA)) {			// backward compatibility
        	m_dta.validateSettings(settings);
        }
        if (settings.containsKey(CFGKEY_LOAD_CHROMATOGRAM)) {	// backward compatibility
        	m_chromatogram.validateSettings(settings);
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

    /**************************************************************************
     * Methods for views to use
     **************************************************************************
     */
    
    /**
     * Dont call any other MS1 method unless this returns true
     * @return
     */
    public boolean hasMS1Points() {
    	return (countMS1Files() > 0 && m_ms1_surface != null && m_ms1_surface.size() > 0);
    }
    
    public int countMS1Files() {
    	if (m_loaded_files == null)
    		return 0;
    	return m_loaded_files.size();
    }
    
    public int countMS1Points() {
    	if (m_ms1_surface == null)
    		return 0;
    	return m_ms1_surface.size();
    }
    
    /**
     * Dont call any other Chromatogram method unless this returns true
     */
    public boolean hasChromatogramPoints() {
    	return (countChromatogramFiles() > 0 && m_chromatogram_surface != null && m_chromatogram_surface.size() > 0);
    }
    
    /**
     * Its possible that not all processed data files include chromatograms, so we provide a separate count to MS1 data
     * @return
     */
    public int countChromatogramFiles() {
    	return m_chromatogram_files.keySet().size();
    }
    
    /**
     * Returns the set of points for each chromatogram loaded 
     * @param fractions_with_chromatograms: files with chromatograms (must be presized to contain <code>countChromatogramFiles()</code> elements)
     * @return surface points
     */
    public List<Coord3d> getChromatogramSurface(final File[] fractions_with_chromatograms) {
    	int i=0;
    	for (File f : m_chromatogram_files.keySet()) {
    		fractions_with_chromatograms[i++] = f;
    	}
    	return m_chromatogram_surface;
    }
    
    /**
     * Return the MS1 base peak surface points as governed by (file ie. fraction, base_peak mz, base peak intensity)
     * when reading from the input data
     *  
     * @param fractions		filenames: must be presized to contain <code>countFiles()</code> elements
     * @param x				file IDs (position into fractions array from 0): must be presized to contain <code>countMS1Points()</code> elements
     * @param y				base peak M/Z from acquired spectra: equal to <code>x</code> in size
     * @param z				base peak intensity from acquired spectra: equal to <code>x</code> in size
     */
    public List<Coord3d> getMS1Surface(final File[] fractions) {
    	for (int i=0; i<fractions.length; i++) {
    		fractions[i] = m_loaded_files.get(i);
    	}
    	return m_ms1_surface;
    }
    
    /**************************************************************************
     * These methods are used to populate an internal model, used by the views. 
     * They are only called when loading mzML due to the data not being present
     * or using a controlled vocabulary in other formats
     **************************************************************************
     */
	@Override
	public void newFile(File f) {
		m_loaded_files.add(f);
	}

	@Override
	public void spectra(int msLevel, double rt_in_sec, double base_peak_mz,
			double base_peak_intensity, final String spectra_id, final String title, final String scan_type) {
		if (msLevel == 1 && !Double.isNaN(base_peak_mz) && !Double.isNaN(base_peak_intensity)) {
			Coord3d pt = new Coord3d((float) (m_loaded_files.size()-1), base_peak_mz, base_peak_intensity );
			m_ms1_surface.add(pt);
		}
	}

	@Override
	public void precursor(int charge, int msLevel, double mz, double intensity, double rt) {
		// NO-OP for now
	}

	@Override
	public void peakList(BasicPeakList bpl) {
		// NO-OP for now
	}

	@Override
	public void chromatogram(String title, double[] rt, double[] intensity) {
		if (title != null && title.startsWith("total ion")) {
			int idx = m_chromatogram_files.size();
			m_chromatogram_files.put(m_loaded_files.get(m_loaded_files.size()-1), new Integer(idx));
			for (int i=0; i<rt.length; i++) {
				m_chromatogram_surface.add(new Coord3d((float) idx, rt[i], intensity[i]));
			}
		}
	}

}

