package au.edu.unimelb.plantcell.statistics.correlation;

public class GrubbOutlierTest {

	public GrubbOutlierTest() {
	}
	
        // Grubbs test for outliers
        public static boolean isOutlier(double val, double[] a, double[] tDis) {
            if (findTestStat(val, a) > findCriticalValue(a.length, tDis)) {
            return true;
            }
            else {return false;}
        }

        // returns the index of an outlier in an array
        // returns -1 if no outliers.
        public static int findOutlier(double[] a, double[] tDis) {
            int greatest = 0;
            for (int i = 0; i < a.length; i++) {
            	
                if (findTestStat(a[greatest], a) < findTestStat(a[i], a)) {
                    greatest = i;
                }
            }
            if(isOutlier(a[greatest], a, tDis)) {
                return greatest;
            }
            else {
            	return -1;
            }
        }
           
        //find a mean of an array of numbers
        public static double findMean(double[] a) {
            int entries = a.length;
            double sum = 0.0;
            for (Number n : a){
                sum += n.doubleValue();
            }
            return sum/entries;
        }
       
        //Find standard deviation
        public static double findSD(double[] a) {
            int entries = a.length;
            Number mean;
            double sumOfSquares = 0;
            mean = findMean(a);
            for (Number n : a) {
            	double d_n    = n.doubleValue();
            	double d_mean = mean.doubleValue();
                sumOfSquares += (d_n-d_mean)*(d_n-d_mean);
            }
            return Math.sqrt(sumOfSquares/entries);
        }
       
        // Find critical value to compare test statistic to
        private static double findCriticalValue(double v, double[] tDis) {
            Number part1 = (v-1)/Math.sqrt(v);
            int idx = (int) v;
            if (idx >= tDis.length)			// CODE DID NOT CHECK IDX BOUNDS, acassin@unimelb.edu.au, 14th Nov 2012
            	idx = tDis.length - 1;
            Number part2 = Math.sqrt((tDis[idx]*tDis[idx])/(v-2+tDis[idx]*tDis[idx]));
            return part1.doubleValue()*part2.doubleValue();
        }
       
        // Find test statistic
        public static double findTestStat(double val, double[] a) {
            return (Math.abs(findMean(a)-val))/findSD(a);
        }
       
        // t-distribution tables
        public static final double[] T_800 = new double[] {     0,
                                            3.078,
                                            1.886,
                                            1.638,
                                            1.533,
                                            1.476,
                                            1.440,
                                            1.415,
                                            1.397,
                                            1.383,
                                            1.372,
                                            1.363,
                                            1.356,
                                            1.350,
                                            1.345,
                                            1.341,
                                            1.337,
                                            1.333,
                                            1.330,
                                            1.328,
                                            1.325,
                                            1.323,
                                            1.321,
                                            1.319,
                                            1.318,
                                            1.316,
                                            1.315,
                                            1.314,
                                            1.313,
                                            1.311,
                                            1.310 };
       
        public static final double[] T_900 = new double[] {   0,
                                            6.314,
                                            2.920,
                                            2.353,
                                            2.132,
                                            2.015,
                                            1.943,
                                            1.895,
                                            1.860,
                                            1.833,
                                            1.812,
                                            1.796,
                                            1.782,
                                            1.771,
                                            1.761,
                                            1.753,
                                            1.746,
                                            1.740,
                                            1.734,
                                            1.729,
                                            1.725,
                                            1.721,
                                            1.717,
                                            1.714,
                                            1.711,
                                            1.708,
                                            1.706,
                                            1.703,
                                            1.701,
                                            1.699,
                                            1.697 };
               
        public static final double[] T_950 = new double[] {    0,
                                            12.710,
                                            4.303,
                                            3.182,
                                            2.776,
                                            2.571,
                                            2.447,
                                            2.365,
                                            2.306,
                                            2.262,
                                            2.228,
                                            2.201,
                                            2.179,
                                            2.160,
                                            2.145,
                                            2.131,
                                            2.120,
                                            2.110,
                                            2.101,
                                            2.093,
                                            2.086,
                                            2.080,
                                            2.074,
                                            2.069,
                                            2.064,
                                            2.060,
                                            2.056,
                                            2.052,
                                            2.048,
                                            2.045,
                                            2.042 };
       
        public static final double[] T_980 =  new double[] {   0,
                                               31.820,
                                            6.965,
                                            4.541,
                                            3.747,
                                            3.365,
                                            3.143,
                                            2.998,
                                            2.896,
                                            2.821,
                                            2.764,
                                            2.718,
                                            2.681,
                                            2.650,
                                            2.624,
                                            2.602,
                                            2.583,
                                            2.567,
                                            2.552,
                                            2.539,
                                            2.528,
                                            2.518,
                                            2.508,
                                            2.500,
                                            2.492,
                                            2.485,
                                            2.479,
                                            2.473,
                                            2.467,
                                            2.462,
                                            2.457 };
       
        public static final double[] T_990 = new double[] {    0,
                                               63.660,
                                            9.925,
                                            5.841,
                                            4.604,
                                            4.032,
                                            3.707,
                                            3.499,
                                            3.355,
                                            3.250,
                                            3.169,
                                            3.106,
                                            3.055,
                                            3.012,
                                            2.977,
                                            2.947,
                                            2.921,
                                            2.898,
                                            2.878,
                                            2.861,
                                            2.845,
                                            2.831,
                                            2.819,
                                            2.807,
                                            2.797,
                                            2.787,
                                            2.779,
                                            2.771,
                                            2.763,
                                            2.756,
                                            2.750 };
       
        public static final double[] T_995 = new double [] {   0,
                                               127.300,
                                            14.090,
                                            7.453,
                                            5.598,
                                            4.773,
                                            4.317,
                                            4.029,
                                            3.833,
                                            3.690,
                                            3.581,
                                            3.497,
                                            3.428,
                                            3.372,
                                            3.326,
                                            3.286,
                                            3.252,
                                            3.222,
                                            3.197,
                                            3.174,
                                            3.153,
                                            3.135,
                                            3.119,
                                            3.104,
                                            3.091,
                                            3.078,
                                            3.067,
                                            3.057,
                                            3.047,
                                            3.038,
                                            3.030 };
            
    
}


