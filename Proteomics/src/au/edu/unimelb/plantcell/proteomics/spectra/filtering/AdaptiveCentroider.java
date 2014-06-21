package au.edu.unimelb.plantcell.proteomics.spectra.filtering;

import org.expasy.jpl.core.ms.spectrum.PeakList;
import org.knime.core.node.NodeLogger;

public class AdaptiveCentroider extends MyCentroider {

	public AdaptiveCentroider(NodeLogger logger) {
		super(logger);
	}

	@Override
	public PeakList processMSPeakList(final PeakList spectrum) {
		if (spectrum.hasIntensities()) {
			calcMzSamplingDist(spectrum);
			return super.processMSPeakList(spectrum);
		} else {
			throw new UnsupportedOperationException("Spectrum has no peaks: "+spectrum);
		}
	}
}
