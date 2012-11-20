package au.edu.unimelb.plantcell.proteomics.ppgroupxml;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * This only reports the number of spectra with iTRAQ quantation, it does nothing with the data for now
 * 
 * @author andrew.cassin
 *
 */
public class iTRAQPeakMatcher implements ProteinPilotMatcher {
	private int cnt = 0;
	private boolean first = true;
	private SpectrumMatcher m_parent;
	private HashMap<String,String> m_attrs = new HashMap<String,String>();
	private String m_itraq_peaks;
	
	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<ProteinPilotMatcher> scope_stack) throws IOException,
			XMLStreamException, InvalidSettingsException {
		if (first) {
			first = false;
			cnt = 0;
		}
		m_attrs.clear();	// ensure clean start
		for (int i=0; i<parser.getAttributeCount(); i++) {
			String field = parser.getAttributeLocalName(i);
			String val   = parser.getAttributeValue(i);
			if (field.length() > 0 && val.length() > 0) {
				if (field.equals("attributes") && !val.startsWith("CENTROID,PEAK AREA,PEAK AREA_ERROR")) {
					l.warn("Cannot understand iTRAQ quant: "+val);
					break;
				}
				m_attrs.put(field, val);
			}
		}
		ProteinPilotMatcher ppm = scope_stack.peek();
		if (ppm instanceof SpectrumMatcher) {
			m_parent = (SpectrumMatcher) ppm;
		}
		m_itraq_peaks = parser.getElementText();
		
		String sz = m_attrs.get("size");
		if (sz != null && !sz.equals("0")) 
			cnt++;
	}

	@Override
	public void summary(NodeLogger logger) {
		logger.info("Found "+cnt+" MS/MS spectra with iTRAQ quantitation");
		cnt = 0;		  // reset for next file
		m_parent = null;
		m_attrs.clear();
	}

	@Override
	public boolean hasMinimalMatchData() {
		return true;
	}

	@Override
	public void save(NodeLogger logger, MyDataContainer my_peptides,
			MyDataContainer my_proteins, MyDataContainer my_quant, File xml_file) {
		String[] lines = m_itraq_peaks.split("[\\r\\n]+");
		
		for (String line : lines) {
			String[] fields = line.trim().split("\\s+");
			if (fields.length < 3) 
				continue;
			DataCell[] cells = new DataCell[4];
			cells[0] = m_parent.getIDCell();
			cells[1] = new DoubleCell(Double.parseDouble(fields[0]));
			cells[2] = new DoubleCell(Double.parseDouble(fields[1]));
			cells[3] = new DoubleCell(Double.parseDouble(fields[2]));
			my_quant.addRow(cells);
		}
	}

}
