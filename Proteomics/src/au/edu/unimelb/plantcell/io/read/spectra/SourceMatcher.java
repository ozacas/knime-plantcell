package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * Match the description of one of three components typically found in a mass spec: the source of ions
 * 
 * @author andrew.cassin
 *
 */
public class SourceMatcher extends AbstractXMLMatcher {
	private String m_accsn, m_name;
	private String m_order;
	
	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<AbstractXMLMatcher> scope_stack) throws IOException,
			XMLStreamException, InvalidSettingsException {
			m_order = parser.getAttributeValue(null, "order");
	}

	protected String getOrder() {
		return m_order;
	}
	
	protected String getName() {
		return m_name;
	}
	
	protected String getAccession() {
		return m_accsn;
	}
	
	protected String getType() {
		return "Mass Spec. Component";
	}
	
	@Override
	public boolean hasMinimalMatchData() {
		return (m_accsn != null && m_name != null);
	}
	
	@Override
	public void save(NodeLogger logger, MyDataContainer file_container,
			MyDataContainer scan_container, File xml_file) {
		if (hasMinimalMatchData()) {
			DataCell[] cells = missing(file_container.getTableSpec());
			cells[0] = new StringCell(xml_file.getAbsolutePath());
			cells[1] = new StringCell(getType());
			cells[2] = new StringCell(getName());
			cells[3] = new StringCell(getAccession());
			try {
				cells[4] = new IntCell(Integer.valueOf(getOrder()));
			} catch (NumberFormatException nfe) {
				// be silent...
			}
			file_container.addRow(cells);
		}
	}

	@Override
	public void addCVParam(String value, String name, String accession, String cvRef) {
		m_accsn = accession;
		m_name  = name;
	}

}
