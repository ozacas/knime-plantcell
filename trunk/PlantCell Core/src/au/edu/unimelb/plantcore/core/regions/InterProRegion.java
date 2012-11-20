package au.edu.unimelb.plantcore.core.regions;

import java.io.IOException;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.def.StringCell;

/**
 * Tracks a single InterPro (v4) site identification 
 * @author andrew.cassin
 *
 */
public class InterProRegion extends ScoredRegion {
	private String m_db, m_db_id;
	private String m_evidence;
	private String m_status;
	private String m_name;
	private String m_interpro_id;
	
	public InterProRegion(String db, String db_id, int start, int end) {
		super(start, end, db_id);
		setDatabase(db);
		setDatabaseID(db_id);
		setScore(Double.NaN);		// score is not known
	}

	public void setInterProID(String m_interpro_id) {
		this.m_interpro_id = m_interpro_id;
	}

	public String getInterProID() {
		return m_interpro_id;
	}

	/**
	 * @param m_name the name to set
	 */
	public void setName(String m_name) {
		this.m_name = m_name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return m_name;
	}

	/**
	 * @param s the status to set
	 */
	public void setStatus(String s) {
		this.m_status = s;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return m_status;
	}

	/**
	 * @param m_evidence the evidence to set
	 */
	public void setEvidence(String evidence) {
		this.m_evidence = evidence;
	}

	/**
	 * @return the evidence
	 */
	public String getEvidence() {
		return m_evidence;
	}

	/**
	 * @param m_db the db to set
	 */
	public void setDatabase(String m_db) {
		this.m_db = m_db;
	}

	/**
	 * @return the database
	 */
	public String getDatabase() {
		return m_db;
	}

	/**
	 * @param m_db_id database ID to set
	 */
	public void setDatabaseID(String m_db_id) {
		this.m_db_id = m_db_id;
	}

	/**
	 * @return the database ID
	 */
	public String getDatabaseID() {
		return m_db_id;
	}
	
	@Override
	public String getIDPrefix() {
		return "ipro";
	}
	
	@Override
	public void serialize(DataCellDataOutput output) throws IOException {
		super.serialize(output);
		writeSafeString(output, getDatabase());
		writeSafeString(output, getDatabaseID());
		writeSafeString(output, getEvidence());
		writeSafeString(output, getStatus());
		writeSafeString(output, getName());
		writeSafeString(output, getInterProID());
	}
	
	private void writeSafeString(DataCellDataOutput output, String s) throws IOException {
		if (s == null)
			output.writeUTF("");
		else 
			output.writeUTF(s);
	}

	@Override 
	public InterProRegion deserialize(DataCellDataInput input) throws IOException {
		super.deserialize(input);
		setDatabase(input.readUTF());
		setDatabaseID(input.readUTF());
		setEvidence(input.readUTF());
		setStatus(input.readUTF());
		setName(input.readUTF());
		setInterProID(input.readUTF());
		return this;
	}
	
	@Override
	public Map<String,DataCell> asCells(String prefix) {
		Map<String,DataCell> ret = super.asCells(prefix);
		ret.put(prefix+": InterPro ID", new StringCell(getInterProID()));
		ret.put(prefix+": Evidence", new StringCell(getEvidence()));
		ret.put(prefix+": Status", new StringCell(getStatus()));
		ret.put(prefix+": Database", new StringCell(getDatabase()));
		ret.put(prefix+": Database ID", new StringCell(getDatabaseID()));
		ret.put(prefix+": Name", new StringCell(getName()));
		return ret;
	}
}
