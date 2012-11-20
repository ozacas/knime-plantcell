package au.edu.unimelb.plantcell.io.jemboss.simple;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class ACDSection {
	private String m_name;
	private final HashMap<String,String> m_props = new HashMap<String,String>();
	private final List<ACDField> m_fields = new ArrayList<ACDField>();
	
	public ACDSection(ACDStreamReader rdr) throws IOException,ParseException {
		String colon = rdr.next_token();
		if (!colon.equals(":"))
			throw new ParseException("Expected :, got "+colon, 0);
		m_name = rdr.next_token();
		rdr.next_properties(m_props);
		rdr.next_field_list(m_fields);
		String endsection = rdr.next_token();
		if (!endsection.equals("endsection"))
			throw new ParseException("Expected endsection, got "+endsection, 0);
		//colon = rdr.next_token();
		//if (!colon.equals(":"))
		//	throw new ParseException("Expected :, got "+colon, 0);
		String name = rdr.next_token();
		if (!m_name.equals(name))
			throw new ParseException("Expected "+m_name+", got "+name, 0);
	}

	public String getName() {
		return m_name;
	}
	
	public boolean hasName(String str) {
		return str.equals(getName());
	}

	public boolean hasUnalignedSequenceInput() {
		for (ACDField f : m_fields) {
			if (f.hasType("seqset") || f.hasType("seqall") || f.hasType("sequence")) {
				if (!f.hasProperty("aligned") || !f.getProperty("aligned").equals("Y"))
					return true;
			}
		}
		return false;
	}

	public boolean hasReport() {
		boolean has_report = false;
		boolean has_rformat= false;
		for (ACDField f : m_fields) {
			if (f.hasType("report")) 
				has_report = true;
			if (f.hasProperty("rformat")) 
				has_rformat = true;
		}
		return (has_report && has_rformat);
	}

	public Collection<? extends ACDField> getMandatoryFields(boolean exclude_first_in_out) {
		ArrayList<ACDField> ret = new ArrayList<ACDField>();
		boolean done_first = false;
		for (ACDField f : m_fields) {
			if (f.isMandatory()) {
				if ((exclude_first_in_out && (hasName("input") || hasName("output")) && done_first) ||
						!exclude_first_in_out) {
					ret.add(f);
				}
				done_first = true;
			} else {
				ret.add(f);
			}
		}
		return ret;
	}
	

	public Collection<? extends ACDField> getFields() {
		return getFields(false);
	}
	
	public Collection<? extends ACDField> getFields(boolean exclude_first_in_out) {
		boolean is_io = hasName("input") || hasName("output");
		List<ACDField> ret = new ArrayList<ACDField>();
		boolean first = true;
		for (ACDField f : m_fields) {
			if (is_io && first && exclude_first_in_out) {
				first = false;
				continue;
			}
			ret.add(f);
			first = false;
		}
		return ret;
	}
	
	public boolean hasPlot() {
		boolean has_graph= false;
		for (ACDField f : m_fields) {
			if (f.hasName("graph") && (f.hasType("xygraph") || f.hasType("graph")))
				has_graph= true;
		}
		return (has_graph);
	}

}
