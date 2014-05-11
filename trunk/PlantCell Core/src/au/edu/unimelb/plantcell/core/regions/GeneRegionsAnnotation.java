package au.edu.unimelb.plantcell.core.regions;

import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.def.StringCell;

import au.edu.unimelb.plantcell.core.cells.AnnotationType;

public class GeneRegionsAnnotation extends RegionsAnnotation {
	@Override
	public AnnotationType getAnnotationType() {
		return AnnotationType.GENE_PREDICTION_REGIONS;
	}
	
	@Override
	public List<DataColumnSpec> asColumnSpec(String prefix) {
		List<DataColumnSpec> ret = super.asColumnSpec(prefix);
		ret.add(new DataColumnSpecCreator(prefix+": Frame", StringCell.TYPE).createSpec());
		return ret;
	}
}
