package au.edu.unimelb.plantcell.proteomics.spectra.quality;

import java.util.Arrays;
import java.util.Comparator;

import au.edu.unimelb.plantcell.io.read.spectra.BasicPeakList;
import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;

/**
 * Computes spectral quality using the Xrea metric: see http://www.aseanbiotechnology.info/Abstract/21020663.pdf
 * for details
 * 
 * @author andrew.cassin
 *
 */
public class XreaScore {
	private SortablePeak[] m_peaks;
	private double[] m_mz;
	private double[] m_i;
	private double m_i_max;
	private double m_tic;
	private int m_num_peaks;
	
	public XreaScore(BasicPeakList bpl) {
		this(bpl.getMZ(), bpl.getIntensity());
	}
	
	public XreaScore(SpectraValue sv) {
		this(sv.getMZ(), sv.getIntensity());
	}
	
	public XreaScore(double[] mz, double[] intensity) {
		assert(mz != null && intensity != null && mz.length == intensity.length);
		
		// this list is computed here and then thrown away at the end of processing each row for other quality ranking code to use
		double sum = 0.0;
		m_mz = mz;
		m_i  = intensity;
		m_peaks = new SortablePeak[m_mz.length];
		m_i_max = 0.0;
		for (int j=0; j<m_mz.length; j++) {
			m_peaks[j] = new SortablePeak(m_mz[j], m_i[j]);
			sum += m_i[j];
			if (m_i[j] > m_i_max)
				m_i_max = m_i[j];
		}
		Arrays.sort(m_peaks);
		m_tic = sum;
		m_num_peaks = mz.length;
	}
	 
    /*
    // code from Henry Lam (author of TPP spectrast, ported from C++ by Andrew Cassin)
    protected double calc_xrea(SpectralDataInterface c) {
    	 int n_peaks = c.getNumPeaks();
    	 
    	 if (n_peaks < 6) {
    		 return (0.0);
    	 }
    	 double[] mz = c.getMZ();
    	 double[] i  = c.getIntensity();
    	 SortablePeak[] ranking = new SortablePeak[mz.length];
     	 double sum = 0.0;
     	 for (int j=0; j<mz.length; j++) {
     		ranking[j] = new SortablePeak(mz[j], i[j]);
     		sum += i[j];
     	 }
     	 Arrays.sort(ranking, new Comparator() {		// descending order
     		public int compare(Object arg0, Object arg1) {
    			SortablePeak a = (SortablePeak) arg0;
    			SortablePeak b = (SortablePeak) arg1;
    		
    			if (a.m_i < b.m_i) 
    				return 1;
    			else if (a.m_i > b.m_i) 
    				return 0;
    			else {
    				return 0;
    			}
    		}
     	 });

     	  double tic = 0.0;
    	  for (double d : i) {
    		  tic += d;
    	  }
    	  double slope = tic / i.length;

    	  double cumInten = 0.0;
    	  double diagonal = 0.0;
    	  double xrea = 0.0;
    	  double triangle = 0.0;

    	  for (int rank = ranking.length - 1; rank >= 0; rank--) {
    	    diagonal += slope;
    	    cumInten += ranking[rank].getIntensity();
    	    xrea     += diagonal - cumInten;
    	    triangle += diagonal;
    	  }

    	  xrea = xrea / triangle;

    	  return ((double)(xrea));
    }*/
    
	
	 /**
     *  A problem with the Xrea calculation is what happens when only a few peaks dominate more than 90% of the TIC (sum of all intensity).
     *  This code removes the dominant peaks, subject to user configuration, and then computes the adjusted xrea score using the remaining peaks only.
     *  If a spectra does not have dominant peaks, the result will be the same as calc_xrea()
     */
    protected double calc_adjusted_xrea(double threshold, int max_peaks) {
    	assert(m_peaks != null && m_tic >= 0.0); // m_peaks must already be computed by xrea
    	
    	// poor spectra?
    	if (m_peaks.length < 6) 
    		return 0.0;
    	
    	// how many peaks to reach x% of tic?
    	int          cnt = 0;
    	double       cum = 0.0;
    	
    	for (int i=m_peaks.length-1; i>=0; i--) {
    		cum += m_peaks[i].getIntensity();
    		
    		if (cum >= threshold * m_tic && cnt <= max_peaks) {
    			// recompute the members with dominant peaks removed and recompute xrea
    			double sum = 0.0;
    			m_num_peaks -= cnt;	// eliminate dominant peaks
    			m_i_max = 0.0;
    	    	m_mz = new double[m_num_peaks];
    	    	m_i  = new double[m_num_peaks];
    	    	SortablePeak[] tmp  = new SortablePeak[m_num_peaks];
    	    	for (int j=m_num_peaks-1; j>=0; j--) {
    	    		SortablePeak sp = new SortablePeak(m_peaks[j].getMZ(), m_peaks[j].getIntensity());
    	    		m_mz[j]= sp.getMZ();
    	    		m_i[j] = sp.getIntensity();
    	    		tmp[j] = sp;
    	    		sum   += sp.getIntensity();
    	    		if (sp.getIntensity() > m_i_max) {
    	    			m_i_max = sp.getIntensity();
    	    		}
    	    	}
    	    	m_peaks = tmp;
    	    	Arrays.sort(m_peaks);
    	    	m_tic = sum;
    	    	
    	    	// since the members have been adjusted to exclude dominant peaks, this will not be the same as the adjacent column
    			return calc_xrea();
    		}
    		
    		cnt++;
    	}
    	
    	// no adjustment
    	return calc_xrea();
    }
    
    protected double calc_xrea() {
    	// spectra with only a few peaks are useless for this calculation...
    	if (m_num_peaks > 5) {
        	assert(m_i.length == m_mz.length);
        	
        	// xrea calculation
        	double[] relative_intensity = new double[m_mz.length];
        	double sum_so_far = 0.0;
        	double peak_area = 0.0;
        	
        	for (int j=0; j<m_mz.length; j++) {
        		sum_so_far += m_peaks[j].getIntensity();
        		relative_intensity[j] = sum_so_far / m_tic;
        		peak_area += relative_intensity[j];
        		
        		//System.err.println(peaks[j].getMZ() + " " + relative_intensity[j]);
        	}
        	int last_intensity = m_mz.length - 1;
        	double triangle_area = 0.5 * m_mz.length * relative_intensity[last_intensity];
        	
        	double alpha = relative_intensity[last_intensity] - relative_intensity[last_intensity-1];
        	return ((triangle_area - peak_area) / (triangle_area + alpha));
    	}
    	
    	return 0.0; // minimum area between curve and diagonal ie. poorest quality spectra
    }
	
	public double getRawQualityScore() {
		return calc_xrea();
	}

	public double getAdjustedQualityScore(double threshold, int max_peaks) {
		return calc_adjusted_xrea(threshold, max_peaks);
	}

    /**
     * Sorts peaks by ascending intensity (or area if thats what the data represents)
     */
    private class SortablePeak implements Comparator<SortablePeak>, Comparable<SortablePeak> {
    	private double m_mz;
    	private double m_i;
    	
    	public SortablePeak(double mz, double i) {
    		m_mz = mz;
    		m_i  = i;
    	}
    	
    	public double getMZ() {
    		return m_mz;
    	}

    	public double getIntensity() {
    		return m_i;
    	}
    	
    	public String toString() {
    		return m_mz + " " + m_i;
    	}
    	
		@Override
		public int compare(SortablePeak a, SortablePeak b) {
			assert(a != null && b != null);
			
			if (a.m_i < b.m_i) 
				return -1;
			else if (a.m_i > b.m_i) 
				return 1;
			else {
				if (a.m_mz < b.m_mz)
					return -1;
				else if (a.m_mz > b.m_mz)
					return 1;
				// else...
				return 0;
			}
		}

		@Override
		public int compareTo(SortablePeak arg0) {
			return compare(this, arg0);
		}
    	
    }
}
