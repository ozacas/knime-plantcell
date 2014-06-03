package au.edu.unimelb.plantcell.proteomics.views;

import java.util.HashMap;

import org.expasy.jpl.core.ms.spectrum.peak.Peak;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.io.read.spectra.BasicPeakList;

/**
 * Computes a set of precursor charges (scores) for MS/MS (or higher) spectra as the superclass traverses
 * the mzML document. The resulting set of values is then normalised over the range [0..1] and returned via <code>getMS2Score()</code>
 * 
 * @author andrew.cassin
 *
 * @param <T> must extend the MassSpecSurfaceNodeModel
 */
public class PrecursorChargeSpectrumReader<T extends MassSpecSurfaceNodeModel> extends SpectrumReader<T> {
	private final HashMap<String,Double> m_ms2_scores = new HashMap<String,Double>();
	private double m_min = Double.POSITIVE_INFINITY;
	private double m_max = Double.NEGATIVE_INFINITY;
	private double m_range = 0.0;
	private int no_prec;	// number of ms/ms (or higher) without a precursor specified
	private int no_peaks;	// number of ms/ms (or higher) without any peaks
	private int no_charge;  // number of ms/ms (or higher) without a precursor charge state defined
	private int no_accepted;	// number of ms/ms (or higher) accepted for their datapoint
	
	public PrecursorChargeSpectrumReader(final SurfaceMatrixAdapter m, final T nodeModel, final PeakThresholdFilter ptf, double peak_intensity_threshold) {
		super(m, nodeModel, ptf, peak_intensity_threshold);
		no_prec = 0;
		no_peaks = 0;
		no_charge = 0;
		no_accepted = 0;
	}
	

	@Override
	public void saveScore() {
		if (hasPeaks()) {
			BasicPeakList bpl = makePeakList();
			Peak precursor = bpl.getPrecursor();
			double score = -1.0;
			if (precursor != null) {
				score = precursor.getCharge();
			} else {
				no_prec++;
			}
			// only score MS/MS spectra where charge is known...
			if (score > 0.0) {
				if (score < m_min) {
					m_min = score;
					m_range = range(m_min, m_max);
				}
				if (score > m_max) {
					m_max = score;
					m_range = range(m_min, m_max);
				}
				m_ms2_scores.put(getID(), new Double(score));
				no_accepted++;
			} else {
				no_charge++;
			}
		} else {
			no_peaks++;
		}
	}
	
	
	private double range(double min, double max) {
		double ret = 0.0f;
    	if ((min < 0.0f && max < 0.0f) || (max >= 0.0f && min >= 0.0f)) {
    		ret = Math.abs(max) - Math.abs(min);
    	} else {
    		ret = Math.abs(min) + Math.abs(max);
    	}
    	
    	if (ret < 0)
    		return -ret;
    	return ret;
	}

	/**
	 * Returns the range of scores normalised into the range [0..1] (0 meaning lowest quality, 1 highest)
	 */
	@Override
	public double getMS2Score(String id) {
		if (!m_ms2_scores.containsKey(id))
			return 0.0;
		double val = m_ms2_scores.get(id).doubleValue();
		return (val - m_min) / m_range;
	}
	
	@Override
	public void logPeakSummary(NodeLogger logger) {
		super.logPeakSummary(logger);
		if (logger != null) {
			logger.info("Number of MS/MS (or higher) without any peaks: "+no_peaks);
			logger.info("Number of MS/MS (or higher) without precursor specified: "+no_prec);
			logger.info("Number of MS/MS (or higher) without predicted precursor charge state: "+no_charge); 
			logger.info("Accepted "+no_accepted+" MS/MS (or higher) spectra for data points.");
		}
	}
}
