package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.expasy.jpl.core.ms.spectrum.PeakList;
import org.expasy.jpl.core.ms.spectrum.PeakListImpl;
import org.expasy.jpl.core.ms.spectrum.peak.Peak;
import org.expasy.jpl.core.ms.spectrum.peak.PeakImpl;
import org.expasy.jpl.io.ms.MSScan;

import uk.ac.ebi.jmzml.model.mzml.Spectrum;


/**
 * MGF-derived peaklist cells are generally instances of this class. We keep track of MGF headers explicity,
 * but most of the code uses {@link http://javaprotlib.sourceforge.net}. This class is a simple one: intended for use
 * when little experimental information about the spectra is available. See {@link FullPeakList} for a more complex class
 * to use when lots more information about the spectra is needed.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class BasicPeakList implements Serializable {

	/**
	 * keep java serialisation happy
	 */
	private static final long serialVersionUID = -4897834331580715693L;
	
	private final HashMap<String,String> m_headers = new HashMap<String,String>();	// all header eg. TITLE, CHARGE, PEPMASS etc with their values
	private PeakList m_pl;
	private double m_mz_min, m_mz_max;
	private int m_ms_level = 2;
	
	public BasicPeakList() {
		this((PeakList)null);
	}
	
	public BasicPeakList(PeakList pl) {
		setPeakList(pl);
	}
	
	public BasicPeakList(List<Peak> pl) {
		setPeakList(pl);
	}
	
	public BasicPeakList(MSScan ms) {
		this(ms.getPeakList());
		
		// populate key headers from ms instance
		if (ms != null) {
			String title = ms.getTitle();
			if (title == null)				// should do better than this if title is missing
				title = ms.getComment();
			addHeader("TITLE",   title);
			Peak precursor = ms.getPeakList().getPrecursor();
			if (precursor != null) {
				addHeader("PEPMASS", String.valueOf(precursor.getMz()));
				addHeader("CHARGE",  String.valueOf(precursor.getCharge()));
			}
		}
	}

	public BasicPeakList(String pepmass, String charge, String title, int tc) {
		assert(tc >= 1);
		
		try {
			double pm = Double.valueOf(pepmass);
			if (charge.endsWith("+")) {
				charge = charge.substring(0, charge.length()-1);
			}
			if (charge.startsWith("+")) {
				charge = charge.substring(1);
			}
			int z = Integer.valueOf(charge);
			m_ms_level = tc;
		} catch (NumberFormatException nfe) {
			// be silent for now...
		}
		addHeader("TITLE", title);
		addHeader("PEPMASS", pepmass);
		addHeader("CHARGE", charge);
	}

	/**
	 * Constructs a BPL from a jMZML {@link Spectrum} instance
	 * 
	 * @param next
	 */
	public BasicPeakList(Spectrum s) {
		addHeader("TITLE", s.getId());
		setPeakList((PeakList)null);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("MyMGFPeakList does not support clone yet!");
	}

	@Override
	public boolean equals(Object obj) {
		return (this == obj);
	}

	@Override
	public int hashCode() {
		return m_pl.hashCode();
	}

	public void setPeakList(List<Peak> pl) {
		// construct a new peak list
		double[] mz = new double[pl.size()];
		double[] intensities = new double[pl.size()];
		PeakList new_list = new PeakListImpl.Builder(mz).
									intensities(intensities).
									msLevel(m_ms_level).
									build();
				
		// and then call appropriate initialiser
		setPeakList(new_list);
	}
	
	public void setPeakList(PeakList pl) {
		assert(pl != null);
		m_pl = pl;
		m_mz_min = 0.0d;
		m_mz_max = 0.0d;
		
		// need to recompute m_mz_min and max?
		if (m_pl != null) {
			double min = Double.MAX_VALUE;
			double max = Double.MIN_VALUE;
			for (Peak p : getPeaks()) {
				double mz = p.getMz();
				if (mz < min)
					min = p.getMz();
				else if (mz > max) 
					max = p.getMz();
			}
			m_mz_min = min;
			m_mz_max = max;
		}
	}
	
	public void addHeader(String key, String val) {
		m_headers.put(key, val);
	}
	
	public String getHeader(String key) {
		if (hasHeader(key)) {
			return m_headers.get(key); 
		}
		return null;
	}

	public boolean hasHeader(String key) {
		if (m_headers != null && m_headers.containsKey(key))
			return true;
		return false;
	}
	
	public void setCharge(String ch) {
		addHeader("CHARGE", ch);
	}
	
	public void setTitle(String title) {
		addHeader("TITLE", title);
	}
	
	public void setPepMass(String pm) {
		addHeader("PEPMASS", pm);
	}
	
	public double getMinMZ() {
		return m_mz_min;
	}
	
	public double getMaxMZ() {
		return m_mz_max;
	}

	public String getTitle_safe() {
		String title = getHeader("TITLE");
		return (title != null) ? title : "";
	}
	
	public String getPepmass_safe() {
		String pepmass = getHeader("PEPMASS");
		return (pepmass != null) ? pepmass : "";
	}
	
	public String getCharge_safe() {
		String charge = getHeader("CHARGE");
		return (charge != null) ? charge : "";
	}

	public int getTandemCount() {
		if (m_pl == null)
			return -1;
		return m_pl.getMSLevel();
	}

	/**
	 * Returns the number of peaks from the spectra or 0 if it is empty (as can happen if a spectra is filtered with too high a threshold)
	 * @return
	 */
	public int getNumPeaks() {
		if (m_pl == null)
			return 0;
		return m_pl.size();
	}
	
	/**
	 * @return never null (but possibly zero length if spectra has no peaks)
	 */
	public double[] getMZ() {
		int n = getNumPeaks();
		if (n < 1)
			return new double[0];
		
		double[] ret = new double[n];
		return m_pl.getMzs(ret);
	}
	
	/**
	 * @return never null (but possibly zero length if spectra has no peaks)
	 */
	public double[] getIntensity() {
		int n = getNumPeaks();
		if (n < 1)
			return new double[0];
		
		double[] ret = new double[n];
		return m_pl.getIntensities(ret);
	}
	
	/**
	 * Return the list of peaks in the order as obtained from the datasource
	 * @return
	 */
	public List<Peak> getPeaks() {
		ArrayList<Peak> ret = new ArrayList<Peak>(getNumPeaks());
		double[] mz = getMZ();
		double[] intensity = getIntensity();
		
		for (int i=0; i<mz.length; i++) {
			ret.add(new PeakImpl.Builder(mz[i]).intensity(intensity[i]).build());
		}
		return ret;
	}

	public Peak getPrecursor() {
		if (m_pl == null)
			return null;
		return m_pl.getPrecursor();
	}
	
}
