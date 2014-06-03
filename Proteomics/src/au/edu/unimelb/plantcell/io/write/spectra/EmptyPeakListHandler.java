package au.edu.unimelb.plantcell.io.write.spectra;

import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;

/**
 * By default, removes peaklists without any peaks during MGF saving
 * 
 * @author acassin
 *
 */
public class EmptyPeakListHandler implements PeakListHandler {
	private boolean save_em;
	private final NodeLogger logger;
	private int min_peaks = 0;
	
	public EmptyPeakListHandler() {
		this(false, null, 0);
	}
	
	/**
	 * Removes all peak lists if save_em is false which have less or equal to the specified number of peaks
	 * 
	 * @param save_em accept the spectra anyway if true, other false will be returned by accept()
	 * @param logger  warnings about spectra with fewer than min_peaks are issued here
	 * @param min_peaks may reject peaklists with fewer than this number of peaks eg. if peak count is very low
	 */
	public EmptyPeakListHandler(boolean save_em, final NodeLogger logger, int min_peaks) {
		this.save_em = save_em;
		this.logger = logger;
		this.min_peaks = min_peaks;
	}
	
	@Override
	public boolean accept(final SpectraValue sv) {
		double[] mz = sv.getMZ();
		if (mz == null || mz.length <= min_peaks) {
			if (!save_em) {
				return false;
			}
			if (logger != null) {
				logger.warn(sv.getID()+ " has fewer than "+min_peaks+" peaks (but saved anyway)!");
			}
			// fallthru
		}
		return true;
	}
		

}
