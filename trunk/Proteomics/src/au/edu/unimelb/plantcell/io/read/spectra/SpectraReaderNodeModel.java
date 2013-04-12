package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
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
public class SpectraReaderNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Spectra Reader");
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
    static final String CFGKEY_FILES= "spectra-folder";
    static final String CFGKEY_LOAD_SPECTRA= "load-spectra";
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
    private final static int NUM_FILE_COLS = 9;

    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels".
    private final SettingsModelStringArray m_files=new SettingsModelStringArray(CFGKEY_FILES, new String[] { "c:/temp/crap.mgf" });
    private final SettingsModelBoolean m_spectra= new SettingsModelBoolean(CFGKEY_LOAD_SPECTRA, true);
    private final SettingsModelBoolean m_mzml = new SettingsModelBoolean(CFGKEY_MZML, DEFAULT_MZML);
    private final SettingsModelBoolean m_mzxml= new SettingsModelBoolean(CFGKEY_MZXML, DEFAULT_MZXML);
    private final SettingsModelBoolean m_mgf  = new SettingsModelBoolean(CFGKEY_MGF, DEFAULT_MGF);
    private final SettingsModelBoolean m_dta  = new SettingsModelBoolean(CFGKEY_DTA, DEFAULT_DTA);
    
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
        
        // make output specs and output containers
        DataTableSpec[] outSpecs = make_output_spec();
        MyDataContainer container = new MyDataContainer(exec.createDataContainer(outSpecs[0]), "Scan");
        MyDataContainer file_container = new MyDataContainer(exec.createDataContainer(outSpecs[1]), "File");
       
        // NB: here we dont check with the readers for each filename (maybe take too long with a large number of readers...)
        //     instead, we just hardcode what is supported
        int done = 0;
        
        // instantiate the data processor's for each desired filetype
        ArrayList<AbstractDataProcessor> dp_list = new ArrayList<AbstractDataProcessor>();
        if (m_mzml.getBooleanValue())
        	dp_list.add(new mzMLDataProcessor(logger));
        if (m_mgf.getBooleanValue())
        	dp_list.add(new MGFDataProcessor());
        if (m_mzxml.getBooleanValue())
        	dp_list.add(new mzXMLDataProcessor(logger));
        if (m_dta.getBooleanValue()) 
        	dp_list.add(new DTADataProcessor());
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

   
    
    private DataTableSpec[] make_output_spec() {

        // if user requests it we will add columns for spectra/chromatograms
        int extra = 0;
        if (m_spectra.getBooleanValue()) {
        	extra++;
        }
        
    	DataColumnSpec[] allColSpecs = new DataColumnSpec[NUM_SCAN_COLS+extra];
        allColSpecs[0] = new DataColumnSpecCreator("Scan Type", StringCell.TYPE).createSpec();
        allColSpecs[1] = new DataColumnSpecCreator("Polarity", StringCell.TYPE).createSpec();
        allColSpecs[2] = new DataColumnSpecCreator("Retention Time", StringCell.TYPE).createSpec();
        allColSpecs[3] = new DataColumnSpecCreator("Base Peak Intensity", DoubleCell.TYPE).createSpec();
        allColSpecs[4] = new DataColumnSpecCreator("Base Peak M/Z", DoubleCell.TYPE).createSpec();
        allColSpecs[5] = new DataColumnSpecCreator("Centroided?", IntCell.TYPE).createSpec();
        allColSpecs[6] = new DataColumnSpecCreator("Deisotoped?", IntCell.TYPE).createSpec();
        allColSpecs[7] = new DataColumnSpecCreator("Charge Deconvoluted?", IntCell.TYPE).createSpec();
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
        fileSpecs[8] = new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
        fileSpecs[0] = new DataColumnSpecCreator("Instrument Manufacturer", StringCell.TYPE).createSpec();
        fileSpecs[1] = new DataColumnSpecCreator("Instrument Model", StringCell.TYPE).createSpec();
        fileSpecs[2] = new DataColumnSpecCreator("Instrument Software", StringCell.TYPE).createSpec();
        fileSpecs[3] = new DataColumnSpecCreator("Instrument Operator", StringCell.TYPE).createSpec();
        fileSpecs[4] = new DataColumnSpecCreator("Mass Analyzer", StringCell.TYPE).createSpec();
        fileSpecs[5] = new DataColumnSpecCreator("Ionization", StringCell.TYPE).createSpec();
        fileSpecs[6] = new DataColumnSpecCreator("Detector", StringCell.TYPE).createSpec();
        fileSpecs[7] = new DataColumnSpecCreator("Data Processing", StringCell.TYPE).createSpec();
        
        return new DataTableSpec[] { new DataTableSpec(allColSpecs), new DataTableSpec(fileSpecs) };
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
        return make_output_spec();
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

