package au.edu.unimelb.plantcell.proteomics.proteowizard.convert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPBinding;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.io.read.spectra.AbstractDataProcessor;
import au.edu.unimelb.plantcell.io.read.spectra.MGFDataProcessor;
import au.edu.unimelb.plantcell.io.read.spectra.SpectraReaderNodeModel;
import au.edu.unimelb.plantcell.io.read.spectra.mzMLDataProcessor;
import au.edu.unimelb.plantcell.io.read.spectra.mzXMLDataProcessor;
import au.edu.unimelb.plantcell.proteomics.proteowizard.filter.MSLevelsFilterNodeModel;
import au.edu.unimelb.plantcell.servers.core.jaxb.results.ListOfDataFile;
import au.edu.unimelb.plantcell.servers.msconvertee.endpoints.MSConvert;
import au.edu.unimelb.plantcell.servers.msconvertee.endpoints.ProteowizardJob;



/**
 * This is the model implementation of XCaliburRawConverter.
 * Converts XCalibur(tm) Raw files to open formats: mzML, mzXML or MGF using msconvert invoked via a SOAP webservice
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class XCaliburRawConverterNodeModel extends MSLevelsFilterNodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("RAW Converter");
    
    // rather than import an entire 1.2MB jar... its really a JAXWSProperties key...
    private final static String HTTP_CLIENT_STREAMING_CHUNK_SIZE = "com.sun.xml.ws.transport.http.client.streaming.chunk.size";

	static final String CFGKEY_RAWFILES = "raw-files";
	
	protected final SettingsModelStringArray m_files = new SettingsModelStringArray(CFGKEY_RAWFILES, new String[] {});

    /**
     * Constructor for the node model.
     */
    public XCaliburRawConverterNodeModel() {
         super(0, 2);
    }

    @Override
    protected NodeLogger getNodeLogger() {
    	return logger;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	
    	NodeLogger logger = getNodeLogger();
    	logger.info("Converting "+m_files.getStringArrayValue().length + " raw files to "+getOutputFormat());
    	
    	// 1. convert each file placing result in destination folder. Abort if something looks bad...
    	List<File> entries = new ArrayList<File>();
    	boolean has_mzml = false;
    	boolean has_mzxml= false;
    	boolean has_mgf  = false;
    	int done = 0;
    	for (String s : m_files.getStringArrayValue()) {
    		File input_file = new File(s);
    		if (!input_file.exists() || !input_file.canRead()) 
    			throw new IOException("Cannot read: "+input_file.getName());
    		
    		/**
    		 * Its possible that a single input file may result in multiple (pieces) of output each in separate files.
    		 * So we handle this case.
    		 */
    		Collection<File> outfiles = convert_and_save(logger, getServiceEndpoint(), shouldOverwriteExistingFiles(),
    				false, input_file, getOutputFolder(), getOutputFormat());
    	
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
    		exec.checkCanceled();
    		exec.setProgress((0.5d * ++done) / m_files.getStringArrayValue().length);
    	
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
        done = 0;
        
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
	    	exec.setProgress(0.5d+(0.5d * done)/cnt, "Completed processing file "+f.getName());
        }
        
    	// 3. and return the results...
        return new BufferedDataTable[]{container.close(), file_container.close()};
    }

    /**
     * Invoke the appropriate webservice method depending on the input data. The converted files from the service
     * are downloaded and {@link java.io.File} references are returned. This method will throw if an attempt is made
     * to overwrite an existing file if the configuration does not permit overwriting of existing files.
     * 
     * @param logger		log message instance to use. Must not be null.
     * @param endpoint		web server (eg. tomcat) instance to use. The only instance is currently behind the plantcell.unimelb.edu.au firewall
     * @param overwrite_existing_files if true overwriting of existing files is permitted
     * @param is_wiff		only true if the caller is providing WIFF files rather than XCalibur raw files
     * @param input_file	Wiff/Raw file reference. Must not be null
     * @param output_folder must already exist or the method will fail
     * @param out_format	one of mzML, mzXML or MGF
     * @return
     * @throws Exception
     */
    public Collection<File> convert_and_save(final NodeLogger logger, String endpoint, boolean overwrite_existing_files, 
    		boolean is_wiff, final File input_file, final File output_folder, String out_format) throws Exception {
    	  assert(logger != null && input_file != null && output_folder != null && out_format != null && out_format.length() > 0);
    	  
    	  if (!output_folder.exists()) {
    		  throw new IOException("No such folder or directory: "+output_folder.getAbsolutePath());
    	  }
    	 
          MSConvert msc = makeServiceProxy(endpoint);
          if (msc == null) 
        	  throw new IOException("Cannot create proxy to MSConvert service!");
                    
          // enable MTOM in client
          BindingProvider bp = (BindingProvider) msc;
          Map<String,Object> ctx = bp.getRequestContext();
          ctx.put(HTTP_CLIENT_STREAMING_CHUNK_SIZE, 8192);
          SOAPBinding binding = (SOAPBinding) bp.getBinding();
          binding.setMTOMEnabled(true);
         
          // convert!
          String part = input_file.getName();
          Pattern p = Pattern.compile("^(.*)\\.\\w+$");
          Matcher m = p.matcher(part);
          if (m.matches()) {
        	  part = m.group(1);
          }
          
          String id = null;
    	  ProteowizardJob j = new ProteowizardJob();
    	  j.setOutputFormat(out_format);

          if (is_wiff) {
        	  logger.info("Submitting wiff file: "+input_file.getName());
        	  ArrayList<DataHandler> dh_list = new ArrayList<DataHandler>();
        	  dh_list.add(new DataHandler(new FileDataSource(input_file)));
        	  j.getInputDataNames().add(input_file.getName());
        	  String scan_name = input_file.getName()+".scan";
        	  File tmp = new File(input_file.getParentFile(), scan_name);
        	  j.setInputDataFormat("WIFF");
        	  if (tmp.exists() && tmp.isFile() && tmp.canRead()) {
        		  dh_list.add(new DataHandler(new FileDataSource(tmp)));
        		  logger.info("Submitting "+tmp.getName()+" for conversion too.");
        		  j.getInputDataNames().add(scan_name);
        	  }
        	 
        	  id = msc.convert(j, dh_list.toArray(new DataHandler[0]));
          } else {
        	  logger.info("Submitting raw file: "+input_file.getName());
        	  j.setInputDataFormat("RAW");
        	  j.getInputDataNames().add(input_file.getName());
        	  id = msc.convert(j, new DataHandler[] {new DataHandler(new FileDataSource(input_file))});
          }
          if (id == null)
        	  throw new IOException("Unable to convert raw file - server gave no job ID!");
          logger.info("Got job id: "+id+" for file: "+input_file.getAbsolutePath());
          // if we get here, we can download the results and save them and then make the output table desired
       	  ListOfDataFile results = msc.getResults(id);
       	  if (results == null || results.getDataFile() == null || results.getDataFile().size() < 1) {
       	      throw new IOException("No results from msconvert for job: "+id);
       	  }
    	
       	  List<File> savedFiles = saveResults(results, getOutputFolder());
       	  return savedFiles;
	}

    /**
     * {@inheritDoc}
     */
    @Override
	public DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
    	/**
    	 * HACK TODO FIXME: we should use a design pattern...
    	 */
        return SpectraReaderNodeModel.make_output_spec(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	 super.saveSettingsTo(settings);
         m_files.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	  super.loadValidatedSettingsFrom(settings);
    	  m_files.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	  super.validateSettings(settings);
    	  m_files.validateSettings(settings);
    }
}

