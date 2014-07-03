package au.edu.unimelb.plantcell.servers.msconvertee.endpoints;

import java.util.List;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.soap.SOAPException;


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
	 * otherwise.
	 */
	public void validateJob(final ProteowizardJob job) throws SOAPException;
	
	/**
	 * Submit and convert the supplied XCalibur (.raw) file from Thermo-Finnigan to mzML format. The conversion is
	 * asynchronous: it will take time. So a jobID is returned for the caller to poll until COMPLETED (indicates successful conversion)
	 * 
	 * @param format one of mzML, mzXML or MGF
	 */
	public String convert(final ProteowizardJob job) throws SOAPException; 
	
	/**
	 * Get job status - starts with one of [QUEUED, RUNNING, FAILED, ERROR, COMPLETED] with the rest of the
	 * string possibly describing the nature of the problem.
	 */
	public String getStatus(String jobID) throws SOAPException;
	
	/**
	 * Returns the number of result file(s) available, as produced by the conversion. Usually 1, but the
	 * caller should iterate through them all. Always [0..getResultFileCount()-1]. Do not call this method
	 * unless the conversion status is COMPLETED.
	 */
	public int getResultFileCount(String jobID) throws SOAPException;
	
	/**
	 * Returns the name of the file (including extension) for the client to use
	 */
	public String getResultFilename(String jobID, int file_index) throws SOAPException;
	
	/**
	 * Returns the size of the converted file (in bytes) for the caller to check that all bytes have been transferred (checksum maybe?).
	 * Returns -1 on error or if the specified file does not exist.
	 */
	public long getResultFilesize(String jobID, int file_index) throws SOAPException;
	
	/**
	 * Download the converted file, this will be deleted upon successful download. Results are only
	 * available for jobs in the COMPLETED state and will be deleted automatically. It is highly recommended
	 * to download the result for each conversion before beginning the next conversion (in case a new submission causes
	 * the old job to be purged)
	 * 
	 * @param jobID the job to retrieve results for
	 * @param file_index which file (if more than one during the conversion process) to retrieve
	 */
	public @XmlMimeType("application/octet-stream") DataHandler getResultFile(String jobID, int file_index) throws SOAPException;
	
	/**
	 * Purge all result files for the specified job ID from the server. No results for the specified
	 * job will be available after this call is executed. The server may carry out this directive immediately or
	 * it may not obey it at all. The server purges results entirely at its discretion: this call is a hint that the data
	 * will not be required anymore. Consider it a friendly notification ;-)
	 */
	public void purgeJobFiles(String jobID) throws SOAPException;
}
