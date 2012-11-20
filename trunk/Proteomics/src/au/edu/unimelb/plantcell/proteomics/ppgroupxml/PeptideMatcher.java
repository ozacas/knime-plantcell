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

public class PeptideMatcher implements ProteinPilotMatcher {
	private final PeptideMap m_pm;
	private final HashMap<String,String> m_attrs = new HashMap<String,String>();
	
	public PeptideMatcher(PeptideMap pm) {
		assert(pm != null);
		m_pm = pm;
	}
	
	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<ProteinPilotMatcher> scope_stack) throws IOException,
			XMLStreamException,InvalidSettingsException {
		m_attrs.clear();
		for (int i=0; i<parser.getAttributeCount(); i++) {
			String localName = parser.getAttributeLocalName(i);
			if ("id".equals(localName) || "matches".equals(localName)) {
				m_attrs.put(localName, parser.getAttributeValue(i));
			}
		}
		
		if (hasMinimalMatchData()) {
			m_pm.add(m_attrs.get("id"), m_attrs.get("matches"));
		}
	}

	@Override
	public void summary(NodeLogger logger) {
		logger.info("Found "+m_pm.count()+" reported peptides");
	}

	@Override
	public boolean hasMinimalMatchData() {
		if (m_attrs.containsKey("matches") && m_attrs.containsKey("id")) 
			return true;
		return false;
	}

	@Override
	public void save(NodeLogger logger, MyDataContainer my_peptides,
			MyDataContainer my_proteins, MyDataContainer my_quant, File xml_file) {
		
	}

}
