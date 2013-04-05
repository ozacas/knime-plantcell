package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.IOException;

import org.expasy.jpl.core.ms.spectrum.PeakList;
import org.expasy.jpl.core.ms.spectrum.PeakListImpl;
import org.expasy.jpl.core.ms.spectrum.peak.Peak;
import org.expasy.jpl.core.ms.spectrum.peak.PeakImpl;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataValue;

/**
 * Implements the supported data for an MGF-derived spectra, not all of this is visible
 * to users: as the renderers (currently) do not make use of some of the data. But eventually,
 * they will ;-)
 * 
 * @author andrew.cassin
 *
 */
public class MGFSpectraCell extends AbstractSpectraCell {
	/**
	 *  for serialisation
	 */
	private static final long serialVersionUID = 837426780080298388L;

	private BasicPeakList m_pl;
	
	private static final DataCellSerializer<MGFSpectraCell> SERIALIZER = new MGFSpectraCellSerializer();
	
	public MGFSpectraCell(BasicPeakList pl) {
		assert(pl != null);
		m_pl = pl;
	}
	
	public static final Class<? extends DataValue> getPreferredValueClass() {
        return SpectraValue.class;
    }
	
	public static final DataCellSerializer<MGFSpectraCell> getCellSerializer() {
		return SERIALIZER;
    }

	@Override
	protected boolean equalsDataCell(DataCell dc) {
		return (this == dc);
	}
	
	@Override
	public String getID() {
		return m_pl.getTitle_safe();	// HACK: assumes title is unique!
	}

	@Override
	public String getCharge() {
		return m_pl.getCharge_safe();
	}
	
	@Override
	public double[] getIntensity() {
		if (getNumPeaks() == 0)
			return null;
		
		return m_pl.getIntensity();
	}

	@Override
	public int getMSLevel() {
		if (m_pl == null)
			return 2;			// TODO HACK FIXME: hmm... well maybe its ok...
		return m_pl.getTandemCount();
	}

	@Override
	public double[] getMZ() {
		if (getNumPeaks() == 0)
			return null;
		
		return m_pl.getMZ();
	}

	@Override
	public MGFSpectraCell getMyValue() {
		return this;
	}

	@Override
	public int getNumPeaks() {
		return m_pl.getNumPeaks();
	}

	@Override
	public int hashCode() {
		return m_pl.hashCode();
	}

	@Override
	public String asString(boolean round) {
		StringBuilder sb = new StringBuilder();
		
		for (Peak p : m_pl.getPeaks()) {
			double mz = p.getMz();
			if (round) {
				sb.append(Math.round(mz * 1000.0) / 1000.0);
			} else {
				sb.append(mz);
			}
			sb.append(' ');
			sb.append(p.getIntensity());
			sb.append('\n');
		}
		return sb.toString();
	}

	@Override
	public double getMaxMZ() {
		return m_pl.getMaxMZ();
	}

	@Override
	public double getMinMZ() {
		return m_pl.getMinMZ();
	}

	public String getPepmass() {
		return m_pl.getPepmass_safe();
	}
	
	/**
	 * Implement our own mechanism to persist MGF spectra objects, typically this is faster
	 * than using java.lang.Serializable but we do this not just for speed but for correct
	 * instantiation of the objects
	 * 
	 * @author andrew.cassin
	 *
	 */
	private static class MGFSpectraCellSerializer implements DataCellSerializer<MGFSpectraCell> {

		@Override
		public MGFSpectraCell deserialize(final DataCellDataInput input) throws IOException {
			
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
			
			// 3. load metadata
			String title   = input.readUTF();
			String pepmass = input.readUTF();
			String charge  = input.readUTF();
			int         tc = input.readInt();
		
			BasicPeakList   mgf = new BasicPeakList(pepmass, charge, title, tc);
			try {
				Peak peak_precursor = new PeakImpl.Builder(pre_mz).intensity(pre_intensity).msLevel(pre_ms_level).charge(pre_charge).build();
				PeakList pl = new PeakListImpl.Builder(mz).intensities(intensity).msLevel(tc).precursor(peak_precursor).build();
				mgf.setPeakList(pl);
			} catch (Exception be) {
				be.printStackTrace();
			}
			
			return new MGFSpectraCell(mgf);
		}

		@Override
		public void serialize(final MGFSpectraCell spectra, final DataCellDataOutput output)
				throws IOException {
			if (spectra == null || output == null) 
				throw new IOException("Bad data given to MGFSpectraCellInitializer::serialize()");
			
			// 1. write output peaks (NB: same length arrays)
			int n_peaks = spectra.getNumPeaks();
			output.writeInt(n_peaks);
			double[] mz = spectra.getMZ();
			double[] intensity = spectra.getIntensity();
			assert(n_peaks == mz.length && n_peaks == intensity.length);
			for (int i=0; i<n_peaks; i++) {
				output.writeDouble(mz[i]);
				output.writeDouble(intensity[i]);
			}
			
			// 2. write precursor peak
			Peak precursor = spectra.getPrecursor();
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
			
			// 3. save header
			BasicPeakList pl = spectra.m_pl;
			if (pl != null) {
				output.writeUTF(pl.getTitle_safe());
				output.writeUTF(pl.getPepmass_safe());
				output.writeUTF(pl.getCharge_safe());
				output.writeInt(pl.getTandemCount());
			} else {
				output.writeUTF("");
				output.writeUTF("0.0");
				output.writeUTF("");
				output.writeInt(-1);
			}
		}
	}

	@Override
	public Peak getPrecursor() {
		return m_pl.getPrecursor();
	}
}
