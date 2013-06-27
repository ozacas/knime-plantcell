package au.edu.unimelb.plantcell.servers.proteowizard;

import javax.activation.DataHandler;
import javax.jws.WebMethod;
import javax.jws.WebService;


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
	@WebMethod public String convertThermo(RawFile raw_file, String format);
	
	/**
	 * Submit and convert the supplied Applied Biosystems WIFF format (incl. .wiff.scan if appropriate) to the desired format
	 * 
	 * @param wiff .wiff file supplied by user
	 * @Param wiff_scan .wiff.scan file (if null indicates not available)
	 * @param format one of mzML, mzXML or MGF
	 */
	@WebMethod public String convertWIFF(RawFile wiff, RawFile wiff_scan, String format);
	
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
	 * Download the converted file, this will be deleted upon successful download. Results are only
	 * available for jobs in the COMPLETED state and will be deleted automatically. It is highly recommended
	 * to download the result for each conversion before beginning the next conversion (in case a new submission causes
	 * the old job to be purged)
	 * 
	 * @param jobID the job to retrieve results for
	 * @param file_index which file (if more than one during the conversion process) to retrieve
	 */
	@WebMethod public DataHandler getResultFile(String jobID, int file_index);
}
