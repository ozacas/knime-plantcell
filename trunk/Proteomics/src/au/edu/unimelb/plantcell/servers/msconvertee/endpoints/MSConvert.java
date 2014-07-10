package au.edu.unimelb.plantcell.servers.msconvertee.endpoints;

import java.util.List;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.soap.SOAPException;

import au.edu.unimelb.plantcell.servers.core.jaxb.results.ListOfDataFile;


/**
 * Converts a raw Mass Spectrometry dataset via ProteoWizard (http://proteowizard.sourceforge.net) to an open-file format.
 * We use mzML here as it is the most complete standard at the moment. Be the code only supports "common vocabulary" elements
 * and not user-defined stuff.
 * 
 * @author andrew.cassin
 *
 */
@WebService
public interface MSConvert {
	/**
	 * Returns list of all features recognised/supported by MSConvertEE. Features not supported will not be here ;)
	 */
	public MSConvertFeature[] allFeatures() throws SOAPException;
	
	/**
	 * Returns list of supported features
	 * @throws SOAPException 
	 */
	public List<MSConvertFeature> supportedFeatures() throws SOAPException;
	
	/**
	 * Service support all of the specified features?
	 */
	public boolean supportsAllFeatures(final MSConvertFeature[] features) throws SOAPException;
	
	/**
	 * Service support any of the specified features?
	 */
	public boolean supportsAnyFeature(final MSConvertFeature[] features) throws SOAPException;
	
	/**
	 * Validates all parameters of the job and throws an exception if any invalid settings are detected.
	 * Does no conversion. In theory, this should be queued as well as you could launch a denial of service attack
	 * otherwise. Does not validate input data.
	 */
	public void validateJob(final ProteowizardJob job) throws SOAPException;
	
	/**
	 * Submit and convert the supplied data files according to the specification in <code>job</code>. 
	 * The conversion is asynchronous: it will take substantial time. So a jobID is returned for the 
	 * caller to poll until COMPLETED (indicates successful conversion)
	 * 
	 * The caller must supply suitable filenames (incl. extension) via the job parameter in the same order
	 * as the input_data_files list.
	 */
	public String convert(final ProteowizardJob job, final DataHandler[] input_data_files) throws SOAPException; 
	
	/**
	 * Used for debugging/testing the command line argument building for the specified job. It returns the
	 * command line rather than executing the specified job
	 * @throws SOAPException if the job is not valid or a server mis-configuration
	 */
	public String debugConvert(final ProteowizardJob job, final DataHandler[] input_data_files) throws SOAPException;
	
	/**
	 * Get job status - starts with one of [QUEUED, RUNNING, FAILED, ERROR, COMPLETED] with the rest of the
	 * string possibly describing the nature of the problem.
	 */
	public String getStatus(final String jobID) throws SOAPException;
	
	/**
	 * Returns an object representing all the result files, their expected file sizes and suggested names (as created
	 * by msconvert) to help the client to populate its data store(s).
	 * 
	 * @param jobID
	 * @return
	 * @throws SOAPException if the job is not finished or some problem occurs
	 */
	public ListOfDataFile getResults(final String jobID) throws SOAPException;
	
	/**
	 * Purge all result files for the specified job ID from the server. No results for the specified
	 * job will be available after this call is executed. The server may carry out this directive immediately or
	 * it may not obey it at all. The server purges results entirely at its discretion: this call is a hint that the data
	 * will not be required anymore. Consider it a friendly notification ;-)
	 */
	public void purgeJobFiles(final String jobID) throws SOAPException;
}
