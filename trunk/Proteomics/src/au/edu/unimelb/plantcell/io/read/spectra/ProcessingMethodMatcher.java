package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.IOException;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 * Report the data processing method(s) used to create a given dataset
 * 
 * @author andrew.cassin
 *
 */
public class ProcessingMethodMatcher extends SourceMatcher {
	private String m_software_ref;
	
	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<AbstractXMLMatcher> scope_stack) throws IOException,
			XMLStreamException, InvalidSettingsException {
		// handle order attribute
		super.processElement(l, parser, scope_stack);
		// other attributes...
		m_software_ref = parser.getAttributeValue(null, "softwareRef");
	}
	
	@Override
	protected String getName() {
		if (m_software_ref != null && m_software_ref.length() > 0)
			return m_software_ref+": "+super.getName();
		return super.getName();
	}
	
	@Override
	public boolean hasMinimalMatchData() {
		return (super.hasMinimalMatchData() && m_software_ref != null);
	}
	
	@Override
	protected String getType() {
		return "Data Processing method";
	}
}
