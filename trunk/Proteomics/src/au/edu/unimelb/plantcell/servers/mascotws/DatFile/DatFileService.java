package au.edu.unimelb.plantcell.servers.mascotws.DatFile;

import javax.activation.DataHandler;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.soap.SOAPException;
import javax.xml.ws.soap.MTOM;

/**
 * Permits the user to download a Mascot .DAT file
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
@WebService
@MTOM
public interface DatFileService {

	/**
	 * Return the list of files since the specified date (and including the specified date). The date is the date of submission of the job
	 * and not when the job actually completes (since that may be much later)
	 * 
	 * @param since_date a supported date in the format <year [1970..present year]><month of year[01..12]><day of month[1..31]>
	 * @return
	 * @throws SOAPException
	 */
	@WebMethod
	public String[] getDatFilesSince(final String YYYYMMdd) throws SOAPException;
	
	/**
	 * Given an entry as returned from <code>getDatFiles()</code> this method downloads
	 * the data and makes it available to the caller. This method carefully validates
	 * its input and will reject data not given from <code>getDatFiles()</code>
	 * 
	 * @param dat_file
	 * @return
	 * @throws SOAPException
	 */
	@WebMethod
	public @XmlMimeType("application/octet-stream") DataHandler getDatFile(final String dat_file) throws SOAPException;
	
}