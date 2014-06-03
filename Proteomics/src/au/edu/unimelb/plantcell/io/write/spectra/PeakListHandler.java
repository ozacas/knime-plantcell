package au.edu.unimelb.plantcell.io.write.spectra;

import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;

public interface PeakListHandler {

	/**
	 * @param sv
	 * @return true if the peaklist should be saved, false otherwise
	 */
	public boolean accept(final SpectraValue sv);
}
