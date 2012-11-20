package au.edu.unimelb.plantcell.proteomics.ppgroupxml;

import java.util.HashMap;

import org.knime.core.node.InvalidSettingsException;

public class PeptideMap {
		private final HashMap<String,String> m_pep_map = new HashMap<String,String>();
		
		public PeptideMap(PSMMap psm) {
			assert(psm != null);
		}

		public int count() {
			return m_pep_map.size();
		}

		public void add(String id, String psm_ids_as_csv) throws InvalidSettingsException {
			if (!id.startsWith("PEPTIDE:")) {
				throw new InvalidSettingsException("Peptide entry does not start with PEPTIDE: "+id);
			}
			String id_new = id.substring(8);
			m_pep_map.put(id_new, psm_ids_as_csv);
		}
}
