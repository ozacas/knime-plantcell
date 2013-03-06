package au.edu.unimelb.plantcore.core.regions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

/**
 * Derived from <code>BlastHitRegion</code> so that it can be part of <code>AlignedRegionsAnnotation</code>,
 * as it is quite similar to hits from BLAST. Provides its own <code>getIDPrefix()</code> so that deserialisation
 * works correctly.
 * 
 * @author andrew.cassin
 *
 */
public class PFAMHitRegion extends BlastHitRegion {
	private String m_class, m_type, m_evidence;
	private int m_hmm_start, m_hmm_end;
	
	public PFAMHitRegion() {
		this(new HashMap<String,String>());
	}
	
	public PFAMHitRegion(Map<String,String> fields) {
		super(fields);
		setPFAMClass(fields.get("pfam-class"));
		setPFAMType(fields.get("pfam-type"));
		setLocationEvidence(fields.get("pfam-evidence"));
		setHMMStart(fields.get("pfam-hmm-start"));
		setHMMEnd(fields.get("pfam-hmm-end"));
	}
	
	public String getPFAMType() {
		return m_type;
	}
	
	public String getPFAMClass() {
		return m_class;
	}
	
	public String getLocationEvidence() {
		return m_evidence;
	}
	
	public int getHMMStart() {
		return m_hmm_start;
	}
	
	public int getHMMEnd() {
		return m_hmm_end;
	}
	
	public void setPFAMType(String new_type) {
		m_type = new_type;
	}
	
	public void setPFAMClass(String new_class) {
		m_class = new_class;
	}
	
	public void setLocationEvidence(String evidence) {
		m_evidence = evidence;
	}
	
	public void setHMMStart(String s) {
		try {
			m_hmm_start = Integer.valueOf(s);
		} catch (Exception e) {
			m_hmm_start = -1;
		}
	}
	
	public void setHMMEnd(String e) {
		try {
			m_hmm_end = Integer.valueOf(e);
		} catch (Exception ex) {
			m_hmm_end = -1;
		}
	}
	
	@Override
	public String getIDPrefix() {
		return "pfamreg";
	}
	
	@Override
	public void serialize(DataCellDataOutput output) throws IOException {
		super.serialize(output);
		String t = getPFAMType();
		// NB: never persist null to disk...
		output.writeUTF(t != null ? t : "");
		String c = getPFAMClass();
		output.writeUTF(c != null ? c : "");
		String e = getLocationEvidence();
		output.writeUTF(e != null ? e : "");
		output.writeInt(getHMMStart());
		output.writeInt(getHMMEnd());
	}
	
	@Override
	public RegionInterface deserialize(DataCellDataInput input) throws IOException {
		RegionInterface ri = super.deserialize(input);
		if (!(ri instanceof PFAMHitRegion))
			throw new IOException("Bad type of PFAMHitRegion: got"+ri.getClass().toString());
		PFAMHitRegion pfam_hit = (PFAMHitRegion) ri;
		pfam_hit.setPFAMType(input.readUTF());
		pfam_hit.setPFAMClass(input.readUTF());
		pfam_hit.setLocationEvidence(input.readUTF());
		pfam_hit.setHMMStart(String.valueOf(input.readInt()));
		pfam_hit.setHMMEnd(String.valueOf(input.readInt()));
		return pfam_hit;
	}
	
	@Override
	public Map<String,DataCell> asCells(String prefix) {
		Map<String,DataCell> ret = super.asCells(prefix);

		ret.put(prefix+": HMM Start", new IntCell(getHMMStart()));
		ret.put(prefix+": HMM End",   new IntCell(getHMMEnd()));
		ret.put(prefix+": Class",     new StringCell(getPFAMClass()));
		ret.put(prefix+": Type",      new StringCell(getPFAMType()));
		ret.put(prefix+": Location Evidence",  new StringCell(getLocationEvidence()));
		return ret;
	}
}
