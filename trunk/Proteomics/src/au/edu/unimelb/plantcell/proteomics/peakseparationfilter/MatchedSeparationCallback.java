package au.edu.unimelb.plantcell.proteomics.peakseparationfilter;

import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;

public interface MatchedSeparationCallback {

	/**
	 * Responsible for returning true if the distance matching process should continue, false if not.
	 * This method will be called when two peaks (specified by mz1 and mz2) are within the specified distance
	 * This method enables the caller to keep track of what is being matched and respond accordingly.
	 * 
	 * @param mz1
	 * @param mz2
	 * @param accepted_distance
	 * @return
	 */
	public boolean acceptHit(double mz1, double mz2, double accepted_distance);

	/**
	 * Called during processing for each spectra, this enables the callback to know what the current spectra is
	 * before acceptHit() is called (if a hit is found)
	 * 
	 * @param sv guaranteed non-null
	 */
	public void setSpectra(final SpectraValue sv);
}
