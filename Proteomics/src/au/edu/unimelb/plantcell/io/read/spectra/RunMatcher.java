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
 * Report key stats from the <code>&lt;run&gt;</code>
 * 
 * @author andrew.cassin
 *
 */
public class RunMatcher extends AbstractXMLMatcher {
	private String m_id, m_config_ref, m_start_time, m_file_ref;
	
	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<AbstractXMLMatcher> scope_stack) throws IOException,
			XMLStreamException, InvalidSettingsException {
		m_id = parser.getAttributeValue(null, "id");
		m_config_ref = parser.getAttributeValue(null, "defaultInstrumentConfigurationRef");
		m_start_time = parser.getAttributeValue(null, "startTimeStamp");
		m_file_ref   = parser.getAttributeValue(null, "defaultSourceFileRef");
	}
	
	@Override
	public boolean hasMinimalMatchData() {
		return (m_id != null && m_config_ref != null);
	}
	
	/**
	 * Guaranteed non-null iff <code>hasMinimalMatchData()</code>
	 * @return
	 */
	protected String getID() {
		return m_id;
	}
	
	@Override
	public void save(NodeLogger logger, MyDataContainer file_container, MyDataContainer scan_container, File xml_file) {
		if (hasMinimalMatchData()) {
			DataCell[] cells = missing(file_container.getTableSpec());
			cells[0] = new StringCell(xml_file.getAbsolutePath());
			cells[1] = new StringCell("Run: "+m_id+ " configuration");
			cells[2] = new StringCell(m_config_ref);
			file_container.addRow(cells);
			if (m_start_time != null) {
				cells[1] = new StringCell("Run: "+m_id+ " start time");
				cells[2] = new StringCell(m_start_time);
				file_container.addRow(cells);
			}
			if (m_file_ref != null) {
				cells[1] = new StringCell("Run: "+m_id+" source file");
				cells[2] = new StringCell(m_file_ref);
				file_container.addRow(cells);
			}
		}
	}
}
