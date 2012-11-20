package au.edu.unimelb.plantcell.xml.query;

import au.edu.unimelb.plantcell.xml.query.XQueryReporter.QueryResponseFragmentType;

/**
 * An object must implement this interface and then register it with the XQueryReporter.
 * 
 * @author andrew.cassin
 *
 */
public interface XQueryResponseInterface {

	public void callback(QueryResponseFragmentType type, String s);
	
}
