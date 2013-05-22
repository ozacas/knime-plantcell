package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.IOException;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 * This container is quite important: it provides an id if multiple configurations are supported by an instrument. So all
 * acquisitions can be impacted by the ID obtained here
 * 
 * @author andrew.cassin
 *
 */
public class InstrumentConfigurationListMatcher extends AbstractXMLMatcher {
	private String m_ref;
	
	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<AbstractXMLMatcher> scope_stack) throws IOException,
			XMLStreamException, InvalidSettingsException {
		m_ref = parser.getAttributeValue(null, "id");
	}

	/**
	 * Returns true if the instance has an id which should be reported to the user
	 * @return
	 */
	public boolean hasReference() {
		return (m_ref != null && m_ref.length() > 0);
	}
	
	/**
	 * Do not call unless <code>hasReference()</code> is true
	 * @return
	 */
	public String getReference() {
		return m_ref;
	}
}
