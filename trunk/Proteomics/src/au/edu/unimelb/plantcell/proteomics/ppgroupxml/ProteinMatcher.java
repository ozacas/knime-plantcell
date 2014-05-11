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
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;

/**
 * Used to match a <code>&lt;PROTEIN&gt;</code> element from the input XML
 * 
 * @author andrew.cassin
 *
 */
public class ProteinMatcher implements ProteinPilotMatcher {
	private ProteinMap         m_proteins;
	private Map<String,String> m_attrs = new HashMap<String,String>();
	private double m_95, m_50, m_0;
	
	public ProteinMatcher(final ProteinMap prot_map) {
		assert(prot_map != null);
		m_proteins = prot_map;
	}
	
	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<ProteinPilotMatcher> scope_stack) throws IOException,
			XMLStreamException {
		
		m_attrs.clear();	// ensure clean start
		m_95 = Double.NaN;
		m_50 = Double.NaN;
		m_0  = Double.NaN;
		for (int i=0; i<parser.getAttributeCount(); i++) {
			String field = parser.getAttributeLocalName(i);
			String val   = parser.getAttributeValue(i);
			if (field.length() > 0 && val.length() > 0) {
				m_attrs.put(field, val);
			}
			//logger.info(field+" = "+val);
		}
		
		if (hasMinimalMatchData()) {
			m_proteins.add(m_attrs.get("id"), m_attrs.get("sequence"));
		}
	}

	@Override
	public void summary(NodeLogger logger) {
	}

	@Override
	public boolean hasMinimalMatchData() {
		if (m_attrs.containsKey("id") && m_attrs.containsKey("sequence")) {
			return true;
		}
		return false;
	}

	@Override
	public void save(NodeLogger logger, MyDataContainer my_peptides,
			MyDataContainer c_proteins, MyDataContainer my_quant, File xml_file) {
		
		if (hasMinimalMatchData()) {
			DataCell[] cells = new DataCell[c_proteins.getTableSpec().getNumColumns()];
			for (int i=0; i<cells.length; i++) {
				cells[i] = DataType.getMissingCell();
			}
			cells[0] = getNameCell();
			cells[1] = getCoverageCell(m_0);
			cells[2] = getCoverageCell(m_95);
			cells[3] = getCoverageCell(m_50);
			cells[4] = getSequenceCell(cells[0].toString());
			cells[5] = getScoreCell();
			cells[9] = new StringCell(xml_file.getAbsolutePath());
			cells[10]= getUseQuantCell();
			cells[11]= getUseTypeCell();
			c_proteins.addRow(cells);
		}
	}

	// known to exist with PP v4.5
	private DataCell getUseQuantCell() {
		String use_quant = m_attrs.get("use_quant");
		if (use_quant != null) {
			return new StringCell(use_quant);
		}
		return DataType.getMissingCell();
	}
	
	// known to exist with PP v4.5
	private DataCell getUseTypeCell() {
		String use_type = m_attrs.get("use_type");
		if (use_type != null) {
			return new StringCell(use_type);
		}
		return DataType.getMissingCell();
	}

	private DataCell getCoverageCell(double dbl) {
		if (dbl < 0 || Double.isNaN(dbl))
			return DataType.getMissingCell();
		return new DoubleCell(dbl);
	}

	private DataCell getScoreCell() {
		String score = m_attrs.get("protscore");
		if (score == null)
			return DataType.getMissingCell();
		return new DoubleCell(Double.valueOf(score));
	}

	private DataCell getSequenceCell(String id) {
		String seq = m_attrs.get("sequence");
		if (seq == null || seq.length() < 1)
			return DataType.getMissingCell();
		try {
			return new SequenceCell(SequenceType.AA, id, seq);
		} catch (InvalidSettingsException ise) {
			ise.printStackTrace();
			return DataType.getMissingCell();
		}
	}

	private DataCell getNameCell() {
		String name = m_attrs.get("name");
		if (name == null || name.length() < 1)
			return DataType.getMissingCell();
		return new StringCell(name);
	}

	public void setCoverage95(double cov) {
		m_95 = cov;
	}
	
	public void setCoverage50(double cov) {
		m_50 = cov;
	}
	
	public void setCoverage0(double cov) {
		m_0 = cov;
	}

	public void setCoverage(String threshold, String coverage) {
		double val = Double.NaN;
		try {
			val = Double.valueOf(coverage);
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
		}
		if (threshold.equals("0")) {
			setCoverage0(val);
		} else if (threshold.equals("0.5")) {
			setCoverage50(val);
		} else if (threshold.equals("0.95")) {
			setCoverage95(val);
		} else {
			// NO-OP: not supported yet
		}
	}
}
