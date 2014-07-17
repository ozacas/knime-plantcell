package au.edu.unimelb.plantcell.proteomics.proteowizard.convert;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.io.read.spectra.AbstractDataProcessor;

/**
 * This is the model implementation of AnalystWiffConverter.
 * Using a JAX-WS web service, this node converts a wiff file (optionally a .wiff.scan file too) to an open-format and then loads it as per Spectra Reader.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AnalystWiffConverterNodeModel extends XCaliburRawConverterNodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("WIFF Converter");
  
    /**
     * Constructor for the node model.
     */
    public AnalystWiffConverterNodeModel() {
        super();
    }

    public NodeLogger getNodeLogger() {
    	return logger;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	getNodeLogger().info("Converting "+m_files.getStringArrayValue().length + " WIFF files to "+getOutputFormat());
    	
    	// 1. convert each file placing result in destination folder. Abort if something looks bad...
    	OutputFiles entries = new OutputFiles();
    	for (String s : m_files.getStringArrayValue()) {
    		File input_file = new File(s);
    		if (!input_file.exists() || !input_file.canRead()) 
    			throw new IOException("Cannot read: "+input_file.getName());
    		
    		/**
    		 * Its possible that a single input file may result in multiple (pieces) of output each in separate files.
    		 * So we handle this case.
    		 */
    		OutputFiles outfiles = convert_and_save(logger, getServiceEndpoint(), 
    				shouldOverwriteExistingFiles(), true,
    				input_file, getOutputFolder(), getOutputFormat());
    		
    		outfiles.filterAndAccumulateTo(entries, new OutputFileFilter() {

    			@Override
				public boolean accept(final File f, final OutputFileFormat format) {
					return (f.canRead() && f.isFile() && format.isSupportedByPlantCell());
				}
    			
    		});
    	}
    	
    	// 2. now process the files and load the spectra for the user
        logger.info("Found "+entries.size()+" mzXML/mzML/MGF files to process");
        reset();
        
        // make output specs and output containers
        DataTableSpec[] outSpecs       = makeOutputTables(null);
        MyDataContainer container      = new MyDataContainer(exec.createDataContainer(outSpecs[0]), "Scan");
        MyDataContainer file_container = new MyDataContainer(exec.createDataContainer(outSpecs[1]), "File");
       
        // NB: here we dont check with the readers for each filename (maybe take too long with a large number of readers...)
        //     instead, we just hardcode what is supported
        int done = 0;
        
        OutputFiles filtered_entries = entries.filterByDataProcessor(logger, OutputFileFormat.supportedFormats());
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
            	
	    		for (AbstractDataProcessor dp : filtered_entries.getDataProcessors(f, logger, OutputFileFormat.supportedFormats())) {
    				dp.setInput(f.getAbsolutePath());
    				dp.process(true, exec, container, file_container);
    				dp.finish();
    				// short-circuit if successfully processed
    				break;
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
    public DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return super.configure(inSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	 super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	 super.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	 super.validateSettings(settings);
    }

}

