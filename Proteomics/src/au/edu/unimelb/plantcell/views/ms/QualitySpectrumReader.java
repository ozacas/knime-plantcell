package au.edu.unimelb.plantcell.views.ms;

import java.util.HashMap;

import au.edu.unimelb.plantcell.io.read.spectra.BasicPeakList;
import au.edu.unimelb.plantcell.proteomics.spectra.quality.XreaScore;


/**
 * Computes Xrea scores for each MSn (n>=2) spectrum seen. This results in the computed surface showing the
 * spectral quality at each datapoint where a precursor ion was fragmented.
 * 
 *
 * @param <T> must be an instance of MassSpecSurfaceNodeModel or subclass
 */
public class QualitySpectrumReader<T extends MassSpecSurfaceNodeModel> extends SpectrumReader<T> {
	private final HashMap<String,Double> m_ms2_scores = new HashMap<String,Double>();
	private double m_min = Double.POSITIVE_INFINITY;
	private double m_max = Double.NEGATIVE_INFINITY;
	private double m_range = 0.0;
	
	public QualitySpectrumReader(SurfaceMatrixAdapter m, T nodeModel, PeakThresholdFilter ptf, double peak_intensity_threshold) {
		super(m, nodeModel, ptf, peak_intensity_threshold);
	}
	
	@Override
	public void saveScore() {
		if (hasPeaks()) {
			BasicPeakList bpl = makePeakList();
			XreaScore xrea = new XreaScore(bpl);
			double score = xrea.getRawQualityScore();
			m_ms2_scores.put(getID(),score);
			if (score < m_min) {
				m_min = score;
				m_range = range(m_min, m_max);
			}
			if (score > m_max) {
				m_max = score;
				m_range = range(m_min, m_max);
			}
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
		return (val - m_min / m_range);
	}
	
}
