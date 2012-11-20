package au.edu.unimelb.plantcell.proteomics.ppgroupxml;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;

public class SpectrumMatcher implements ProteinPilotMatcher {
	/**
	 * this implementation only retains the state for the current <SPECTRUM> being processed, it has no
	 * memory of previous spectra
	 */
	private final Map<String,String> m_attrs = new HashMap<String,String>();
	private int cnt = 0;
	
	@Override
	public void processElement(NodeLogger logger, XMLStreamReader parser,
			Stack<ProteinPilotMatcher> scope_stack) throws IOException, XMLStreamException {
		m_attrs.clear();	// ensure clean start
		for (int i=0; i<parser.getAttributeCount(); i++) {
			String field = parser.getAttributeLocalName(i);
			String val   = parser.getAttributeValue(i);
			if (field.length() > 0 && val.length() > 0) {
				m_attrs.put(field, val);
			}
			//logger.info(field+" = "+val);
		}
		
		cnt++;
	}

	@Override
	public boolean hasMinimalMatchData() {
		// only report if minimal information is available
		boolean has_id             = m_attrs.containsKey("id");
		boolean has_precursor_mass = m_attrs.containsKey("precursormass");
		
		return (has_id && has_precursor_mass);
	}
	
	@Override
	public void save(NodeLogger logger, MyDataContainer c_peptide, MyDataContainer c_protein, 
			MyDataContainer my_quant, File f) {
	}

	public void summary(NodeLogger logger) {
		logger.info("Processed "+cnt+" MS/MS spectra");
		cnt = 0;		// for next file
	}

	public DataCell getIDCell() {
		// we know there must be a ID attribute since it is required for the spectra to be reported
		return new StringCell(m_attrs.get("id"));
	}

	public DataCell getElutionCell() {
		String el = m_attrs.get("elution");
		if (el == null)
			return DataType.getMissingCell();
		return new DoubleCell(Double.parseDouble(el));
	}

	public DataCell getPrecursorCell() {
		String pc = m_attrs.get("precursormass");
		if (pc == null)
			return DataType.getMissingCell();
		return new DoubleCell(Double.parseDouble(pc));
	}
}
