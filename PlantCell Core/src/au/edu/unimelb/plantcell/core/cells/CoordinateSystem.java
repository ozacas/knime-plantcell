package au.edu.unimelb.plantcell.core.cells;

/**
 * 
 * @author andrew.cassin
 *
 */
public enum CoordinateSystem {
	// offset from the start of the sequence (first residue is always 0)
	OFFSET_FROM_START {
		public String toString() {
			return "Start/End";			// two integral values describing offset from start of string (0 relative)
		}
	},
	
}
