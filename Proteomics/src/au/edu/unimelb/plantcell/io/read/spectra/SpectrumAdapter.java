package au.edu.unimelb.plantcell.io.read.spectra;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.expasy.jpl.core.ms.spectrum.PeakList;
import org.expasy.jpl.core.ms.spectrum.PeakListImpl;
import org.expasy.jpl.core.ms.spectrum.peak.Peak;
import org.expasy.jpl.core.ms.spectrum.peak.PeakImpl;
import org.knime.core.node.InvalidSettingsException;

import uk.ac.ebi.jmzml.model.mzml.BinaryDataArray;
import uk.ac.ebi.jmzml.model.mzml.CVParam;
import uk.ac.ebi.jmzml.model.mzml.Spectrum;

/**
 * Adapter around class {@link Spectrum} (part of jmzml) to provide more convenient methods
 * 
 *   Constructs a BPL from a jMZML {@link Spectrum} instance. Credit to the author of javaprotlib's MZMLReader for 
 *   providing this code, refer to {@link http://javaprotlib.sourceforge.net/javadoc/jpl-io/rel1.0/src-html/org/expasy/jpl/io/ms/reader/MZMLReader.html}
 *   and to {@link http://proteo-suite.googlecode.com/svn-history/r93/trunk/src/main/java/org/proteosuite/utils/MzML2MGF.java}
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class SpectrumAdapter {
	private PeakList pl;
	private double rt;
	private int pc;
	private int mslevel;	// MS1 or MS2 or MS3 or ... ?
	private double pmz;		// parent ion m/z
	private double pi;		// parent ion intensity
	private boolean m_scan_polarity; 	// true denotes positive scan
	private double  m_base_peak_mz, m_base_peak_intensity;
	private double  m_tic, m_low_mz, m_high_mz;
	private Spectrum m_s;
	private String   m_title;
	private Boolean  is_centroided;		// class Boolean so it may be null (denotes not specifed/unknown)
	
	public SpectrumAdapter(Spectrum s, boolean dont_load_ms1) throws InvalidSettingsException {
		assert(s != null);
		
		m_s = s;
		// scan level?
		CVParam msLevel_param = cvParam(s.getCvParam(), "MS:1000511");
		mslevel = -1;
		if (msLevel_param != null) {
			try {
				mslevel = Integer.valueOf(msLevel_param.getValue());
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
			}
		}
		
		// centroided scan?
		CVParam centroid_param = cvParam(s.getCvParam(), "MS:1000127");
		if (centroid_param != null) {
			setCentroided(Boolean.TRUE);
		} else {
			// we dare not assume the absence means false...
			setCentroided(null);
		}
		
		// retention time?
		List<CVParam> scan_params = s.getScanList().getScan().get(0).getCvParam();
		CVParam rt_param = cvParam(scan_params, "MS:1000016");
		rt = 0.0;
		if (rt_param != null) {
			String unit = rt_param.getUnitAccession().trim();
			if (unit.equals("UO:0000031")) {
				rt = Double.valueOf(rt_param.getValue().trim()) * 60.0d;
			} else {
				rt = Double.valueOf(rt_param.getValue().trim());
			}
		}
		
		// dummy up a title?
		CVParam title_param = cvParam(scan_params, "filter string");
		if (title_param != null && title_param.getValue().trim().length() > 0) {
			setTitle(title_param.getValue());
		} else {
			title_param = cvParam(m_s.getCvParam(), "scan title");	// generally for mzML files this isn't as good as the filter string so we use it if we must
			if (title_param != null) {
				setTitle(title_param.getValue());
			} else {
				setTitle(null);		// for now, we may try to dummy something one day...
			}
		}
	
		// BUG FIXME: only look at first precursor ion
		List<CVParam> precursor_list = null;
		if (s.getPrecursorList() != null) {
			precursor_list = s.getPrecursorList().getPrecursor().get(0).getSelectedIonList().getSelectedIon().get(0).getCvParam();
		}
		pmz = 0.0;
		CVParam parent_ion_mz = cvParam(precursor_list, "MS:1000744");
		if (parent_ion_mz != null) {
			pmz = Double.valueOf(parent_ion_mz.getValue().trim());
		}
		CVParam parent_charge = cvParam(precursor_list, "MS:1000041");
		pc = 0;
		if (parent_charge != null) {
			pc = Integer.valueOf(parent_charge.getValue().trim());
		}
		CVParam parent_intensity = cvParam(precursor_list, "MS:1000042");
		pi = 0.0;
		if (parent_intensity != null) {
			pi = Double.valueOf(parent_intensity.getValue().trim());
		}
		if (pc == 0)		// charge not specified? assume +1
			pc = 1;
		
		// if the user doesn't want MS1's loaded then use a singleton peak list and the caller can test for it
		if (dont_load_ms1 && mslevel < 2) {
			pl = PeakListImpl.newEmptyInstance();
		} else {
			// peaklist data
			List<BinaryDataArray> bdal = s.getBinaryDataArrayList().getBinaryDataArray();
			BinaryDataArray mz_bda = bdal.get(0);
			BinaryDataArray intensity_bda = bdal.get(1);
			
			double[] mz = getDoubles(mz_bda.getBinaryDataAsNumberArray());
			double[] intensity = getDoubles(intensity_bda.getBinaryDataAsNumberArray());
			
			Peak precursor_peak = null;
			if (parent_ion_mz != null)
				precursor_peak = new PeakImpl.Builder(pmz).intensity(pi).charge(pc).build();
			pl = null;
			if (precursor_peak != null) {
				pl = new PeakListImpl.Builder(mz).intensities(intensity).msLevel(mslevel).precursor(precursor_peak).build();
			} else {
				pl = new PeakListImpl.Builder(mz).intensities(intensity).msLevel(mslevel).build();
			}
		}
		
		// other stuff: base peak
		
		Map<String,String> cv_map = cvParamList2Map(s.getCvParam(), false);
		
		if (cv_map.containsKey("positive scan")) {
			setPositiveScan(true);
		} else if (cv_map.containsKey("negative scan")) {
			setPositiveScan(false);
		}
		setBasePeakMZ(cv_map.get("base peak m/z"));
		setBasePeakIntensity(cv_map.get("base peak intensity"));
		setTIC(cv_map.get("total ion current"));
		
		// HACK BUG TODO FIXME: this is really the lowest *observed* not what was configured, but we'll let the user figure that out ;-)
		setScanWindowLow(cv_map.get("lowest observed m/z"));
		setScanWindowHigh(cv_map.get("highest observed m/z"));
	}
	
	private void setCentroided(Boolean new_value) {
		is_centroided = new_value;
	}

	public Boolean isCentroided() {
		return is_centroided;
	}
	
	private void setTitle(String title) {
		m_title = title;
	}
	
	/**
	 * wont return null for now, but code should not assume it impossible (should use a missing cell in the case of a spectra without a title)
	 */
	public String getTitle() {
		if (m_title != null)  {
			return m_title;
	    } else {
			return "TITLE=Scan:"+getIndex()+", RT: "+getRetentionTime()+", ID: "+ getID();
		}
	}

	private void setScanWindowHigh(String val) {
		m_high_mz = Double.NaN;
		try {
			if (val == null) {
				return;
			}
			m_high_mz = Double.valueOf(val);
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
		}
	}

	private void setScanWindowLow(String val) {
		m_low_mz = Double.NaN;
		try {
			if (val == null) {
				return;
			}
			m_low_mz = Double.valueOf(val);
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
		}
	}
	
	private void setTIC(String val) {
		m_tic = Double.NaN;
		try {
			if (val == null) {
				return;
			}
			m_tic = Double.valueOf(val);
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
		}
	}

	private void setBasePeakIntensity(String val) {
		m_base_peak_intensity = Double.NaN;

		try {
			if (val == null) {
				return;
			}
			m_base_peak_intensity = Double.valueOf(val);
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
		}
	}
	
	private void setBasePeakMZ(String val) {
		m_base_peak_mz = Double.NaN;

		try {
			if (val == null) {
				return;
			}
				
			m_base_peak_mz = Double.valueOf(val);
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
		}
	}

	private void setPositiveScan(boolean true_if_positive) {
		m_scan_polarity = true_if_positive;
	}

	/**
	 * Construct a map using accessions (iff <code>use_accsn</code> is true) or names of data values
	 * otherwise using the given list
	 * @param cvParam list of parameters to use
	 * @return a map from accessions to values or names to values as specified
	 */
	private Map<String, String> cvParamList2Map(List<CVParam> cvParam, boolean use_accsn) {
		HashMap<String,String> ret = new HashMap<String,String>();
		for (CVParam p : cvParam) {
			if (use_accsn) 
				ret.put(p.getAccession(), p.getValue());
			else 
				ret.put(p.getName(), p.getValue());
		}
		return ret;
	}

	/**
	 * MS or MS/MS or ... ?
	 * @return
	 */
	public int getMSLevel() {
		return mslevel;
	}
	
	/*
	 * Return the parent ion m/z
	 */
	public double getParentMZ() {
		return pmz;
	}
	
	/**
	 * Return the parent ion charge
	 */
	public int getParentCharge() {
		return pc;
	}
	
	/**
	 * Return the parent ion intensity
	 */
	public double getParentIntensity() {
		return pi;
	}
	
	/**
	 * Returns the retention time
	 * @return
	 */
	public double getRetentionTime() {
		return rt;
	}
	
	/**
	 * Index position in file
	 */
	public Integer getIndex() {
		if (m_s == null)
			return null;
		return m_s.getIndex();
	}
	
	public double getBasePeakMZ() {
		return m_base_peak_mz;
	}
	
	public double getBasePeakIntensity() {
		return m_base_peak_intensity;
	}
	
	/**
	 * 
	 * @return true denotes +ve mode, otherwise -ve
	 */
	public boolean isPositiveScan() {
		return m_scan_polarity;
	}
	
	/**
	 * Return the ID of the scan (only guaranteed unique on a per file basis)
	 */
	public String getID() {
		if (m_s == null) 
			return null;
		return m_s.getId();
	}
	
	/**
	 * Returns the peak list
	 */
	public PeakList getPeakList() {
		return pl;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getScanID() {
		String id = getID();
		if (id != null) {
			Pattern p = Pattern.compile("\\sscan=(\\d+)\\s*$");
			Matcher m = p.matcher(id);
			if (m.find()) {
				return m.group(1);
			}
			// else fallthru...
		} 
		if (m_s != null) {
			return m_s.getIndex().toString();
		} else {
			// give up
			return null;
		}
	}

	/**
	 * 
	 * @param da
	 * @return
	 */
	private double[] getDoubles(Number[] da) {
		double[] ret = new double[da.length];
		for (int i=0; i<ret.length; i++) {
			ret[i] = da[i].doubleValue();
		}
		return ret;
	}

	private CVParam cvParam(final List<CVParam> list, String id) {
	   if (list == null)
		   return null;
	   for (CVParam cvp : list) {
	         if (cvp.getAccession().equals(id)) {
	                   return cvp;
	         } else if (cvp.getName().equals(id)) {
	                   return cvp;
	         }
	   }
	   return null;
	}

	public double getTIC() {
		return m_tic;
	}

	public double getHighMZ() {
		return m_high_mz;
	}
	
	public double getLowMZ() {
		return m_low_mz;
	}

	


}
