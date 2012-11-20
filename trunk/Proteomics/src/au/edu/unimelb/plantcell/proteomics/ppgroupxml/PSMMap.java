package au.edu.unimelb.plantcell.proteomics.ppgroupxml;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps between spectra and peptide identifications. Deliberately hides all
 * state from caller to prevent abstraction problems.
 * 
 * @author andrew.cassin
 *
 */
public class PSMMap {
	private final Map<String,String> m_psm = new HashMap<String,String>();
	
	public void add(String psm_id, String knime_peptide_row_id) {
		m_psm.put(psm_id, knime_peptide_row_id);
	}

	/**
	 * Lookup a particular PSM id and return the corresponding knime peptide row
	 */
	public String lookup(final String psm_id) {
		return m_psm.get(psm_id);
	}
}
