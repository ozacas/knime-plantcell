package au.edu.unimelb.plantcell.proteomics.views;

import org.la4j.matrix.functor.MatrixProcedure;

public class MaximumMatrixProcedure implements MatrixProcedure {
	public double max = Double.NEGATIVE_INFINITY;
	@Override
	public void apply(int arg0, int arg1, double arg2) {
		if (arg2 > max)
			max = arg2;
	}
	
};