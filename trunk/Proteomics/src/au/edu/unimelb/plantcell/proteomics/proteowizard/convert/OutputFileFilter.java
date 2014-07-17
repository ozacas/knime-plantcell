package au.edu.unimelb.plantcell.proteomics.proteowizard.convert;

import java.io.File;

public interface OutputFileFilter {
	/**
	 * Implementors must return true if the file is to be considered acceptable or false otherwise
	 * @param f the file to be processed (never null)
	 * @param data_format the expected data format which the file is expected to contain. May be wrong if the user is playing games with filenames
	 */
	public boolean accept(final File f, final OutputFileFormat expected_data_format_for_f);
}
