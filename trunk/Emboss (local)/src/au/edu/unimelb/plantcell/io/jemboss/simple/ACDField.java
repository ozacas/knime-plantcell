package au.edu.unimelb.plantcell.io.jemboss.simple;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class ACDField {
	private String m_name;
	private String m_type;
	private final HashMap<String,String> m_props = new HashMap<String,String>();
	
	public ACDField(ACDStreamReader rdr) throws IOException, ParseException {
		m_type = rdr.next_token();
		String colon = rdr.next_token();
		if (!colon.equals(":")) 
			throw new ParseException("Expected :, but got "+colon, 0);
		m_name = rdr.next_token();
		
		rdr.next_properties(m_props);
	}
	
	public ACDField(String type, String name, ACDStreamReader rdr) throws IOException, ParseException {
		m_type = type;
		m_name = name;
		//Logger.getAnonymousLogger().info(name + " = "+type);
		rdr.next_properties(m_props);
		//Logger.getAnonymousLogger().info("Leaving field"+rdr.peek_token());
	}

	public ACDField(ACDField clone_me) {
		m_name = new String(clone_me.getName());
		m_type = new String(clone_me.getType());
		m_props.putAll(clone_me.m_props);
	}
	
	protected String getType() {
		return m_type;
	}

	public boolean hasType(String str) {
		return str.equals(m_type);
	}

	public boolean isMandatory() {
		if (m_props.containsKey("parameter") && m_props.get("parameter").equals("Y"))
			return true;
		
		return false;
	}
	
	@Override 
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(m_name+":"+m_type+"\n");
		for (String key : m_props.keySet()) {
			sb.append("\t"+key+"="+m_props.get(key)+"\n");
		}
		return sb.toString();
	}

	public boolean hasProperty(String str) {
		return m_props.containsKey(str);
	}

	public String getProperty(String str) {
		return m_props.get(str);
	}

	public boolean hasName(String str) {
		return m_name.equals(str);
	}
	
	public String getName() {
		return m_name;
	}
	
	public boolean isSequence() {
		return (m_type.equals("sequence") || 
				m_type.equals("seqset") || 
				m_type.equals("seqall"));
	}
	
	protected Map<String,String> getProperties() {
		return m_props;
	}
}
