package au.edu.unimelb.plantcell.io.jemboss.simple;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;



public class ACDStreamReader {
	private Reader m_rdr;
	private String next_token;
	
	public ACDStreamReader(Reader rdr) {
		assert(rdr != null);
		m_rdr = rdr;
		next_token = null;
	}
	
	public String next_token() throws IOException {
		if (next_token != null) {
			String ret = next_token;
			next_token = null;
			//Logger.getAnonymousLogger().info("next=null, current: "+ret);
			return ret;
		}
		
		StringBuilder sb = new StringBuilder();
		int           ch = skip_whitespace();
	
		do {
			if (ch < 0 || Character.isWhitespace(ch)) {
				next_token = null;
				break;
			}
			if (ch == ':' && sb.length() > 0) {
				next_token = ":";
				break;
			}
			if (ch == '[' && sb.length() > 0) {
				next_token = "[";
				break;
			}
			sb.append((char) ch);
			
			if (ch == '\"') {
				next_token = null;
				break;
			}
		} while ((ch = m_rdr.read()) > 0);
		
		
		// ensure null on eof
		if (ch < 0 && sb.length() < 1) {
			//Logger.getAnonymousLogger().info("EOF!");
			return null;
		}
		
		//Logger.getAnonymousLogger().info("next: "+next_token+", current: "+sb.toString());
		return sb.toString();
	}
	
	private int skip_whitespace() throws IOException {
		int ch;
		
		while ((ch = this.m_rdr.read()) > 0 && Character.isWhitespace(ch)) 
				;
	
		if (ch == '#') {	// skip comment
			while ((ch = m_rdr.read()) > 0) {
				//Logger.getAnonymousLogger().info(""+(char) ch);
				if (ch == '\n' || ch == '\r') {
					while ((ch = m_rdr.read()) > 0 && Character.isWhitespace(ch))
						;
					break;
				}
			}
		}
		
		return ch;	
	}

	public String[] next_keyvalue_pair() throws IOException, ParseException {
		String[] ret = new String[2];
		ret[0] = next_token();
		//Logger.getAnonymousLogger().info(ret[0]);
		if (ret[0].equals("]")) {	// end of key value pairs?
			return null;
		}
		String colon = next_token();
		if (!colon.equals(":")) 
			throw new ParseException("Expected :, got "+colon, 0);
		ret[1] = next_literal();
		String close_quote = next_token();
		if (!close_quote.equals("\"")) {
			throw new ParseException("Expected close quote: "+close_quote, 0);
		}
		return ret;
	}
	
	public String next_literal() throws IOException, ParseException {
		String token = next_token();
		if (!token.equals("\""))
			throw new ParseException("Expected opening quote, got "+token, 0);
		StringBuilder sb = new StringBuilder();
		int ch;
		while ((ch = m_rdr.read()) > 0) {
			if (ch == '\"') {			// closing quote?
				next_token = "\"";
				break;
			}
			sb.append((char) ch);
		}
		
		return sb.toString();
	}

	public void next_properties(HashMap<String, String> props) throws IOException,ParseException {
		String open_bracket = next_token();
		if (!open_bracket.equals("[")) {
			throw new ParseException("Expected [, got "+open_bracket, 0);
		}
		String[] pair;
		props.clear();
		while ((pair = next_keyvalue_pair()) != null) {
			props.put(pair[0], pair[1]);
		}
		
		//Logger.getAnonymousLogger().info("leaving next_properties, next="+next_token); 
	}

	public void next_field_list(List<ACDField> m_fields) throws ParseException, IOException {
		m_fields.clear();
		
		do {
			String type = next_token();
			if (type.equals("endsection") || type == null) {
				next_token = type;
				break;
			}
			String colon= next_token();
			if (!colon.equals(":"))
				throw new ParseException("Expected :, got "+colon,0);
			String name = next_token();
			ACDField  f = new ACDField(type, name, this);
		
			m_fields.add(f);
		} while (true);
		//Logger.getAnonymousLogger().info("leaving next_field_list, next="+next_token);
	}

	public void read_section_list(List<ACDSection> m_sections) throws ParseException, IOException {
		String section;
		
		while ((section = next_token()) != null) {
			if (!section.equals("section"))
				throw new ParseException("Expected section, got "+section, 0);
			ACDSection s = new ACDSection(this);
			m_sections.add(s);
		}
		
		//Logger.getAnonymousLogger().info("leaving read_section_list, next="+next_token);
	}

	public String peek_token() {
		return next_token;
	}
}
