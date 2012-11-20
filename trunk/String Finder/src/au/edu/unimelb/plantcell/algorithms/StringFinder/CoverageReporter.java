package au.edu.unimelb.plantcell.algorithms.StringFinder;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;

public class CoverageReporter implements MatchReporter {

	public CoverageReporter() {	
	}
	
	@Override
	public DataCell report(FindGlobalNodeModel m, DataCell str_cell) throws Exception {
		DenseBitVector bv = m.getResultsBitVector();
		if (bv == null)
			return DataType.getMissingCell();
		long len    = bv.length();
		long n_bits = bv.cardinality();
		return new DoubleCell(100.0*n_bits/len);
	}

}
