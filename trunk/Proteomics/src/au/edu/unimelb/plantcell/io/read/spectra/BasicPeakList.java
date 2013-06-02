package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.expasy.jpl.core.ms.spectrum.PeakList;
import org.expasy.jpl.core.ms.spectrum.PeakListImpl;
import org.expasy.jpl.core.ms.spectrum.peak.Peak;
import org.expasy.jpl.core.ms.spectrum.peak.PeakImpl;
import org.expasy.jpl.io.ms.MSScan;
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
	}
	
	/**
	 * Typical usage:<code>
	 * MSScan ms = ...;
	 * new BasicPeakList(ms);
	 * </code>
	 * 
	 * @param ms: used to populate the entire internal state based on javaprotlib's internals
	 */
	public BasicPeakList(MSScan ms) {
		Peak precursor = ms.getPrecursor();
		
		// try to determine if MS/MS or ...
		int ms_level = -1;
		if (ms.getMSLevel() > 0)
			ms_level = ms.getMSLevel();
		
		// determine a title from internal state from ms...
		String title = "";
		if (ms.getId() != null && ms.getId().length() > 0)
			title += ms.getId() + " ";
		if (ms.getComment() != null && ms.getComment().length() > 0)
			title += ms.getComment();
		
		// ensure peaklist and internal state is set...
		if (precursor != null) {
			init(String.valueOf(precursor.getMz()), String.valueOf(precursor.getCharge()), title, ms_level);
		} else {
			init("", "", title, ms_level);
		}
		
		setPeakList(ms.getPeakList());
	}

	public BasicPeakList(String pepmass, String charge, String title, int msLevel) {
		assert(msLevel >= 1);
		init(pepmass, charge, title, msLevel);
	}

	@SuppressWarnings("unused")
	protected void init(String pepmass, String charge, String title, int msLevel) {
		try {
			double pm = Double.valueOf(pepmass);
			if (charge != null && charge.length() > 0 && !charge.equals("NA") && !charge.equals("-1")) {
				if (charge.endsWith("+")) {
					charge = charge.substring(0, charge.length()-1);
				}
				if (charge.startsWith("+")) {
					charge = charge.substring(1);
				}
				int z = Integer.valueOf(charge);
				if (z > 0)
					addHeader("CHARGE", charge);
			}
			m_ms_level = msLevel;
		} catch (NumberFormatException nfe) {
			// be silent for now...
		}
		addHeader("TITLE", title);
		addHeader("PEPMASS", pepmass);
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
		setPeakList(mz, intensity);
	}
	
	/**
	 * @param next
	 *
	public BasicPeakList(SpectrumAdapter sa) {
		assert(sa != null);
	
		addHeader("TITLE", sa.getTitle());
		addHeader("PEPMASS", ""+sa.getParentMZ()+" "+sa.getParentIntensity());
		addHeader("CHARGE", String.valueOf(sa.getParentCharge()));
		if (sa.getRetentionTime() > 0.0) {
			addHeader("RTINSECONDS", String.valueOf(sa.getRetentionTime()));
		}
		addHeader("SCANS", String.valueOf(sa.getIndex()));
		setPeakList(sa.getPeakList());
	}*/
	 
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

	public void setPeakList(double[] mz, double[] intensity) {
		if (mz == null || intensity == null || mz.length < 1 || mz.length != intensity.length)
			setPeakList(PeakListImpl.newEmptyInstance());
		else {
			List<Peak> pl = new ArrayList<Peak>(mz.length);
			for (int i=0; i<mz.length; i++) {
				pl.add(new PeakImpl.Builder(mz[i]).intensity(intensity[i]).msLevel(m_ms_level).build());
			}
			Collections.sort(pl);
			setPeakList(pl);
		}
	}
	
	/**
	 * Convenience wrapper around <code>setPeakList(PeakList pl)</code>. This constructor assumes the input list
	 * has been sorted into increasing m/z
	 * 
	 * @param pl
	 */
	public void setPeakList(List<Peak> pl) {
		// construct a new peak list
		double[] mz = new double[pl.size()];
		double[] intensities = new double[pl.size()];
		int idx = 0;
		for (Peak p : pl) {
			mz[idx] = p.getMz();
			intensities[idx] = p.getIntensity();
			idx++;
		}
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
			m_pl = PeakListImpl.newEmptyInstance();
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
	public static BasicPeakList load(DataCellDataInput input) throws IOException {
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
		Peak peak_precursor = new PeakImpl.Builder(pre_mz).intensity(pre_intensity).msLevel(pre_ms_level).charge(pre_charge).build();
		PeakList pl = new PeakListImpl.Builder(mz).intensities(intensity).msLevel(tc).precursor(peak_precursor).build();
		ret.setPeakList(pl);
		ret.m_headers.clear();
		ret.m_headers.putAll(map);
		return ret;
	}
	
	public static void save(BasicPeakList saveme, DataCellDataOutput output) throws IOException {
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
		if (precursor != null) { 	// handle this exceptional case
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
		
		// 3. save metadata
		output.writeInt(saveme.getTandemCount());
		output.writeInt(saveme.m_headers.size());
		for (String key : saveme.m_headers.keySet()) {
			output.writeUTF(key);
			output.writeUTF(saveme.m_headers.get(key));
		}
	}

}
