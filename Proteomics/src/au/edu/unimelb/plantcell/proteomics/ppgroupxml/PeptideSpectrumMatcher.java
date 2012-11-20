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
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * Matches a &lt;MATCH&gt; element and reports the peptide identification via <code>save()</code>
 * 
 * @author andrew.cassin
 *
 */
public class PeptideSpectrumMatcher implements ProteinPilotMatcher {
	private int cnt = 0;
	private int failed = 0;
	private PSMMap m_psm;
	
	/**
	 * this implementation only retains the state for the current <MATCH> being processed, it has no
	 * memory of previous matches
	 */
	private final Map<String,String> m_attrs = new HashMap<String,String>();
	
	/*
	 * parent spectra
	 */
	private SpectrumMatcher m_parent;
	
	public PeptideSpectrumMatcher(PSMMap psm) {
		assert(psm != null);
		m_psm = psm;
	}

	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<ProteinPilotMatcher> scope_stack) throws IOException,
			XMLStreamException {
		
		m_attrs.clear();	// ensure clean start
		for (int i=0; i<parser.getAttributeCount(); i++) {
			String field = parser.getAttributeLocalName(i);
			String val   = parser.getAttributeValue(i);
			if (field.length() > 0 && val.length() > 0) {
				m_attrs.put(field, val);
			}
			//logger.info(field+" = "+val);
		}
		m_parent = (SpectrumMatcher) scope_stack.peek();
		cnt++;
	}

	@Override
	public void summary(NodeLogger logger) {
		logger.info("Processed "+cnt+" PSMs (peptide->spectra matches)");
		if (failed > 0) {
			logger.warn("Some "+failed+" peptide identifications were missing key data (not reported)");
		}
		cnt = 0;		// reset for next file
		failed = 0;
	}

	@Override
	public boolean hasMinimalMatchData() {
		// to be considered legitimate 
		if (m_parent.hasMinimalMatchData() && m_attrs.containsKey("charge") &&
				m_attrs.containsKey("confidence") && m_attrs.containsKey("mz")) {
			return true;
		}
		return false;
	}

	@Override
	public void save(NodeLogger logger, MyDataContainer my_peptides,
			MyDataContainer my_proteins, MyDataContainer my_quant, File xml_file) {
		if (hasMinimalMatchData()) {
			DataCell[] cells = new DataCell[my_peptides.getTableSpec().getNumColumns()];
			for (int i=0; i<cells.length; i++) {
				cells[i] = DataType.getMissingCell();
			}
			
			// spectrum data from parent
			cells[0]  = m_parent.getIDCell();
			cells[1]  = m_parent.getElutionCell();
			cells[2]  = m_parent.getPrecursorCell();
			cells[3]  = getChargeCell();
			cells[4]  = getDeltaCell();
			cells[5]  = getEvalueCell();
			cells[6]  = getModifiedPeptideCell();
			cells[7]  = getPeptideSequenceCell();
			cells[8]  = getConfidenceCell();
			cells[9]  = getMZCell();
			cells[10] = getScoreCell();
			cells[11] = new StringCell(xml_file.getAbsolutePath());
			cells[12] = getTypeCell();
			
			// peptide match attributes from this
			my_peptides.addRow(cells);
			
			m_psm.add(m_attrs.get("id"), my_peptides.lastRowID());
		} else {
			logger.warn("Peptide match did not meet minimal data reporting standards -- ignored");
			failed++;
		}
	}

	private DataCell getTypeCell() {
		String type = m_attrs.get("type");
		if (type == null)
			return DataType.getMissingCell();
		return new IntCell(Integer.valueOf(type));
	}
	
	private DataCell getScoreCell() {
		String score = m_attrs.get("score");
		if (score == null)
			return DataType.getMissingCell();
		return new DoubleCell(Double.valueOf(score));
	}

	private DataCell getMZCell() {
		String mz = m_attrs.get("mz");
		if (mz == null)
			return DataType.getMissingCell();
		return new DoubleCell(Double.valueOf(mz));
	}

	private DataCell getPeptideSequenceCell() {
		String pep = m_attrs.get("seq");
		if (pep == null)
			return DataType.getMissingCell();
		return new StringCell(pep);
	}

	private DataCell getModifiedPeptideCell() {
		String ht = m_attrs.get("ht");
		if (ht == null)
			return DataType.getMissingCell();
		return new StringCell(ht);
	}
	
	private DataCell getEvalueCell() {
		String e = m_attrs.get("eval");
		if (e == null)
			return DataType.getMissingCell();
		return new DoubleCell(Double.parseDouble(e));
	}

	private DataCell getDeltaCell() {
		String da = m_attrs.get("da_delta");
		if (da == null)
			return DataType.getMissingCell();
		return new DoubleCell(Double.parseDouble(da));
	}

	private DataCell getConfidenceCell() {
		String conf = m_attrs.get("confidence");
		if (conf == null)
			return DataType.getMissingCell();
		return new DoubleCell(Double.parseDouble(conf));
	}

	private DataCell getChargeCell() {
		String charge = m_attrs.get("charge");
		if (charge == null)
			return DataType.getMissingCell();
		return new IntCell(new Integer(charge));
	}

}
