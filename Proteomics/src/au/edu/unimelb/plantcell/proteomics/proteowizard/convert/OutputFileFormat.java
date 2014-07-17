package au.edu.unimelb.plantcell.proteomics.proteowizard.convert;

/**
 * Supported open-source file formats
 * @author acassin
 *
 */
public enum OutputFileFormat {
	MGF, MZML, MZXML, MZ5, ANALYST_WIFF, THERMO_RAW, UNKNOWN;

	/**
	 * Must return true if the file format can be loaded by this toolkit, false otherwise
	 * @return
	 */
	public boolean isSupportedByPlantCell() {
		return (this.equals(OutputFileFormat.MGF) || 
				this.equals(OutputFileFormat.MZML) ||
				this.equals(OutputFileFormat.MZXML));
	}

	public static OutputFileFormat[] supportedFormats() {
		return new OutputFileFormat[] { OutputFileFormat.MGF, OutputFileFormat.MZML, OutputFileFormat.MZXML };
	}
}
