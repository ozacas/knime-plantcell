package au.edu.unimelb.plantcell.proteomics.ppgroupxml;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * Methods which are invoked during parsing of a group XML file
 * 
 * @author andrew.cassin
 *
 */
public interface ProteinPilotMatcher {
	/**
	 * Called when the registered start tag for the element is found in the XML input.
	 * Registration of each {@link ProteinPilotMatcher} is performed by the {@link GroupXMLReaderNodeModel}
	 * 
	 * @param parser
	 * @param scope_stack	Used to identify which objects are within element scope at the current position of the parse
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws InvalidSettingsException when the internal XML markup doesn't match what is expected (or a BUG ;-)
	 */
	public void processElement(NodeLogger l, XMLStreamReader parser, Stack<ProteinPilotMatcher> scope_stack)
					throws IOException, XMLStreamException, InvalidSettingsException;
	
	/**
	 * Reports a suitable summary for this object to the specified logger. <code>logger.info</code> is
	 * recommended, but others may be used where appropriate. This method is called after processing the file,
	 * so the caller may reset counters and other data as appropriate for the next file (if any)
	 * 
	 * @param logger
	 */
	public void summary(NodeLogger logger);
	
	/**
	 * Returns true if the current matcher has found minimal information to be reported, otherwise false
	 */
	public boolean hasMinimalMatchData();
	
	/**
	 * persists an object to the output port of the knime node (once parsing of the object is complete).
	 * It is up to implementation to decide if any data should be output. It is an error
	 * to refer to the invoking object after this call completes, to ensure sufficient memory during processing.
	 */
	public void save(NodeLogger logger, 
			MyDataContainer my_peptides, MyDataContainer my_proteins, MyDataContainer my_quant,
			File xml_file);

}
