package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forester.phylogeny.PhylogenyNode;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;



public class PairwiseHeatMatrix {
	private final Map<Pair, Double> matrix = new HashMap<Pair,Double>();
	private double max = Double.NEGATIVE_INFINITY;
	
	public PairwiseHeatMatrix(final BufferedDataTable in, int a_idx, int b_idx, int heat_idx) throws InvalidSettingsException {
		assert(in != null);
		
		if (a_idx < 0 || b_idx < 0 || heat_idx < 0)
			throw new InvalidSettingsException("Cannot find pairwise heat columns - reconfigure?");
		
		// build heat data
		for (DataRow r : in) {
			 DataCell a_cell = r.getCell(a_idx);
			 DataCell b_cell = r.getCell(b_idx);
			 DataCell heat_cell = r.getCell(heat_idx);
			 
			 if (a_cell.isMissing() || b_cell.isMissing() || heat_cell.isMissing()) {
				 throw new InvalidSettingsException("Pairwise heat data cannot be missing!");
			 }
			 if (!(heat_cell instanceof DoubleValue)) {
				 throw new InvalidSettingsException("Heat column must be numeric!");
			 }
			 
			 Double val = Double.valueOf(((DoubleValue)heat_cell).getDoubleValue());
			 if (max < val.doubleValue()) {
				 max = val.doubleValue();
			 }
			 Pair p = new Pair(a_cell.toString(), b_cell.toString());
			 if (matrix.containsKey(p)) {
				 throw new InvalidSettingsException("Multiple datapoints for pair: "+a_cell.toString() + ", "+ b_cell.toString());
			 }
			 matrix.put(p, val);
		}
		
		if (Math.abs(max) < 0.00000000000000000001)
			throw new InvalidSettingsException("Maximum datapoint is too close to zero!");
	}
	 
	/**
	 *  Always returns a value between [0,1] since it divides by the maximum value. Exception: Double.NaN
	 *  is returned if a pair is specified which is not present in the matrix.
	 */
	public double getHeat(final Pair p) {
		Double d = matrix.get(p);
		if (d == null)
			return Double.NaN;
		return d.doubleValue() / max;
	}
	
	/**
	 * Given the two phylogeny nodes, this routine calculates heat from the pairwise matrix
	 * and then returns the result scaled to the maximum datapoint.
	 * 
	 * @param a
	 * @param b
	 * @return Double.NaN if datapoint not present, otherwise as per specification
	 */
	public double getHeat(final PhylogenyNode a, final PhylogenyNode b) {
		assert(a != null && b != null);
		
		Pair p = new Pair(a.getName(), b.getName());
		return getHeat(p);
	}
	
	public class Pair {
		public String a, b;
		
		public Pair(final String a, final String b) {
			assert(a != null && b != null);
			this.a = a;
			this.b = b;
		}
		
		public boolean hasEndpoint(String name) {
			return (a.equals(name) || b.equals(name));
		}
		
		public int hashCode() {
			return a.hashCode() * b.hashCode();
		}
		
		public boolean equals(Object o) {
			if (o == null || !(o instanceof Pair))
				return false;
			Pair y = (Pair) o;
			return ((this.a.equals(y.a) && this.b.equals(y.b)) ||
					(this.b.equals(y.a) && this.a.equals(y.b)));
		}
	}

	
	public List<Pair> findAll(String name) {
		List<Pair> ret = new ArrayList<Pair>();
		for (Pair p : matrix.keySet()) {
			if (p.hasEndpoint(name))
				ret.add(p);
		}
		return ret;
	}

	/**
	 * Return a list of heat values for the chosen set of pairs
	 * @param pairs return result is guaranteed to be the same length as input vector (in same order)
	 * @return Double.NaN if a pair is not in the matrix, otherwise the scale
	 */
	public List<Double> findAllValues(List<Pair> pairs) {
		List<Double> ret = new ArrayList<Double>();
		for (Pair p : pairs) {
			ret.add(getHeat(p));
		}
		return ret;
	}
}
