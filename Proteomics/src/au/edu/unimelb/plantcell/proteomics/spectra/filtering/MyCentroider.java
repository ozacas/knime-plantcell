package au.edu.unimelb.plantcell.proteomics.spectra.filtering;

import org.expasy.jpl.commons.collection.IntegerSequence;
import org.expasy.jpl.commons.collection.IntegerSequenceWithGap;
import org.expasy.jpl.core.ms.spectrum.PeakList;
import org.expasy.jpl.core.ms.spectrum.PeakListImpl;
import org.expasy.jpl.core.ms.spectrum.PeakListImpl.Builder;
import org.expasy.jpl.core.ms.spectrum.editor.AbstractPeakListEditor;
import org.knime.core.node.NodeLogger;

/**
 * Need a class which wraps the PeakCentroider depending on whether it is adaptive or fixed for the KNIME platform.
 * 
 * @author andrew.cassin
 *
 */
public class MyCentroider extends AbstractPeakListEditor {
	private NodeLogger logger;
	
	/**
	 * mz sampling distance (minimal difference between 2 consecutive mz values)
	 */
	private double mzSamplingDist;
	/** Maximum distance between consecutive mz values in same peak group */
	private double mzMaxDiff;
	
	/**
	 * Default constructor
	 */
	public MyCentroider(final NodeLogger logger) {
		this.logger = logger;
		setMzSamplingDist(0.1);
	}
	
	/**
	 * constructor
	 */
	public MyCentroider(final NodeLogger logger, double mzSamplingDist) {
		this.logger = logger;
		setMzSamplingDist(mzSamplingDist);
	}
	
	/**
	 * Calculates sampling difference. Assumes constant sampling rate for all
	 * masses.
	 * 
	 * @param inputSpectrum Spectrum to determine the sampling difference
	 */
	public void calcMzSamplingDist(final PeakList inputSpectrum) {
		final double[] masses = inputSpectrum.getMzs();
		double minDiff = 1000000.0;
		double diff;
		
		// Calc minimal difference between 2 consecutive mz values
		for (int i = 1; i < masses.length; i++) {
			diff = masses[i] - masses[i - 1];
			if (diff < minDiff) {
				minDiff = diff;
			}
		}
		
		setMzSamplingDist(minDiff);
	}
	
	/**
	 * Find the local maxima and the preceding and succeeding valleys.
	 * 
	 * @param inputSpectrum the input spectrum.
	 * @return sequence of interval.
	 */
	private IntegerSequenceWithGap getLocalMaxima(final PeakList inputSpectrum) {
		
		final IntegerSequenceWithGap maxima =
		    IntegerSequenceWithGap.newInstance();
		
		final double[] heights = inputSpectrum.getIntensities();
		final double[] masses = inputSpectrum.getMzs();
		
		// Find maxima
		for (int i = 0; i < masses.length; i++) {
			int iMin = Math.max(i - 1, 0);
			int iMax = Math.min(i + 1, masses.length - 1);
			boolean isMax = true;
			// check whether there is a higher peak to the left ..
			while (isMax && (iMin >= 0)
			    && (masses[iMin] >= masses[i] - mzMaxDiff)) {
				if (heights[iMin] > heights[i]) {
					isMax = false;
				}
				iMin--;
			}
			// and higher peak to the right of masses[i]
			while (isMax && (iMax < masses.length)
			    && (masses[iMax] <= masses[i] + mzMaxDiff)) {
				if ((iMax > i) && (heights[iMax] >= heights[i])) {
					isMax = false;
				}
				iMax++;
			}
			
			if (isMax) {
				iMin = i;
				// find lower bound for centroid
				while ((iMin >= 1)
				    && (masses[iMin] - masses[iMin - 1] < mzMaxDiff)) {
					if (heights[iMin] < heights[iMin - 1]) {
						break;
					}
					iMin--;
				}
				// find upper bound for centroid
				iMax = i;
				while ((iMax < masses.length - 1)
				    && (masses[iMax + 1] - masses[iMax] < mzMaxDiff)) {
					if (heights[iMax] < heights[iMax + 1]) {
						break;
					}
					iMax++;
				}
				
				// maxima.add(new Interval(iMin, iMax, i));
				maxima.add(new IntegerSequence.Builder(iMin, iMax + 1).by(1)
				    .build());
			}
		}
		
		return maxima;
	}
	
	/**
	 * Selects local maxima and calculates their centroid values. Returns the
	 * same but processed spectrum object.
	 * 
	 * @param spectrum The spectrum to process.
	 * @return The processed spectrum.
	 */
	@Override
	public PeakList processMSPeakList(final PeakList spectrum) {
		if (spectrum.hasIntensities()) {
			final double[] masses = spectrum.getMzs();
			ensurePeakListIsSortedByMZ(masses);
			final double[] heights = spectrum.getIntensities();
			rejectZeroIntensityPeaks(heights);
			
			// Get maxima and their lower and upper bounds for centroiding
			final IntegerSequenceWithGap maxima = getLocalMaxima(spectrum);
			
			// define new arrays
			final double[] cHeights = new double[maxima.size()];
			final double[] cMasses = new double[maxima.size()];
			
			// calculate centroids
			for (int i = 0; i < maxima.size(); i++) {
				final IntegerSequence interval = maxima.get(i);
				double h = 0.0;
				double m = 0.0;
				for (int j = interval.getFrom(); j < interval.getTo(); j +=
				    interval.getBy()) {
					h += heights[j];
					m += heights[j] * masses[j];
				}
				
				if (h > 0) {
					cMasses[i] = m / h;
				} else {
					for (int j = interval.getFrom(); j < interval.getTo(); j +=
					    interval.getBy()) {
						m += masses[j];
					}
					cMasses[i] = m / interval.size();
				}
				
				cHeights[i] = h;
			}
			
			Builder builder =
			    new PeakListImpl.Builder(cMasses)
			        .msLevel(spectrum.getMSLevel()).intensities(cHeights);
			
			if (spectrum.getPrecursor() != null) {
				builder.precursor(spectrum.getPrecursor());
			}
			
			return builder.build();
		} else {
			throw new UnsupportedOperationException("process is not supported"
			    + " for " + spectrum.getClass().getSimpleName());
		}
	}
	
	private void rejectZeroIntensityPeaks(double[] heights) throws IllegalArgumentException {
		if (heights == null || heights.length < 1) {
			return;
		}
		for (double d : heights) {
			if (d <= 0.0d) {
				throw new IllegalArgumentException("Must not have zero peaks in spectra!");
			}
		}
	}

	private void ensurePeakListIsSortedByMZ(double[] masses) throws IllegalArgumentException {
		if (masses == null || masses.length < 1) {
			return;
		}
		double prev = masses[0];
		for (int i=1; i<masses.length; i++) {
			double cur = masses[i];
			if (cur < prev) {
				throw new IllegalArgumentException("Peaklist must be sorted by increasing m/z");
			}
			prev = cur;
		}
	}

	/**
	 * Get maximum allowed distance between consecutive mz values in same peak
	 * group.
	 * 
	 * @return the Maximum distance.
	 */
	public double getMzMaxDiff() {
		return mzMaxDiff;
	}
	
	/**
	 * Set maximum allowed distance between consecutive mz values in same peak
	 * group.
	 * 
	 * @param mzMaxDiff the Maximum distance.
	 */
	public void setMzMaxDiff(final double mzMaxDiff) {
		if (mzMaxDiff < mzSamplingDist) {
			throw new IllegalArgumentException("mzMaxDiff " + mzMaxDiff
			    + " has to be larger than mzSamplingDist " + mzSamplingDist);
		}
		this.mzMaxDiff = mzMaxDiff;
	}
	
	/**
	 * Set minimal distance between mz consecutive values.
	 * 
	 * @param mzSamplingDist the Minimal distance between mz consecutive values.
	 */
	public void setMzSamplingDist(final double mzSamplingDist) {
		this.mzSamplingDist = mzSamplingDist;
		this.mzMaxDiff      = 1.5 * mzSamplingDist;
		logger.info("mzSampleDist: "+mzSamplingDist+", mzMaxDiff: "+mzMaxDiff);
	}
	
	/**
	 * Get minimal distance between mz consecutive values.
	 * 
	 * @return Minimal distance between mz consecutive values.
	 */
	public double getMzSamplingDist() {
		return mzSamplingDist;
	}
}
