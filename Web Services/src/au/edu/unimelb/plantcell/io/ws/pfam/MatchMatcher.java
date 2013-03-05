package au.edu.unimelb.plantcell.io.ws.pfam;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

public class MatchMatcher implements XMLMatcher {
		private final HashSet<StringCell> m_accessions = new HashSet<StringCell>();
		private String m_id, m_accsn, m_type, m_class;
		
		@Override
		public void processElement(NodeLogger l, XMLStreamReader parser,
				Stack<XMLMatcher> scope_stack, DataCell[] cells)
				throws IOException, XMLStreamException, InvalidSettingsException {
			m_accsn = parser.getAttributeValue(null, "accession");
			if (m_accsn != null && m_accsn.length() > 0) {
				m_accessions.add(new StringCell(m_accsn));
			}
			m_id = parser.getAttributeValue(null, "id");
			m_type = parser.getAttributeValue(null, "type");
			m_class = parser.getAttributeValue(null, "class");
		}

		public void clear() {
			m_accessions.clear();
		}
		
		public Set<StringCell> getAccessions() {
			return m_accessions;
		}
		
		public String getLastAccession() {
			return (m_accsn != null) ? m_accsn : "";
		}
		
		public String getLastID() {
			return (m_id != null) ? m_id : "";
		}
		
		public String getLastType() {
			return (m_type != null) ? m_type : "";
		}
		
		public String getLastClass() {
			return (m_class != null) ? m_class : "";
		}
		
		@Override
		public void saveText(String string, DataCell[] cells) {
			// NO-OP
		}
}
