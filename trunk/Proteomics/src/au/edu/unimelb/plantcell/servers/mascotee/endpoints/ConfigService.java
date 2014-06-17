package au.edu.unimelb.plantcell.servers.mascotee.endpoints;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.soap.SOAPException;

/**
 * Session Bean implementation class ConfigService
 */
@WebService
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
	 * @param idx must be in the range <code>0..countDatabaseSequenceFiles(dbName)-1]</code>
	 */
	public DataHandler getDatabaseSequenceFile(final String dbName, int idx) throws SOAPException;
	
	/**
	 * Returns REST-style GET url which returns the same data as getDatabaseSequenceFile()
	 */
	public String getDatabaseSequenceURL(final String dbName, int idx) throws SOAPException;
	
	/**
	 * Is the specified database AA or NA? Will throw an exception if neither...
	 * @return true if the database contains protein sequence, false if nucleic acid data
	 * (which mascot will auto-six-frame translate when searching)
	 */
	public boolean isDatabaseAA(final String dbName) throws SOAPException;
	
	/**
	 * Return list of available configuration settings as described in mascot/config/mascot.dat
	 */
	public String[] availableConfigParameters() throws SOAPException;
	
	/**
	 * Return value association with the specified configuration parameter name
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
	
	/**
	 * returns list of available top hits (5, 10, 20, AUTO etc...) which the mascot server supports. Currently
	 * hardcoded, but eventually will work more sensibly. The first option should be considered the default. 
	 * Guaranteed non-empty and non-null if no exception is thrown.
	 */
	public String[] availableTopHits() throws SOAPException;
	
	/**
	 * returns a list of available peptide charges. Currently hardcoded. Guaranteed non-empty and non-null if no exception is thrown.
	 */
	public String[] availablePeptideChargeStates() throws SOAPException;
	
	/**
	 * Returns a detailed specification (text-based) of the chosen enzyme incl. specificity. 
	 * The specified enzyme must be in this mascot configuration or an exception will be thrown.
	 */
	public String getDetailedEnzymeRecord(final String enzyme) throws SOAPException;
	
	/**
	 * Returns a detailed specification (text-based) of the chosen database record. An exception will be thrown if the
	 * specified database is not present in the specified mascot configuration.
	 */
	public String getDetailedDatabaseRecord(final String db) throws SOAPException;
	
	/**
	 * Returns a detailed specification (text-based) of the chosen chemical modification in the current mascot 
	 * configuration. An exception will be thrown if it does not exist.
	 * 
	 * @param mod
	 * @return
	 */
	public String getDetailedModificationRecord(final String mod) throws SOAPException;
	
	/**
	 * Return the administratively configured MascotEE URL. This is configured via XML
	 * directives in your application server, or in the MascotEE .jar file. The URL is
	 * the root of all MascotEE url's (either SOAP or REST-style) and may require
	 * authentication
	 */
	public String getURL() throws SOAPException;
	
	
}
