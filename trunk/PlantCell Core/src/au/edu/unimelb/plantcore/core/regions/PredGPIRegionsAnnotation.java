package au.edu.unimelb.plantcore.core.regions;

import au.edu.unimelb.plantcell.core.cells.AnnotationType;

/**
 * An annotation of this type typically has one scored region in it: that of the omega site. It could
 * also be empty (denoting no prediction of a GPI anchor)
 * @author andrew.cassin
 *
 */
public class PredGPIRegionsAnnotation extends RegionsAnnotation {
	@Override
	public AnnotationType getAnnotationType() {
		return AnnotationType.PREDGPI_REGIONS;
	}
	
	/**
	 * Co-ordinates of predictions start at 1 for TMHMM
	 */
	@Override
	public int getOffset() {
		return 1;
	}
}
