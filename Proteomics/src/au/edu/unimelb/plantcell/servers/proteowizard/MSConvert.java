package au.edu.unimelb.plantcell.servers.proteowizard;

import javax.activation.DataHandler;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlMimeType;


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
	 * Submit and convert the supplied XCalibur (.raw) file from Thermo-Finnigan to mzML format. The conversion is
	 * asynchronous: it will take time. So a jobID is returned for the caller to poll until COMPLETED (indicates successful conversion)
	 * 
	 * @param format one of mzML, mzXML or MGF
	 */
	@WebMethod public String convertThermo(@XmlMimeType("application/octet-stream") DataHandler raw_file, 
			long raw_length, String format);
	
	/**
	 * Similar to convertThermo(), but for a single WIFF file conversion (ie. no .scan file is present). Better
	 * to use this method than call <code>convertWiff(wiff, null, ...);</code>
	 */
	@WebMethod public String convertWIFFsingle(@XmlMimeType("application/octet-stream") DataHandler wiff, 
			long wiff_length, String format);
	
	/**
	 * Submit and convert the supplied Applied Biosystems WIFF format (incl. .wiff.scan if appropriate) to the desired format
	 * 
	 * @param wiff .wiff file supplied by user
	 * @Param wiff_scan .wiff.scan file (if null indicates not available)
	 * @param format one of mzML, mzXML or MGF
	 */
	@WebMethod public String convertWIFF(@XmlMimeType("application/octet-stream") DataHandler wiff, 
			@XmlMimeType("application/octet-stream") DataHandler wiff_scan, 
			long wiff_length, long wiff_scan_length,
			String format);
	
	/**
	 * Get job status - one of [QUEUED, RUNNING, FAILED, COMPLETED, NO QUEUE]
	 */
	@WebMethod public String getStatus(String jobID);
	
	/**
	 * Returns the number of result file(s) available, as produced by the conversion. Usually 1, but the
	 * caller should iterate through them all. Always [0..getResultFileCount()-1]. Do not call this method
	 * unless the conversion status is COMPLETED.
	 */
	@WebMethod public int getResultFileCount(String jobID);
	
	/**
	 * Returns the name of the file (including extension) for the client to use
	 */
	@WebMethod public String getResultFilename(String jobID, int file_index);
	
	/**
	 * Returns the size of the converted file (in bytes) for the caller to check that all bytes have been transferred (checksum maybe?).
	 * Returns -1 on error or if the specified file does not exist.
	 */
	@WebMethod public long getResultFilesize(String jobID, int file_index);
	
	/**
	 * Download the converted file, this will be deleted upon successful download. Results are only
	 * available for jobs in the COMPLETED state and will be deleted automatically. It is highly recommended
	 * to download the result for each conversion before beginning the next conversion (in case a new submission causes
	 * the old job to be purged)
	 * 
	 * @param jobID the job to retrieve results for
	 * @param file_index which file (if more than one during the conversion process) to retrieve
	 */
	@WebMethod public @XmlMimeType("application/octet-stream") DataHandler getResultFile(String jobID, int file_index);
	
	/**
	 * Purge all result files for the specified job ID from the server. No results for the specified
	 * job will be available after this call is executed. The server may carry out this directive immediately or
	 * it may not obey it at all. The server purges results entirely at its discretion: this call is a hint that the data
	 * will not be required anymore. Consider it a friendly notification ;-)
	 */
	@WebMethod public void purgeJobFiles(String jobID);
}
