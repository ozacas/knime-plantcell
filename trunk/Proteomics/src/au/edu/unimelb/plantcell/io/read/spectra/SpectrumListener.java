package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;

/**
 * When the caller wants to be notified of each spectrum being processed (as its being read) it should
 * implement this interface and call the appropriate constructor of the data processor class. We use
 * this listener to build the model data structure for the base peak view based on data coming from the mzML file(s) being loaded.
 * 
 * @author andrew.cassin
 *
 */
public interface SpectrumListener {
	
	/**
	 * called when a new file is started. Guaranteed to be the first listener method called for each file. Its possible
	 * that no other method gets called (eg. IO problem)
	 */
	public void newFile(final File f);
	
	/**
	 * called only if the user wants chromatograms loaded (performance penalty) and if present in the input file
	 */
	public void chromatogram(final String title, double[] rt, double[] intensity);
	
	/**
	 * called when a new spectra is loaded (eg. <code>&lt;spectrum&gt;</code> element in mzML)
	 */
	public void spectra(final int msLevel, final double rt_in_sec, 
						final double base_peak_mz, final double base_peak_intensity, 
						final String spectra_id, final String title, final String scan_type);
	
	/**
	 * called for each precursor ion
	 */
	public void precursor(int charge, int msLevel, double mz, double intensity, double rt);

	/**
	 * Where the spectra data is *loaded* from the file, this method is called. Guaranteed non-null, but you must
	 * not assume that there are peaks present in the input data
	 * 
	 * @param bpl
	 */
	public void peakList(final BasicPeakList bpl);
}
