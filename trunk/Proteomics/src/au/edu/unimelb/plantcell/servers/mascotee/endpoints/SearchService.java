package au.edu.unimelb.plantcell.servers.mascotee.endpoints;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.soap.SOAPException;

import au.edu.unimelb.plantcell.servers.mascotee.jaxb.Search;


/**
 * Web service interface for Mascot searches.
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
@WebService
public interface SearchService {

	/**
	 * Validates the search parameters, but does not perform the search. Will throw if something is wrong.
	 */
	public void validateParameters(final Search mascotee_xml) throws SOAPException;
	
	/**
	 * Like simpleSearch() but this mandates that a single file is part of the search
	 * @param mascotee_search  mascot search parameters (only one MS/MS, PMF or SeqQuery is supported)
	 * @return
	 * @throws SOAPException
	 */
	public String validateAndSearch(final Search mascotee_xml) throws SOAPException;
	
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
	public @XmlMimeType("application/octet-stream") DataHandler getResults(String jobID) throws SOAPException;
	
	/**
	 * Returns the dat file name for the specified job or null if the job failed for some reason.
	 * This webmethod enables the caller to use the same filename as stored on the mascot server for record-keeping purposes.
	 */ 
	public String getResultsDatFile(String jobID) throws SOAPException;
}
