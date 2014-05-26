package au.edu.unimelb.plantcell.servers.mascotee.endpoints;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.soap.SOAPException;
import javax.xml.ws.soap.MTOM;

/**
 * Session Bean implementation class ConfigService
 */
@WebService
@MTOM
public interface ConfigService {

	/**
	 * Return a list of available proteolytic enzymes which may be used in search (as loaded from <mascot root>/config/enzymes)
	 */
	public String[] availableEnzymes() throws SOAPException;
	
	/**
	 * Return a list of the supported data formats by this Mascot instance
	 */
	public String[] availableDataFormats() throws SOAPException;
	
	/**
	 * Return a list of the unique names of available chemical modifications (fixed or variable) which 
	 * are supported by this Mascot instance. Each returned named may have more details returned via the <code>modificationDetails()</code>
	 * method below
	 */
	public String[] availableModifications() throws SOAPException;
	
	/**
	 * Return list of available instruments (as loaded from <mascot root>/config/fragmentation_rules)
	 */
	public String[] availableInstruments() throws SOAPException;
	
    /**
     * Return list of available search databases as described in mascot/config/mascot.dat
     */
	public String[] availableDatabases() throws SOAPException;
	
	/**
	 * Returns the number of sequence files (ie. fasta's) which comprise the mascot database. Usually one, but
	 * might not be if there is a separate file for each chromosome for example
	 */
	public int countDatabaseSequenceFiles(final String dbName) throws SOAPException;
	
	/**
	 * download the specified database, as returned by <code>availableDatabases()</code>. The resulting
	 * data will be FASTA format, unless mascot supports another kind of database. Only the first matching file (if there
	 * are multiple) is returned via this method
	 * @param dbName name of mascot database
	 * @Param idx must be in the range <code>0..countDatabaseSequenceFiles(dbName)-1]</code>
	 */
	public @XmlMimeType("application/octet-stream") DataHandler getDatabaseSequenceFile(final String dbName, int idx) 
			throws SOAPException;
	
	/**
	 * Return list of available configuration settings as described in mascot/config/mascot.dat
	 */
	public String[] availableConfigParameters() throws SOAPException;
	
	/**
	 * Return value association with the specified parameter name
	 * @param name one of the config parameters as returned by <code>availableConfigParameters()</code>
	 */
	public String getParamValue(final String paramName) throws SOAPException;
	
	/**
	 * Return list of fragmentation rules (in english) for the specified instrument
	 * @param instrument one of the strings from <code>availableInstruments()</code>
	 */
	public String[] getFragmentationRulesForInstrument(final String instrument) throws SOAPException;

	/**
	 * Return list of available taxonomy entries (ie. taxa) which this mascot installation supports
	 */
	public String[] availableTaxa() throws SOAPException;
	
}
