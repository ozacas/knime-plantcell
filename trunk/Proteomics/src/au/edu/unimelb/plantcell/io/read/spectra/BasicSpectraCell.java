package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.IOException;

import org.expasy.jpl.core.ms.spectrum.peak.Peak;
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
public class BasicSpectraCell extends AbstractSpectraCell {
	/**
	 *  for serialisation
	 */
	private static final long serialVersionUID = 837426780080298388L;

	private BasicPeakList m_pl;
	
	private static final DataCellSerializer<BasicSpectraCell> SERIALIZER = new BasicSpectraCellSerializer();
	
	public BasicSpectraCell(BasicPeakList pl) {
		assert(pl != null);
		m_pl = pl;
	}
	
	public static final Class<? extends DataValue> getPreferredValueClass() {
        return SpectraValue.class;
    }
	
	public static final DataCellSerializer<BasicSpectraCell> getCellSerializer() {
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
	public BasicSpectraCell getMyValue() {
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

	public void initHeaders(BasicSpectraCell sv) {
		assert(sv != null && m_pl != null);
		m_pl.initHeaders(sv.m_pl);
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
	
	public String getScan() {
		String ret = m_pl.getHeader("SCANS");
		return (ret != null) ? ret : "";
	}
	
	public String getRT() {
		return m_pl.getRT_safe();
	}
	

	@Override
	public Peak getPrecursor() {
		return m_pl.getPrecursor();
	}
	
	/**
	 * Implement our own mechanism to persist <code>BasicSpectraCell</code> instances, typically this is faster
	 * than using java.lang.Serializable but we do this not just for speed but for correct
	 * instantiation of the objects
	 * 
	 * @author andrew.cassin
	 *
	 */
	private static class BasicSpectraCellSerializer implements DataCellSerializer<BasicSpectraCell> {

		@Override
		public BasicSpectraCell deserialize(final DataCellDataInput input) throws IOException {
			BasicPeakList mgf = BasicPeakList.load(input);
			return new BasicSpectraCell(mgf);
		}

		@Override
		public void serialize(final BasicSpectraCell spectra, final DataCellDataOutput output)
				throws IOException {
			if (spectra == null || output == null) 
				throw new IOException("Bad data given to BasicSpectraCellInitializer::serialize()");
			
			BasicPeakList.save(spectra.m_pl, output);
		}
	}

}
