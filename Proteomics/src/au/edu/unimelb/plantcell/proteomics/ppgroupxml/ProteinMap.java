package au.edu.unimelb.plantcell.proteomics.ppgroupxml;

import java.util.HashMap;

public class ProteinMap {
	private final HashMap<String,String> m_proteins = new HashMap<String,String>();
	
	public ProteinMap(final PeptideMap pm) {
		assert(pm != null);
	}


	public void add(String id, String sequence) {
		m_proteins.put(id, sequence);
	}
}
