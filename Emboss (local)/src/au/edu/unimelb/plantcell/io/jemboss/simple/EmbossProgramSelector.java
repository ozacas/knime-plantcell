package au.edu.unimelb.plantcell.io.jemboss.simple;

public interface EmbossProgramSelector {

	/**
	 * Returns true if the program is acceptable, false otherwise
	 */
	public boolean accept(ACDApplication appl);
}
