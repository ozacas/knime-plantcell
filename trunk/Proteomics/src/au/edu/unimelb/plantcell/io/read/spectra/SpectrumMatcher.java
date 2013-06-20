package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.expasy.jpl.core.ms.lc.RetentionTime;
import org.expasy.jpl.core.ms.spectrum.peak.Peak;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * Parse a spectrum entry and outputs a spectra cell as well as summary data to both output ports once
 * parsing of an entry is complete.
 * 
 * @author andrew.cassin
 *
 */
public class SpectrumMatcher extends  AbstractXMLMatcher {
	private final HashMap<String,String> m_accsn2name = new HashMap<String,String>();
	private final HashMap<String,String> m_name2value = new HashMap<String,String>();
	private String m_id, m_index;
	@SuppressWarnings("unused")
	private String m_dp_ref;
	private double[] mz;
	private double[] intensity;
	private boolean load_ms1;
	private List<Peak> precursors = new ArrayList<Peak>();
	private SpectrumListener m_sl;
	
	public SpectrumMatcher() {
		this(false);
	}
	
	public SpectrumMatcher(boolean load_ms1) {
		this.load_ms1 = load_ms1;
		this.m_sl = null;
	}
	
	public SpectrumMatcher(SpectrumListener sl) {
		this(false);
		assert(sl != null);
		m_sl = sl;
	}

	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<AbstractXMLMatcher> scope_stack) throws IOException,
			XMLStreamException, InvalidSettingsException {
		m_accsn2name.clear();		// forget previous spectra state
		m_name2value.clear();
		precursors.clear();
		
		mz = null;
		intensity = null;
		m_id    = parser.getAttributeValue(null, "id");
		m_index = parser.getAttributeValue(null, "index");
		m_dp_ref= parser.getAttributeValue(null, "dataProcessingRef");
	}

	public void addPrecursor(Peak p) {
		assert(p != null);
		precursors.add(p);
	}
	
	public void setBinaryData(final BinaryDataType bdt, final double[] val, final String unit) {
		if (bdt.isMZ()) {
			mz = val;
		} else if (bdt.isIntensity()) {
			intensity = val;
		}
	}
	
	@Override
	public boolean hasMinimalMatchData() {
		if (m_id == null || m_index == null || m_id.length() < 1 || m_index.length() < 1)
			return false;
		
		// TODO: should we require particular keys?
		return (m_accsn2name.size() > 0 && m_name2value.size() > 0);
	}
	
	protected DataCell find(String[] items) {
		for (String s : items) {
			if (m_accsn2name.containsKey(s) && m_name2value.containsKey(m_accsn2name.get(s))) {
				return new StringCell(m_name2value.get(m_accsn2name.get(s)));
			} else if (m_name2value.containsKey(s)) {
				return new StringCell(m_name2value.get(s));
			}
		}
		return null;
	}
	
	protected DataCell getTitle() {
		DataCell c = find(new String[] { "filter string", "MS:1000512", "scan title", "spectrum title", "MS:1000796" });
		if (c != null) {
			return c;
		} else if (m_id != null) {
			return new StringCell(m_id);
		} else {
			return DataType.getMissingCell();
		}
	}
	
	protected DataCell getScanType() {
		DataCell has_profile = find(new String[] { "profile spectrum", "MS:1000128" });
		DataCell has_centroid= find(new String[] { "centroid spectrum", "MS:1000127" });
		DataCell is_positive_mode = find(new String[] { "positive scan", "MS:1000130" });
		DataCell is_negative_mode = find(new String[] { "negative scan", "MS:1000129" });
		
		DataCell is_full_scan = find(new String[] { "full scan", "MS:1000498" });
		
		StringBuilder sb = new StringBuilder(1024);
		if (has_profile != null)
			sb.append("profile");
		if (has_centroid != null)
			sb.append("centroid");
		sb.append(" spectrum: ");
		if (is_positive_mode != null)
			sb.append("scan mode: positive ");
		else if (is_negative_mode != null)
			sb.append("scan mode: negative ");
		else 
			sb.append("scan mode: unknown ");
		
		if (is_full_scan != null)
			sb.append(" full ");
		return new StringCell(sb.toString());
	}
	
	protected boolean hasPrecursor() {
		return precursors.size() > 0;
	}
	
	protected DataCell getRT() {
		return find(new String[] { "MS:1000016", "scan start time"});
	}
	
	protected DataCell getMSLevel() {
		return asIntCell(find(new String[] { "ms level", "MS:1000511" }));
	}
	
	protected DataCell getIndex() {
		return new StringCell(m_id);
	}
	
	protected DataCell asIntCell(DataCell c) {
		if (c == null || c.isMissing())
			return DataType.getMissingCell();
		try {
			return new IntCell(Integer.valueOf(c.toString()));
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			return DataType.getMissingCell();
		}
	}
	
	protected DataCell asDoubleCell(DataCell c) {
		if (c == null || c.isMissing())
			return DataType.getMissingCell();
		
		try {
			return new DoubleCell(Double.valueOf(c.toString()));
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			return DataType.getMissingCell();
		}
	}
	
	protected DataCell getTIC() {
		return asDoubleCell(find(new String[] { "total ion current", "MS:1000285"}));
	}
	
	protected DataCell getCentroided() {
		DataCell c = find(new String[] { "centroid spectrum", "MS:1000127" });
		if (c == null)
			return DataType.getMissingCell();
		return BooleanCell.TRUE;
	}
	
	protected DataCell getDeisotoped() {
		// TODO FIXME
		return DataType.getMissingCell();
	}
	
	protected DataCell getBasePeakIntensity() {
		return asDoubleCell(find(new String[] { "base peak intensity", "MS:1000505"}));
	}
	
	protected DataCell getBasePeakMZ() {
		return asDoubleCell(find(new String[] { "base peak m/z", "MS:1000504" }));
	}
	
	protected DataCell getStartMZ() {
		return asDoubleCell(find(new String[] { "scan window lower limit", "MS:1000501" }));
	}
	
	protected DataCell getEndMZ() {
		return asDoubleCell(find(new String[] { "scan window upper limit", "MS:1000500" }));
	}
	
	protected DataCell getLowMZ() {
		return asDoubleCell(find(new String[] { "lowest observed m/z", "MS:1000528" }));
	}
		
	protected DataCell getHighMZ() {
		return asDoubleCell(find(new String[] { "highest observed m/z", "MS:1000527" }));
	}
	
	protected DataCell getPrecursorCharge() {
		if (!hasPrecursor())
			return DataType.getMissingCell();
		return new IntCell(precursors.get(0).getCharge());
	}
	
	protected DataCell getPrecursorIntensity() {
		if (!hasPrecursor())
			return DataType.getMissingCell();
		return new DoubleCell(precursors.get(0).getIntensity());
	}
	
	protected DataCell getPrecursorMZ() {
		if (!hasPrecursor())
			return DataType.getMissingCell();
		return new DoubleCell(precursors.get(0).getMz());
	}
	
	@Override
	public void save(NodeLogger logger, MyDataContainer file_container,
			MyDataContainer scan_container, File xml_file) {
		BasicPeakList bpl = null;
		if (hasMinimalMatchData()) {
			DataCell[] cells = missing(scan_container.getTableSpec());
			cells[0] = getTitle();
			cells[1] = getScanType();
			cells[2] = getRT();
			cells[3] = getBasePeakIntensity();
			cells[4] = getBasePeakMZ();
			cells[5] = getCentroided();
			cells[6] = getDeisotoped();
			cells[8] = getMSLevel();
			cells[9] = getIndex();
			cells[10] = getPrecursorCharge();
			cells[12] = getPrecursorIntensity();
			cells[13] = getPrecursorMZ();
			cells[14] = getTIC();
			cells[17] = getStartMZ();
			cells[18] = getEndMZ();
			cells[19] = getLowMZ();
			cells[20] = getHighMZ();
			cells[21] = new StringCell(xml_file.getAbsolutePath());
			if (cells.length >= 24) {
				int msLevel = -1;
				if (!cells[8].isMissing()) {
					try {
						msLevel = Integer.valueOf(cells[8].toString());
					} catch (NumberFormatException nfe) {
						// be silent
					}
				}
				double pepmass = -1.0d;
				if (!cells[13].isMissing()) {
					try {
						pepmass = Double.valueOf(cells[13].toString());
					} catch (NumberFormatException nfe) {
						// be silent
					}
				}
				if ((load_ms1 || msLevel > 1) && mz != null && intensity != null) {
					// must use the right constructor to ensure the peaks are sorted by increasing m/z which may not be the case
					// from the input data: public BasicPeakList(double pepmass, int charge, String title, int msLevel, double[] mz, double[] intensity)
					bpl = new BasicPeakList(pepmass, precursors.get(0).getCharge(), cells[0].toString(), msLevel, mz, intensity);
					if (mz.length != intensity.length) 
						logger.warn("MZ list length not same as intensity length for "+cells[0].toString());
					cells[22] = new IntCell(mz.length);
					cells[23] = SpectraUtilityFactory.createCell(bpl);
				}
			}
			scan_container.addRow(cells);
			
			// invoke the listener methods to provide the various data?
			if (m_sl != null) {
				m_sl.spectra(asInteger(cells[8]), asDouble(cells[2]), asDouble(cells[4]), 
						asDouble(cells[3]), cells[9].toString(), cells[0].toString(), cells[1].toString());
				
				for (Peak p : precursors) {
					// TODO BUG FIXME... reported unit for RT must be in seconds
					RetentionTime rt = p.getRT();
					double rt_val = Double.NaN;
					if (rt != null) {
						rt_val = rt.getValue();
						if (rt.getUnit().equals(RetentionTime.RTUnit.minute))
							rt_val *= 60.0d;
					}
					m_sl.precursor(p.getCharge(), p.getMSLevel(), p.getMz(), p.getIntensity(), rt_val);
				}
				if (bpl != null)
					m_sl.peakList(bpl);
			}
		}
	}
	
	/**
	 * Extracts the <code>int</code> value from the specified KNIME cell: -1 if missing.
	 * @param dc
	 * @return
	 */
	private int asInteger(DataCell dc) {
		if (dc == null || dc.isMissing() || !(dc instanceof IntCell)) {
			return -1;
		}
		return ((IntCell)dc).getIntValue();
	}
	
	/**
	 * Extracts the <code>double</code> value from the specified KNIME cell: 
	 * @param dc
	 * @return returns <code>Double.NaN</code> on unsuitable cell (eg. missing)
	 */
	private double asDouble(DataCell dc) {
		if (dc == null || dc.isMissing() || !(dc instanceof DoubleCell)) {
			return Double.NaN;
		}
		return ((DoubleCell)dc).getDoubleValue();
	}
	
	@Override
	public void addCVParam(String value, String name, String accession, String cvRef, String unitAccession, String unitName) throws Exception {
		if (m_accsn2name.containsKey(accession))
			throw new Exception("Duplicate key for "+accession);
		m_accsn2name.put(accession, name);
		m_name2value.put(name, value);
	}
}
