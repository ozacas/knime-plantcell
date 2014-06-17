package au.edu.unimelb.plantcell.servers.mascotee.endpoints;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.soap.SOAPException;

/**
 * Permits the user to download a Mascot .DAT file
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
@WebService
public interface DatFileService {

	/**
	 * Return the list of files since the specified date (and including the specified date). The date is the date of submission of the job
	 * and not when the job actually completes (since that may be much later)
	 * 
	 * @param since_date a supported date in the format <year [1970..present year]><month of year[01..12]><day of month[1..31]>
	 * @return
	 * @throws SOAPException
	 */
	public String[] getDatFilesSince(final String YYYYMMdd) throws SOAPException;
	
	/**
	 * Returns the full date of the specified data file eg. given "F003585.dat" this method will 
	 * search the mascot dat file repository from most recent folder to most ancient and return the full dated
	 * dat file which may then be downloaded eg. "20140509/F003585.dat"
	 * 
	 * @return string of the form YYYYMMdd/F\d+.dat or null if no such dat file is available
	 * @throws SOAPException if input parameter is illegal
	 */
	public String getDatedDatFilePath(String dat_file_name) throws SOAPException;
	
	/**
	 * Given an entry as returned from <code>getDatFiles()</code> this method downloads
	 * the data and makes it available to the caller. This method carefully validates
	 * its input and will reject data not given from <code>getDatFiles()</code>
	 * 
	 * @param dat_file
	 * @return
	 * @throws SOAPException
	 */
	public @XmlMimeType("application/octet-stream") DataHandler getDatFile(final String dat_file) throws SOAPException;
	
	/**
	 * Returns a rest-style URL rather than MTOM data
	 * @param dat_file
	 * @return
	 * @throws SOAPException
	 */
	public String getDatFileURL(final String dat_file) throws SOAPException;
}
