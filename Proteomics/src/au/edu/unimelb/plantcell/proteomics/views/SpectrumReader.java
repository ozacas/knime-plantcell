package au.edu.unimelb.plantcell.proteomics.views;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.expasy.jpl.core.ms.spectrum.peak.Peak;
import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.io.read.spectra.BasicPeakList;
import au.edu.unimelb.plantcell.io.read.spectra.SpectrumMatcher;

/**
 * The process of extracting key pieces of information from the mzML file is the responsibility of this class,
 * and classes which handle nested XML elements.
 * 
 * @author andrew.cassin
 *
 * @param <T>
 */
public class SpectrumReader<T extends MassSpecSurfaceNodeModel> extends SpectrumMatcher {
	private double  rt = Double.NaN;
	private int bad_ms1 = 0;
	private int total_ms1 = 0;
	private PeakThresholdFilter peak_filter;
	private final SummaryStatistics accepted_peak_stats = new SummaryStatistics();
	// for keeping track of which MS1 gave birth to a MS2/MS3/... scan
	private final HashMap<String,Double> scan2rt = new HashMap<String,Double>(10000);
	private final HashMap<String,Integer> scan2mslevel = new HashMap<String,Integer>(10000);
	private final HashMap<String,String> scan2parent = new HashMap<String,String>(10000);
	private final HashMap<String,Double> scan2mz = new HashMap<String,Double>(10000);
	private SurfaceMatrixAdapter m_surface;
	private double m_threshold;
	private T      m_mdl;
	private double min_rt, max_rt, min_mz, max_mz;
	
	// constructor
	public SpectrumReader(final SurfaceMatrixAdapter m, final T nodeModel, final PeakThresholdFilter ptf, double peak_intensity_threshold) {
		super(true);		// MUST load MS1 ;-)
		peak_filter = ptf;
		m_threshold = peak_intensity_threshold;
		m_surface   = m;
		m_mdl       = nodeModel;
		min_rt      = Double.POSITIVE_INFINITY;
		max_rt      = Double.NEGATIVE_INFINITY;
		min_mz      = Double.POSITIVE_INFINITY;
		max_mz      = Double.NEGATIVE_INFINITY;
	}
	
	public void logPeakSummary(NodeLogger logger) {
		logger.info("Accepted peaks: "+accepted_peak_stats.getN());
		logger.info("Minimum peak intensity accepted: "+accepted_peak_stats.getMin());
		logger.info("Maximum peak intensity accepted: "+accepted_peak_stats.getMax());
		logger.info("Mean accepted peak intensity: "+accepted_peak_stats.getMean());
		logger.info("SD of accepted peak intensity: "+accepted_peak_stats.getStandardDeviation());
		logger.info("Observed RT range: ["+min_rt+", "+max_rt+"]");
		logger.info("Observed M/Z range: "+min_mz+", "+max_mz+"]");
	}
	
	@Override
	public boolean hasMinimalMatchData() {
		if (!super.hasMinimalMatchData())
			return false;
		
		return (hasPeaks() && !Double.isNaN(rt));
	}
	
	public int getTotalMS1() {
		return total_ms1;
	}
	
	public int getBadMS1() {
		return bad_ms1;
	}
	
	@Override
	public void save(NodeLogger logger, MyDataContainer file_container,
			MyDataContainer scan_container, File xml_file) {
		// always store a scan into the internal state regardless of how much data was recorded...
		scan2mslevel.put(getID(), getMSLevel());
		String parent_id = getParentSpectrumID();
		if (parent_id != null)
			scan2parent.put(getID(), parent_id);
		
		if (getMSLevel() == 1) {
			double val = unbox_cell(getRT());
			scan2rt.put(getID(), new Double(val));
		} else if (getMSLevel() >= 2) {
			double mz = unbox_cell(getPrecursorMZ());
			scan2mz.put(getID(), new Double(mz));
			saveScore();
		}
		
		// NB: do NOT invoke the superclass method as it assumes the containers are non-null
		File mzmlFile = m_surface.getFile();
		if (hasMinimalMatchData() && getMSLevel() == 1) {
			BasicPeakList pbl = makePeakList();
			int rt_bin = m_mdl.getRTBin(mzmlFile, rt);
			if (rt < min_rt)
				min_rt = rt;
			if (rt > max_rt)
				max_rt = rt;
			for (Peak p : pbl.getPeaks()) {
				double mz = p.getMz();
				if (mz < min_mz)
					min_mz = mz;
				if (mz > max_mz)
					max_mz = mz;
				int mz_bin = m_mdl.getMZBin(mzmlFile, mz);
				double intensity = p.getIntensity();
				if (rt_bin >= 0 && mz_bin >= 0 && (peak_filter == null || peak_filter.accept(pbl, p, m_threshold))) {
					
					// increment peak in bin by the measured intensity
					m_surface.set(rt_bin, mz_bin, m_surface.get(rt_bin, mz_bin)+intensity);
					accepted_peak_stats.addValue(intensity);
				}
			}
			total_ms1++;
		} else {
			if (getMSLevel() == 1) {
				bad_ms1++;
				total_ms1++;
			}
			
		}
		
		rt = Double.NaN;
	}
	
	/**
	 * If the node model, as configured by the user, does not want the MSn spectra from the mzML this will be false. Otherwise true.
	 * Code in the reader (or subclass) should respond to this to avoid breaking the contract with the node model.
	 * 
	 * @return
	 */
	protected boolean wantMS2() {
		if (m_mdl == null)			// should not happen
			return false;
		return m_mdl.wantMS2fromMzML();
	}
	
	protected void saveScore() {
		// this implementation does nothing since the getScore() implementation for this class always returns 1
	}

	private double unbox_cell(DataCell dc) {
		if (dc == null || dc.isMissing())
			return Double.NaN;
		
		if (dc.getType().isCompatible(DoubleValue.class)) {
			DoubleValue dv = (DoubleValue) dc;
			return dv.getDoubleValue();
		} else if (dc.getType().isCompatible(StringValue.class)) {
			try {
				return Double.valueOf(((StringValue)dc).getStringValue());
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
				// FALLTHRU
			}
		}
		return Double.NaN;
	}

	@Override
	public void addCVParam(String value, String name, String accession, String cvRef, String unitAccession, String unitName) throws Exception {
		super.addCVParam(value, name, accession, cvRef, unitAccession, unitName);
		
		// get scan start time ie. retention time?
		if (accession.equals("MS:1000016") || name.equals("scan start time")) {
			if (!Double.isNaN(rt))
				throw new InvalidSettingsException("Already seen scan start time!");
			
			rt = Double.valueOf(value);		// must always be calculated in seconds
			if (unitName.trim().equalsIgnoreCase("minute"))
				rt *= 60.0d;
		}
	}
	
	/**********************************
	 * METHODS FOR node model to use
	 **********************************/
	public Collection<String> getMS2Scans() {
		ArrayList<String> ret = new ArrayList<String>();
		if (!wantMS2())
			return ret;
		
		for (String s : scan2mslevel.keySet()) {
			Integer lvl = scan2mslevel.get(s);
			if (lvl != null && lvl.intValue() == 2) {
				ret.add(s);
			}
		}
		return ret;
	}
	
	public String getParent(String scanID) {
		if (scanID == null)
			return null;
		return scan2parent.get(scanID);
	}
	
	public double getMZ(String scanID) {
		Double mz = scan2mz.get(scanID);
		if (mz != null)
			return mz.doubleValue();
		return getMZ(scan2parent.get(scanID));
	}
	
	public double getRetentionTime(String scanID) {
		assert(scanID != null);
		Double rt = scan2rt.get(scanID);
		if (rt != null)
			return rt.doubleValue();
		return getRetentionTime(scan2parent.get(scanID));
	}

	/**
	 * Current implementation returns a constant, but subclasses may override to compute a score for each msN (n>=2) as required
	 * @param ms2
	 * @return
	 */
	public double getMS2Score(final String ms2_scan_id) {
		return 1.0;
	}
}