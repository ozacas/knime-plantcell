package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.expasy.jpl.core.ms.lc.RetentionTime;
import org.expasy.jpl.core.ms.spectrum.PeakList;
import org.expasy.jpl.core.ms.spectrum.PeakListImpl;
import org.expasy.jpl.core.ms.spectrum.PeakListImpl.Builder;
import org.expasy.jpl.core.ms.spectrum.peak.Peak;
import org.expasy.jpl.core.ms.spectrum.peak.PeakImpl;
import org.expasy.jpl.io.ms.MassSpectrum;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;


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
		
		if (pl != null && pl.getPrecursor() != null) {
			init(pl.getPrecursor(), "", pl.getMSLevel());
		}
	}
	
	/**
	 * Typical usage:<code>
	 * MSScan ms = ...;
	 * new BasicPeakList(ms);
	 * </code>
	 * 
	 * @param ms: used to populate the entire internal state based on javaprotlib's internals
	 */
	public BasicPeakList(MassSpectrum ms) {
		assert(ms != null);
		PeakList pl = ms.getPeakList();
		assert(pl != null);
		
		Peak precursor = pl.getPrecursor();
		
		// try to determine if MS/MS or ...
		int ms_level = -1;
		if (pl.getMSLevel() > 0)
			ms_level = pl.getMSLevel();
		
		// determine a title from internal state from ms...
		String id      = ms.getId();
		String comment = ms.getComment();
		String title   = ms.getTitle();
		if (title == null)
			title = "";
	
		// ensure peaklist and internal state is set...
		if (precursor != null) {
			init(precursor, title, ms_level);
		} else {
			init("", "", title, ms_level);
		}
		if (id != null && id.length() > 0)
			addHeader("SCANS", id);
		if (comment != null && comment.length() > 0)
			addHeader("COMMENT", comment);
		
		setPeakList(ms.getPeakList());
	}

	public BasicPeakList(String pepmass, String charge, String title, int msLevel) {
		assert(msLevel >= 1);
		init(pepmass, charge, title, msLevel);
	}

	/**
	 * only call this if the precursor is known
	 * @param precursor may not be null
	 * @param title
	 * @param msLevel
	 */
	protected void init(final Peak precursor, final String title, final int msLevel) {
		assert(precursor != null);
		init(String.valueOf(precursor.getMz()), String.valueOf(precursor.getCharge()), title, msLevel);
		
		// add headers for the retention time if the precursor has it...
		RetentionTime rt = precursor.getRT();
		if (rt != null) {
			addHeader("RTINSECONDS", String.valueOf(rt2seconds(rt)));
		}
	}
	
	/**
	 * Returns the RT in seconds as a double
	 * @param rt
	 * @return
	 */
	private double rt2seconds(RetentionTime rt) {
		double val = rt.getValue();
		if (rt.getUnit().equals(RetentionTime.RTUnit.minute))
			val *= 60.0;
		return val;
	}

	@SuppressWarnings("unused")
	protected void init(final String pepmass, final String charge, final String title, int msLevel) {
		double pm = Double.valueOf(pepmass);
		int z = decodeChargeString(charge);
		if (z > 0)
				addHeader("CHARGE", charge);
		m_ms_level = msLevel;
		addHeader("TITLE", title);
		addHeader("PEPMASS", pepmass);
	}
	
	/**
	 * Attempt to decode the charge state of the ion, if this fails -1 will be returned. Mascot strings are supported (partially)
	 * 
	 * @param charge
	 * @return -1 if charge string cannot be decoded
	 */
	public static final int decodeChargeString(String charge) {
		try {
			if (charge != null && charge.length() > 0 && !charge.equals("NA") && !charge.equals("-1")) {
				if (charge.endsWith("+")) {
					charge = charge.substring(0, charge.length()-1);
				}
				if (charge.startsWith("+")) {
					charge = charge.substring(1);
				}
				return Integer.valueOf(charge);
			}
		} catch (NumberFormatException nfe) {
			// be silent for now... and fallthru
		}
		return -1;
	}
	
	/**
	 * This is the recommended constructor to use as it ensures that the m/z peaks are ordered (ascending)
	 * @param pepmass   should be greater than zero
	 * @param charge    should be greater or equal to zero
	 * @param title
	 * @param msLevel
	 * @param mz		must not be null. Must be the same length as intensity array
	 * @param intensity must not be null. Must be the same length as mz array
	 */
	public BasicPeakList(double pepmass, int charge, String title, int msLevel, double[] mz, double[] intensity) {
		this(String.valueOf(pepmass), String.valueOf(charge), title, msLevel);
		assert(mz != null && intensity != null);
		Peak precursor = null;
		if (charge > 0 && pepmass > 0.0)
			precursor = new PeakImpl.Builder(pepmass).charge(charge).msLevel(msLevel).build();
		setPeakList(mz, intensity, precursor);
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

	/**
	 * Construct the internal state for the peaklist using the specified parameters
	 * 
	 * @param mz		must not be null or zero length and must be the same length as the intensity vector
	 * @param intensity
	 * @param precursor may be null
	 */
	public void setPeakList(double[] mz, double[] intensity, Peak precursor) {
		if (mz == null || intensity == null || mz.length < 1 || mz.length != intensity.length)
			setPeakList(PeakListImpl.emptyInstance());
		else {
			List<Peak> pl = new ArrayList<Peak>(mz.length);
			for (int i=0; i<mz.length; i++) {
				pl.add(new PeakImpl.Builder(mz[i]).intensity(intensity[i]).msLevel(m_ms_level).build());
			}
			Collections.sort(pl);
			setPeakList(pl, precursor);
		}
	}
	
	/**
	 * Convenience wrapper around <code>setPeakList(PeakList pl)</code>. This constructor assumes the input list
	 * has been sorted into increasing m/z
	 * 
	 * @param pl
	 */
	public void setPeakList(List<Peak> pl, final Peak precursor) {
		// construct a new peak list
		double[] mz = new double[pl.size()];
		double[] intensities = new double[pl.size()];
		int idx = 0;
		for (Peak p : pl) {
			mz[idx] = p.getMz();
			intensities[idx] = p.getIntensity();
			idx++;
		}
		Builder new_list = new PeakListImpl.Builder(mz).
									intensities(intensities).
									msLevel(m_ms_level);
		if (precursor != null)
			new_list = new_list.precursor(precursor);
		
		// and then call appropriate initialiser
		setPeakList(new_list.build());
	}
	
	/**
	 * Initialises the peaklist and related members, null may be passed, in which case a singleton instance of an empty peak list will be used
	 */
	public void setPeakList(PeakList pl) {
		assert(pl != null);
		m_pl = pl;
		m_mz_min = 0.0d;
		m_mz_max = 0.0d;
		
		// need to recompute m_mz_min and max?
		if (m_pl != null && m_pl.size() > 0) {
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
		} else {
			m_pl = PeakListImpl.emptyInstance();
		}
	}
	
	private void addHeader(String key, String val) {
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

	public String getRT_safe() {
		String rt = getHeader("RTINSECONDS");
		return (rt != null) ? rt : "";
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
		
		return m_pl.getMzs();
	}
	
	/**
	 * @return never null (but possibly zero length if spectra has no peaks)
	 */
	public double[] getIntensity() {
		int n = getNumPeaks();
		if (n < 1)
			return new double[0];
		return m_pl.getIntensities();
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

	/**
	 * Copy key header information about the peaklist from <code>in</code>
	 * @param in
	 */
	public void initHeaders(final BasicPeakList in) {
		for (String hdr : in.m_headers.keySet()) {
			addHeader(hdr, in.m_headers.get(hdr));
		}
		this.setTitle(in.getTitle_safe());
	}
	
	
	/**
	 * Read the internal state for the peaklist from the specified input stream
	 * @param input guaranteed non-NULL
	 * @return the loaded peaklist
	 */
	public static BasicPeakList load(final DataCellDataInput input) throws IOException {
		// 1. load the peaklist
		int n_peaks = input.readInt();
		double[] mz = new double[n_peaks];
		double[] intensity = new double[n_peaks];
		for (int i=0; i<n_peaks; i++) {
			mz[i]        = input.readDouble();
			intensity[i] = input.readDouble();
		}
		
		// 2. load precursor peak
		double pre_mz = input.readDouble();
		double pre_intensity = input.readDouble();
		int pre_charge = input.readInt();
		int pre_ms_level = input.readInt();
		
		// 3. load metadata (MS Level, number of header map <string,stirng> pairs, pairs)
		int tc = input.readInt();
		int n_headers = input.readInt();
		HashMap<String,String> map = new HashMap<String,String>();
		for (int i=0; i<n_headers; i++) {
			String hdr = input.readUTF();
			String val = input.readUTF();
			map.put(hdr, val);
		}
	
		BasicPeakList ret = new BasicPeakList(map.get("PEPMASS"), map.get("CHARGE"), map.get("TITLE"), tc);
		PeakList pl = null;
		if (n_peaks > 0) {
			Peak peak_precursor = new PeakImpl.Builder(pre_mz).intensity(pre_intensity).msLevel(pre_ms_level).charge(pre_charge).build();
			pl = new PeakListImpl.Builder(mz).intensities(intensity).msLevel(tc).precursor(peak_precursor).build();
		} else {
			pl = PeakListImpl.emptyInstance();
		}
		ret.setPeakList(pl);
		ret.m_headers.clear();
		ret.m_headers.putAll(map);
		return ret;
	}
	
	public static void save(final BasicPeakList saveme, final DataCellDataOutput output) throws IOException {
		if (saveme == null) {
			// no peaklist, but must still be compatible...
			output.writeInt(0);
			
			// no precursor, but must still be compatible...
			output.writeDouble(0.0d);
			output.writeDouble(0.0d);
			output.writeInt(-1);
			output.writeInt(-1);
			
			// save minimal (and bogus) header
			output.writeInt(-1);
			output.writeInt(3);
			
			output.writeUTF("TITLE"); output.writeUTF("unknown");
			output.writeUTF("PEPMASS"); output.writeUTF("0.0");
			output.writeUTF("CHARGE"); output.writeUTF("1+");
			return;
		}
		// else...
		
		// 1. write output peaks (NB: same length arrays)
		int n_peaks = saveme.getNumPeaks();
		output.writeInt(n_peaks);
		double[] mz = saveme.getMZ();
		double[] intensity = saveme.getIntensity();
		assert(n_peaks == mz.length && n_peaks == intensity.length);
		for (int i=0; i<n_peaks; i++) {
			output.writeDouble(mz[i]);
			output.writeDouble(intensity[i]);
		}
		
		// 2. write precursor peak
		Peak precursor = saveme.getPrecursor();
		if (precursor != null) { 	// handle no precursor case 
			output.writeDouble(precursor.getMz());
			output.writeDouble(precursor.getIntensity());
			output.writeInt(precursor.getCharge());
			output.writeInt(precursor.getMSLevel());
		} else {
			output.writeDouble(0.0d);
			output.writeDouble(0.0d);
			output.writeInt(-1);
			output.writeInt(-1);
		}
		
		// 3. save metadata: here we must be very careful to always have PEPMASS, CHARGE and TITLE specified even if not supplied by the input data
		//    as the loading code expects it to be there. Impute from something if need be...
		output.writeInt(saveme.getTandemCount());
		HashMap<String,String> out_map = new HashMap<String,String>();
		out_map.putAll(saveme.m_headers);
		if (!out_map.containsKey("TITLE")) {
			out_map.put("TITLE", saveme.getTitle_safe());
		}
		if (!out_map.containsKey("PEPMASS")) {
			if (precursor != null)
				out_map.put("PEPMASS", String.valueOf(precursor.getMz()));
			else 
				out_map.put("PEPMASS", "0.0");
		}
		if (!out_map.containsKey("CHARGE")) {
			if (precursor != null) 
				out_map.put("CHARGE", String.valueOf(precursor.getCharge())+"+");
			else {
				out_map.put("CHARGE", "1+");
			}
		}
		output.writeInt(out_map.size());
		for (String key : out_map.keySet()) {
			output.writeUTF(key);
			output.writeUTF(out_map.get(key));
		}
	}

}
