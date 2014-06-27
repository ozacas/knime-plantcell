package au.edu.unimelb.plantcell.proteomics.proteowizard.convert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.io.read.spectra.AbstractDataProcessor;
import au.edu.unimelb.plantcell.io.read.spectra.MGFDataProcessor;
import au.edu.unimelb.plantcell.io.read.spectra.SpectraReaderNodeModel;
import au.edu.unimelb.plantcell.io.read.spectra.mzMLDataProcessor;
import au.edu.unimelb.plantcell.io.read.spectra.mzXMLDataProcessor;

/**
 * This is the model implementation of AnalystWiffConverter.
 * Using a JAX-WS web service, this node converts a wiff file (optionally a .wiff.scan file too) to an open-format and then loads it as per Spectra Reader.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AnalystWiffConverterNodeModel extends NodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("WIFF Converter");
  
	static final String CFGKEY_RAWFILES = "raw-files";
	static final String CFGKEY_OUTPUT_FORMAT = "output-format";
	static final String CFGKEY_OUTPUT_FOLDER = "output-folder";
	static final String CFGKEY_OVERWRITE     = "overwrite-existing-files?";
	static final String CFGKEY_ENDPOINT      = "service-endpoint";
	
	private final SettingsModelStringArray m_files = new SettingsModelStringArray(CFGKEY_RAWFILES, new String[] {});
	private final SettingsModelString m_outformat  = new SettingsModelString(CFGKEY_OUTPUT_FORMAT, "mzML");
	private final SettingsModelString m_outfolder  = new SettingsModelString(CFGKEY_OUTPUT_FOLDER, "c:/temp");
	private final SettingsModelBoolean m_overwrite = new SettingsModelBoolean(CFGKEY_OVERWRITE, Boolean.FALSE);
	private final SettingsModelString  m_endpoint  = new SettingsModelString(CFGKEY_ENDPOINT, "http://10.36.10.96:9090/");
	
    /**
     * Constructor for the node model.
     */
    protected AnalystWiffConverterNodeModel() {
        super(0, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	logger.info("Converting "+m_files.getStringArrayValue().length + " WIFF files to "+m_outformat.getStringValue());
    	
    	// 1. convert each file placing result in destination folder. Abort if something looks bad...
    	List<File> entries = new ArrayList<File>();
    	boolean has_mzml = false;
    	boolean has_mzxml= false;
    	boolean has_mgf  = false;
    	for (String s : m_files.getStringArrayValue()) {
    		File input_file = new File(s);
    		if (!input_file.exists() || !input_file.canRead()) 
    			throw new IOException("Cannot read: "+input_file.getName());
    		
    		/**
    		 * Its possible that a single input file may result in multiple (pieces) of output each in separate files.
    		 * So we handle this case.
    		 */
    		Collection<File> outfiles = XCaliburRawConverterNodeModel.convert_and_save(logger, m_endpoint.getStringValue().trim(), 
    				m_overwrite.getBooleanValue(), true,
    				input_file, new File(m_outfolder.getStringValue()), 
    				m_outformat.getStringValue());
    	
    		for (File f : outfiles) {
    	        	if (f.canRead() && f.isFile()) {
    	        		entries.add(f);
    	        		if (f.getName().toLowerCase().endsWith("mzml"))
    	        			has_mzml = true;
    	        		if (f.getName().toLowerCase().endsWith("mzxml") || f.getName().toLowerCase().endsWith("xml"))
    	        			has_mzxml = true;
    	        		if (f.getName().toLowerCase().endsWith("mgf"))
    	        			has_mgf = true;
    	        	}
    	    }
    	}
    	
    	// 2. now process the files and load the spectra for the user
        logger.info("Found "+entries.size()+" mzXML/mzML/MGF files to process");
        reset();
        
        // make output specs and output containers
        DataTableSpec[] outSpecs = SpectraReaderNodeModel.make_output_spec(true);
        MyDataContainer container = new MyDataContainer(exec.createDataContainer(outSpecs[0]), "Scan");
        MyDataContainer file_container = new MyDataContainer(exec.createDataContainer(outSpecs[1]), "File");
       
        // NB: here we dont check with the readers for each filename (maybe take too long with a large number of readers...)
        //     instead, we just hardcode what is supported
        int done = 0;
        
        // instantiate the data processor's for each desired filetype
        ArrayList<AbstractDataProcessor> dp_list = new ArrayList<AbstractDataProcessor>();
        if (has_mzml)
        	dp_list.add(new mzMLDataProcessor(logger, null, false));
        if (has_mgf)
        	dp_list.add(new MGFDataProcessor(logger));
        if (has_mzxml)
        	dp_list.add(new mzXMLDataProcessor(logger));
      
        if (dp_list.size() == 0) {
        	throw new InvalidSettingsException("No suitable filetypes found! Conversion successful?");
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
	    				dp.process(true, exec, container, file_container);
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
        
    	// 3. and return the results...
        return new BufferedDataTable[]{container.close(), file_container.close()};
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
        return SpectraReaderNodeModel.make_output_spec(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	 m_files.saveSettingsTo(settings);
         m_outfolder.saveSettingsTo(settings);
         m_outformat.saveSettingsTo(settings);
         m_overwrite.saveSettingsTo(settings);
         m_endpoint.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	 m_files.loadSettingsFrom(settings);
         m_outfolder.loadSettingsFrom(settings);
         m_outformat.loadSettingsFrom(settings);
         m_overwrite.loadSettingsFrom(settings);
         m_endpoint.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	 m_files.validateSettings(settings);
         m_outfolder.validateSettings(settings);
         m_outformat.validateSettings(settings);
         m_overwrite.validateSettings(settings);
         m_endpoint.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO: generated method stub
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO: generated method stub
    }

}

