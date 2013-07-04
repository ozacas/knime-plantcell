package au.edu.unimelb.plantcell.views.ms;

import org.expasy.jpl.core.ms.spectrum.peak.Peak;

import au.edu.unimelb.plantcell.io.read.spectra.BasicPeakList;

/**
 * Eliminates peaks from the surface if they do not meet the required implementation
 * @author andrew.cassin
 *
 */
public interface PeakThresholdFilter {

	/**
	 * returns true if the specified peak is acceptable for the surface, false otherwise
	 * @param pbl
	 * @param p
	 * @param threshold	user configured threshold value
	 * @return
	 */
	boolean accept(BasicPeakList pbl, Peak p, double threshold);

}
