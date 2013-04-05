package au.edu.unimelb.plantcell.io.read.spectra;

import java.util.List;
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
	private Spectrum m_s;
	
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
		
		// retention time?
		CVParam rt_param = cvParam(s.getScanList().getScan().get(0).getCvParam(), "MS:1000016");
		rt = 0.0;
		if (rt_param != null) {
			String unit = rt_param.getUnitAccession().trim();
			if (unit.equals("UO:0000031")) {
				rt = Double.valueOf(rt_param.getValue().trim()) * 60.0d;
			} else {
				rt = Double.valueOf(rt_param.getValue().trim());
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
		
		if (dont_load_ms1 && mslevel < 2) 
			throw new InvalidSettingsException("Cowardly refusing to load MS1 spectra as per requirements!");
		
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

	


}
