package au.edu.unimelb.plantcell.io.ws.phobius;

import java.io.IOException;
import java.io.Serializable;

/**
 * Implements all that is required by EHCache to store and retrieve a phobius record, simple-minded
 * implementation (for now). Only positive results (ie. non errors) are cached
 * 
 * @author andrew.cassin
 *
 */
public class CacheablePhobiusRecord implements Serializable {

	/**
	 * for Serializable classes...
	 */
	private static final long serialVersionUID = 2210390732281197583L;

	private String m_seq;		// primary key for the cache
	private String m_id;		// EBI-assigned Job ID
	private String m_raw;		// raw (textual string) results from Phobius@EBI
	
	
	public CacheablePhobiusRecord(String seq, String job_id, String raw) {
		assert(seq != null && seq.length() > 0 &&
				job_id != null && job_id.length() > 0 &&
				raw != null && raw.length() > 0);
		m_seq = seq;
		m_id  = job_id;		// job id comes from EBI: but NOT suitable as a cache key
		m_raw = raw;
	}
	
	/**
	 * Cache constructor which constructs a cache object from the serialised form (from the cache).
	 * Must match <code>toString()</code>
	 * 
	 * @param seq
	 * @param serialised_data
	 */
	public CacheablePhobiusRecord(String seq, String serialised_data) {
		assert(seq != null && serialised_data != null && serialised_data.length() > 0);
		
		m_seq = seq;
		int idx = serialised_data.indexOf('\n');
		assert(idx >= 0);
		m_id  = serialised_data.substring(0, idx);
		m_raw = serialised_data.substring(idx+1);
	}
	
	public static final String makeKey(String seq) {
		return seq;
	}
	
	/**
	 * Must always correspond to <code>makeKey()</code>
	 * @return
	 */
	public final String getKey() {
		return makeKey(m_seq);
	}
	
	public final String getSequence() {
		return m_seq;
	}
	
	public final String getPhobiusResult() {
		return m_raw;
	}
	
	public final String getJobID() {
		return m_id;
	}
	
	/**
	 * The key (sequence) is excluded from this output for the cache
	 */
	@Override
	public String toString() {
		return m_id + "\n" + m_raw;
	}
	
	/************* SERIALIZABLE INTERFACE METHODS **********************/
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeUTF(m_seq);
		out.writeUTF(m_raw);
		out.writeUTF(m_id);
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		m_seq = in.readUTF();
		m_raw = in.readUTF();
		m_id  = in.readUTF();
		assert(m_seq != null && m_raw != null && m_id != null);
	}
}