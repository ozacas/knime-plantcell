package au.edu.unimelb.plantcell.statistics;

import java.util.Arrays;

import org.apache.commons.math3.stat.ranking.RankingAlgorithm;

/**
 * Used only when "MINIMUM CONSECUTIVE" ties handling is chosen: given the data
 * 
 * 1.0, 2.0, 3.0, 2.0, 2.0, 4.0
 * will output the ranks:
 * 1, 2, 3, 2, 2, 4
 * 
 * in other words the rank values are guaranteed consecutive (from 1) with the minimum value being chosen for ties.
 * This implementation does not support any NaN strategies at the moment (I want to use the superclass implementations but cant...)
 * 
 * @author andrew.cassin
 *
 */
public class MyNaturalRanking implements RankingAlgorithm {

	public MyNaturalRanking() {
		
	}
	

	@Override
	public double[] rank(double[] data) {
		if (data == null || data.length < 1)
			return new double[] {};
		
		IntDoublePair[] vec = new IntDoublePair[data.length];
		for (int i=0; i<vec.length; i++) {
			vec[i] = new IntDoublePair(data[i], i);
		}
		Arrays.sort(vec);
		
		double[] out = new double[vec.length];
		double last_rank = 0.0;
		double last_value = Double.NEGATIVE_INFINITY;
		for (int i=0; i<vec.length; i++) {
			IntDoublePair idp = vec[i];
			
			if (idp.getValue() == last_value) {
				out[idp.getPosition()] = last_rank;
			} else {
				last_value = idp.getValue();
				last_rank += 1.0d;
				out[idp.getPosition()] = last_rank;
			}
		}
		return out;
	}
	
	/**
	 * Copied from apache commons math3 source... ugh yuk!
	 * @author Apache Commons Math 3
	 *
	 */
	private static class IntDoublePair implements Comparable<IntDoublePair>  {

        /** Value of the pair */
        private final double value;

        /** Original position of the pair */
        private final int position;

        /**
         * Construct an IntDoublePair with the given value and position.
         * @param value the value of the pair
         * @param position the original position
         */
        public IntDoublePair(double value, int position) {
            this.value = value;
            this.position = position;
        }

        /**
         * Compare this IntDoublePair to another pair.
         * Only the <strong>values</strong> are compared.
         *
         * @param other the other pair to compare this to
         * @return result of <code>Double.compare(value, other.value)</code>
         */
        public int compareTo(IntDoublePair other) {
            return Double.compare(value, other.value);
        }

        /**
         * Returns the value of the pair.
         * @return value
         */
        public double getValue() {
            return value;
        }

        /**
         * Returns the original position of the pair.
         * @return position
         */
        public int getPosition() {
            return position;
        }
    }

}
