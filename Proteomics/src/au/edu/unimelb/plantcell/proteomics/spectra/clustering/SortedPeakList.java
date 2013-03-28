package au.edu.unimelb.plantcell.proteomics.spectra.clustering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;

/**
 * Provides a wrapper around the peaks in a {@link SpectraValue} with 
 * the peaks/intensities in increasing m/z order
 * 
 * @author andrew.cassin
 *
 */
public class SortedPeakList {
	private final List<Peak> m_sorted = new ArrayList<Peak>();
	
	public SortedPeakList(SpectraValue sv) {
		assert(sv != null);
		double[] mz = sv.getMZ();
		double[] intensities = sv.getIntensity();
		
		assert(mz.length == intensities.length);
		m_sorted.clear();
		for (int i=0; i<mz.length; i++) {
			m_sorted.add(new Peak(mz[i], intensities[i]));
		}
		
		Collections.sort(m_sorted, new Comparator<Peak>() {

			@Override
			public int compare(Peak o1, Peak o2) {
				if (o1.getMZ() < o2.getMZ()) {
					return -1;
				} else if (o1.getMZ() > o2.getMZ()) {
					return 1;
				} else {
					// compare intensities to decide order
					if (o1.getIntensity() < o2.getIntensity()) {
						return -1;
					} else if (o1.getIntensity() > o2.getIntensity()) {
						return 1;
					} else {
						return 0;
					}
				}
			}
			
		});
	}
	
	public double[] getMZ() {
		double[] mz = new double[m_sorted.size()];
		int i=0;
		for (Peak p : m_sorted) {
			mz[i++] = p.getMZ();
		}
		return mz;
	}
	
	public double[] getIntensities() {
		double[] ret = new double[m_sorted.size()];
		int i=0;
		for (Peak p : m_sorted) {
			ret[i++] = p.getIntensity();
		}
		return ret;
	}
	
	class Peak {
		private final double m_mz;
		private final double m_intensity;
		
		public Peak(double mz, double intensity) {
			m_mz = mz;
			m_intensity = intensity;
		}
		
		public final double getMZ() { return m_mz; }
		
		public final double getIntensity() { return m_intensity; }
	}
}
