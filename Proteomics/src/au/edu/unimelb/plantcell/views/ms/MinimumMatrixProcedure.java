package au.edu.unimelb.plantcell.views.ms;

import org.la4j.matrix.functor.MatrixProcedure;


public class MinimumMatrixProcedure implements MatrixProcedure {
	public double min = Double.POSITIVE_INFINITY;
	@Override
	public void apply(int arg0, int arg1, double arg2) {
		if (arg2 < min)
			min = arg2;
	}
	
};