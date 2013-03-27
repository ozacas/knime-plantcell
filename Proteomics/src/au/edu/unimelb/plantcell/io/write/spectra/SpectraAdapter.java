package au.edu.unimelb.plantcell.io.write.spectra;

import au.edu.unimelb.plantcell.io.read.spectra.MGFSpectraCell;
import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;
import au.edu.unimelb.plantcell.io.read.spectra.mzMLSpectraCell;

/**
 * Handles dealing with the vaguaries of each input format, from the perspective of writing the data.
 * Enables the writing code in the node model to be cleaner.
 * 
 * @author andrew.cassin
 *
 */
public class SpectraAdapter {
	private SpectraValue m_sdi;
	private boolean m_from_mgf;
	private boolean m_from_mzml;
	
	public SpectraAdapter(SpectraValue sdi) {
		assert(sdi != null);
		m_from_mgf = false;
		m_from_mzml= false;
		if (sdi instanceof MGFSpectraCell) {
			m_from_mgf = true;
		} else if (sdi instanceof mzMLSpectraCell) {
			m_from_mzml = true;
		}
		m_sdi = sdi;
	}
	
	public boolean hasRetentionTime() {
		return false;
	}
	
}
