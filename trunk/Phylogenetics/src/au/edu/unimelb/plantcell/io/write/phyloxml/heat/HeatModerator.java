package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;

public abstract class HeatModerator {
	public abstract double moderate(final List<Double> l);
	
	protected Double average(final List<Double> vec) {
		DescriptiveStatistics ds = new DescriptiveStatistics(); 
		for (Double d : vec) {
			ds.addValue(d);
		}
		return ds.getMean();
	}
	
	protected Double maximum(final List<Double> vec) {
		DescriptiveStatistics ds = new DescriptiveStatistics(); 
		for (Double d : vec) {
			ds.addValue(d);
		}
		return ds.getMax();
	}
	
	protected Double minimum(final List<Double> vec) {
		DescriptiveStatistics ds = new DescriptiveStatistics(); 
		for (Double d : vec) {
			ds.addValue(d);
		}
		return ds.getMin();
	}
	
	protected Double median(final List<Double> vec) {
		Median m = new Median();
		double[] v = new double[vec.size()];
		for (int i=0; i<v.length; i++) {
			v[i] = vec.get(i);
		}
		m.setData(v);
		return m.evaluate();
	}
}
