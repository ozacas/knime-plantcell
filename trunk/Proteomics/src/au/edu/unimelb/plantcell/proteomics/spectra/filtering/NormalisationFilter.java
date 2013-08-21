package au.edu.unimelb.plantcell.proteomics.spectra.filtering;

import org.expasy.jpl.core.ms.spectrum.PeakList;
import org.expasy.jpl.core.ms.spectrum.PeakListImpl;
import org.expasy.jpl.core.ms.spectrum.filter.AbstractPeakListFilter;

/**
 * Provide normalisation methods for spectra: fraction of biggest peak, multiple of smallest peak and total peak intensity
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class NormalisationFilter extends AbstractPeakListFilter {
	@SuppressWarnings("unused")
	private boolean use_biggest, use_smallest, use_total;
	
	public NormalisationFilter(String normalisation_method) {
		assert(normalisation_method != null && normalisation_method.length() > 0);
		use_biggest = false;
		use_smallest= false;
		use_total   = true;
		normalisation_method = normalisation_method.toLowerCase();
		if (normalisation_method.indexOf("highest intensity") >= 0) {
			use_biggest = true; use_total = false;
		} else if (normalisation_method.indexOf("lowest intensity") >= 0) {
			use_smallest = true; use_total = false;
		} else {
			// assume use total intensity normalisation
		}
	}
	
	@Override
	public boolean isPassingFilter(PeakList arg0, int arg1) {
		return true;	// all peaks are kept with this filter
	}

	@Override
	public PeakList transform(final PeakList input) {
		if (input.hasIntensities()) {
			double val = 1.0;
			if (use_biggest)
				val = find_biggest(input);
			else if (use_smallest)
				val = find_smallest(input);
			else 
				val = find_total(input);
			return transform(val, input);
		} else {
			return PeakListImpl.emptyInstance();
		}
	}

	private double find_biggest(final PeakList input) {
		double max = 0.0;
		for (int i=0; i<input.size(); i++) {
			if (input.getIntensityAt(i) > max) {
				max = input.getIntensityAt(i);
			}
		}
		return max;
	}
	
	private double find_smallest(final PeakList input) {
		double min = Double.MAX_VALUE;
		for (int i=0; i<input.size(); i++) {
			if (input.getIntensityAt(i) < min) {
				min = input.getIntensityAt(i);
			}
		}
		return min;
	}
	
	private double find_total(final PeakList input) {
		double sum = 0.0;
		for (int i=0; i<input.size(); i++) {
			sum += input.getIntensityAt(i);
		}
		return sum;
	}

	private PeakList transform(double factor, PeakList input) {
		assert(factor > 0.0);
		double[] new_intensities = input.getIntensities();
		for (int i=0; i<new_intensities.length; i++) {
			new_intensities[i] /= factor;
		}
		PeakList ret = new PeakListImpl.Builder(input).intensities(new_intensities).build();
		return ret;
	}
}
