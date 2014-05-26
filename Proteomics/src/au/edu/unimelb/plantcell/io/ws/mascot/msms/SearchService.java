package au.edu.unimelb.plantcell.io.ws.mascot.msms;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.soap.SOAPException;
import javax.xml.ws.soap.MTOM;


/**
 * Web service interface for Mascot searches.
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
@WebService
@MTOM
public interface SearchService {

	/**
	 * Perform a search which does not require a data file to be uploaded (eg. some PMF/Sequence Query)
	 * @return job ID of the newly submitted job 
	 * @throws SOAPException an exception if the job cannot be submitted eg. invalid XML
	 */
	public String simpleSearch(final String mascotee_xml) throws SOAPException;
	
	/**
	 * Like simpleSearch() but this mandates that a single file is part of the search
	 * @param query_data	data file (byte stream)
	 * @param mascotee_xml  remainder of mascot search parameters
	 * @return
	 * @throws SOAPException
	 */
	public String search(final String mascotee_xml, final DataHandler query_data) throws SOAPException;
	
	/**
	 * Get status on a current job
	 * @throws SOAPException 
	 */
	public String getStatus(String jobID) throws SOAPException;
	
	/**
	 * Remove all trace of a current job (server is free to ignore)
	 */
	public void purgeJob(String jobID) throws SOAPException;
	
	/**
	 * Get Mascot results (.dat) file
	 */
	public DataHandler getResults(String jobID) throws SOAPException;
	
	/**
	 * Returns the dat file name for the specified job or null if the job failed for some reason.
	 * This webmethod enables the caller to use the same filename as stored on the mascot server for record-keeping purposes.
	 */ 
	public String getResultsDatFile(String jobID) throws SOAPException;
}
