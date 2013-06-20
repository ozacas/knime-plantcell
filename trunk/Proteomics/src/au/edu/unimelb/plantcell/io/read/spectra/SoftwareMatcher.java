package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * Match a software element and its contents
 * 
 * @author andrew.cassin
 *
 */
public class SoftwareMatcher extends AbstractXMLMatcher {
	private String m_software_name, m_software_id, m_version;
	private String m_accsn, m_ref;
	
	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<AbstractXMLMatcher> scope_stack) throws IOException,
			XMLStreamException, InvalidSettingsException {
		m_software_id = parser.getAttributeValue(null, "id");
		m_version  = parser.getAttributeValue(null, "version");
		if (m_version == null || m_version.length() < 1) 
			m_version = null;
				
	}

	@Override
	public void save(NodeLogger logger, MyDataContainer file_container,
			MyDataContainer scan_container, File xml_file) {
		if (m_software_name != null && m_software_id != null && m_accsn != null && m_ref != null) {
			DataCell[] cells = missing(file_container.getTableSpec());
			cells[0] = new StringCell(xml_file.getAbsolutePath());
			cells[1] = new StringCell("Software");
			String descr = m_software_name +"("+m_software_id+") ";
			if (m_version != null) {
				descr += m_version;
			}
			cells[2] = new StringCell(descr);
			cells[3] = new StringCell(m_accsn);
			//cells[4] = index...cells ;
			file_container.addRow(cells);
		}
	}
	
	@Override
	public void addCVParam(final String value, final String name, final String accession, 
							final String cvRef, final String unitAccsn, final String unitName) {
		m_accsn = accession;
		m_ref   = cvRef;
		m_software_name = name;
		// value is not set to anything so we ignore it for now
	}

}
