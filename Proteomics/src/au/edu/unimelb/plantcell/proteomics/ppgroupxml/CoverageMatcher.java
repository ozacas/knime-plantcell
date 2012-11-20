package au.edu.unimelb.plantcell.proteomics.ppgroupxml;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * Matches the XML element &lt;COVERAGE&gt; which relates to the protein peptide coverage at three
 * different thresholds of significance: 95%, 50% and 0% (ie. anything is ok)
 * 
 * @author andrew.cassin
 *
 */
public class CoverageMatcher implements ProteinPilotMatcher {
	private final HashMap<String,String> m_attrs = new HashMap<String,String>();
	private ProteinPilotMatcher m_parent = null;
	
	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<ProteinPilotMatcher> scope_stack) throws IOException,
			XMLStreamException, InvalidSettingsException {
		m_attrs.clear();
		m_parent = scope_stack.peek();		// get handle to <PROTEIN> element matcher instance
		for (int i=0; i<parser.getAttributeCount(); i++) {
			String field = parser.getAttributeLocalName(i);
			String val   = parser.getAttributeValue(i);
			if (field.length() > 0 && val.length() > 0) {
				m_attrs.put(field, val);
			}
		}
		
	}

	@Override
	public void summary(NodeLogger logger) {
	}

	@Override
	public boolean hasMinimalMatchData() {
		if (m_attrs.containsKey("coverage") && m_attrs.containsKey("threshold")) {
			return true;
		}
		return false;
	}

	@Override
	public void save(NodeLogger logger, MyDataContainer my_peptides,
			MyDataContainer my_proteins, MyDataContainer my_quant, File xml_file) {
		if (hasMinimalMatchData() && m_parent != null && m_parent instanceof ProteinMatcher) {
			ProteinMatcher pm = (ProteinMatcher) m_parent;
			String threshold = m_attrs.get("threshold");
			pm.setCoverage(threshold, m_attrs.get("coverage"));
		}
	}

}
