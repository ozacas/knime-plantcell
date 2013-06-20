package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;

import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;


/**
 * Responsible for matching a <code>&lt;chromatogram&gt;</code> element in the mzML file and children.
 * Saves the data to the internal state and is capable of invoking a <code>SpectrumListener</code> to notify
 * interested parties of the loaded chromatogram.
 * 
 * @author andrew.cassin
 *
 */
public class ChromatogramMatcher extends SpectrumMatcher {		// MUST be derived from SpectrumMatcher so that binary data is given to us!
	private SpectrumListener m_sl;
	private double[] m_rt;
	private double[] m_intensity;
	private String m_title;
	private String m_rt_unit;
	
	public ChromatogramMatcher(SpectrumListener sl) {
		m_sl = sl;
		m_rt = null;
		m_intensity = null;
		m_title = null;
		m_rt_unit = null;
	}
	
	@Override 
	public boolean hasMinimalMatchData() {
		if (!super.hasMinimalMatchData())
			return false;
		
		// but also check that RT and intensity array's are also present
		if (m_rt == null || m_intensity == null || m_title == null)
			return false;
		
		return true;
	}
	
	@Override
	public void setBinaryData(final BinaryDataType bdt, final double[] vec) {
		if (bdt.isTime()) {
			m_rt = vec;
			if ("minute".equals(m_rt_unit)) {
				for (int i=0; i<m_rt.length; i++) {
					m_rt[i] *= 60.0;
				}
			}
		} else if (bdt.isIntensity())
			m_intensity = vec;
		// else do nothing with vector... maybe warn about it?
	}
	
	@Override
	public void addCVParam(final String value, final String name, final String accession, final String cvRef, final String unitAccsn, final String unitName) throws Exception {
		super.addCVParam(value, name, accession, cvRef, unitAccsn, unitName);
		if (name.toLowerCase().indexOf("chromatogram") >= 0) {
			m_title = name;
		} else if (unitName != null && unitName.length() >= 0) {
			m_rt_unit = unitName;
		}
	}
	
	@Override
	public void save(NodeLogger logger, MyDataContainer file_container,
			MyDataContainer scan_container, File xml_file) {
		if (hasMinimalMatchData() && m_sl != null) {
			m_sl.chromatogram(m_title, m_rt, m_intensity);
		}
	}
}
