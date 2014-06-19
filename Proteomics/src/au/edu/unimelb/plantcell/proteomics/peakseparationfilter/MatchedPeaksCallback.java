package au.edu.unimelb.plantcell.proteomics.peakseparationfilter;

public interface MatchedPeaksCallback {

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
}
