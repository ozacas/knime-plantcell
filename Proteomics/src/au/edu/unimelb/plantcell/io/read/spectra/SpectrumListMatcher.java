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
 * Report the number of spectra acquired during a given run (not really needed since 
 * the spectra loaded will tell a similar story) but perhaps useful for QC.
 * 
 * @author andrew.cassin
 *
 */
public class SpectrumListMatcher extends AbstractXMLMatcher {
	private String m_count;
	private String m_dp_ref;
	private AbstractXMLMatcher parent;	// should be a RunMatcher instance
	
	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<AbstractXMLMatcher> scope_stack) throws IOException,
			XMLStreamException, InvalidSettingsException {
		m_count = parser.getAttributeValue(null, "count");
		m_dp_ref= parser.getAttributeValue(null, "defaultDataProcessingRef");
		parent = getParent(scope_stack);
	}
	
	@Override
	public boolean hasMinimalMatchData() {
		return (parent != null && m_count != null);
	}
	
	@Override
	public void save(NodeLogger logger, MyDataContainer file_container,
			MyDataContainer scan_container, File xml_file) {
		if (hasMinimalMatchData() && parent instanceof RunMatcher) {
			RunMatcher rm = (RunMatcher) parent;
			DataCell[] cells = missing(file_container.getTableSpec());
			cells[0] = new StringCell(xml_file.getAbsolutePath());
			cells[1] = new StringCell("Run: "+rm.getID()+" spectra count");
			cells[2] = new StringCell(m_count);
			file_container.addRow(cells);
			cells[1] = new StringCell("Run: "+rm.getID()+" data processing method");
			cells[2] = new StringCell(m_dp_ref);
			file_container.addRow(cells);
		}
		
		parent = null;	// keep GC happy
	}
}
