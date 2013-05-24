package au.edu.unimelb.plantcell.io.ws.quickgo;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.biojava.bio.seq.io.ParseException;
import org.biojava.ontology.Synonym;
import org.biojava.ontology.obo.OboFileEventListener;
import org.biojava.ontology.obo.OboFileHandler;
import org.biojava.ontology.obo.OboFileParser;

public class MyOBOParser extends OboFileParser {

		List<OboFileEventListener> listeners;

		protected String line;
		protected int linenum = 0;
		protected int totalSize = 0;
		protected int bytesRead = 0;
		protected StringBuffer tempBuffer = new StringBuffer();
		protected SimpleDateFormat dateFormat = new SimpleDateFormat("dd:MM:yyyy HH:mm");


		protected static final Map<Character, Character> escapeChars =
			new HashMap<Character, Character>();

		protected static final Map<Character, Character> unescapeChars = 
			new HashMap<Character, Character>();

		static {
			escapeChars.put(new Character('n'), new Character('\n'));
			escapeChars.put(new Character('W'), new Character(' '));
			escapeChars.put(new Character('t'), new Character('\t'));
			escapeChars.put(new Character(':'), new Character(':'));			
			escapeChars.put(new Character(','), new Character(','));
			escapeChars.put(new Character('"'), new Character('"'));
			escapeChars.put(new Character('\''), new Character('\''));		
			escapeChars.put(new Character('\\'), new Character('\\'));
			escapeChars.put(new Character('{'), new Character('{'));
			escapeChars.put(new Character('}'), new Character('}'));
			escapeChars.put(new Character('('), new Character('('));
			escapeChars.put(new Character(')'), new Character(')'));
			escapeChars.put(new Character('['), new Character('['));
			escapeChars.put(new Character(']'), new Character(']'));
			escapeChars.put(new Character('!'), new Character('!'));
			Iterator <Character> it = escapeChars.keySet().iterator();
			while (it.hasNext()) {
				Character key = it.next();
				Character value = escapeChars.get(key);
				unescapeChars.put(value, key);
			}
		}

		public MyOBOParser(){
			listeners = new ArrayList<OboFileEventListener>();
		}



		public void addOboFileEventListener(OboFileEventListener listener){
			listeners.add(listener);
		}

		public List<OboFileEventListener> getOboFileEventListener(){
			return listeners;
		}

		/** parse an ontology file
		 * 
		 * @param oboFile
		 * @throws IOException
		 * @throws ParseException 
		 */
		public void parseOBO(BufferedReader oboFile) throws IOException,ParseException{

			String line;
			String currentStanza;

			while ((line = oboFile.readLine()) != null) {
				if (line.length() == 0)
					continue;

				if ( line.charAt(0) == '[') {
					if (line.charAt(line.length() - 1) != ']')
						throw new ParseException("Unclosed stanza: \"" + line + "\"" );
					String stanzaname = line.substring(1, line.length() - 1);
					if (stanzaname.length() < 1)
						throw new ParseException("Empty stanza: \"" +line+"\"");
					currentStanza = stanzaname;				

					//System.out.println("stanza: " + currentStanza);
					triggerNewStanza(currentStanza);

				} else {
					// a content line
					SOPair pair;

					pair = unescape(line, ':', 0, true);

					//sSystem.out.println(pair);
					String name = pair.str;
					int lineEnd = findUnescaped(line, '!', 0, line.length(), true);
					if (lineEnd == -1)
						lineEnd = line.length();

					// find nested values
					NestedValue nv = null;

					int trailingStartIndex = -1;
					int trailingEndIndex = -1;
					for (int i = lineEnd - 1; i >= 0; i--) {
						if (Character.isWhitespace(line.charAt(i))) {
							// keep going until we see non-whitespace
						} else if (line.charAt(i) == '}') {
							// if the first thing we see is a closing brace,
							// we have a trailing modifier
							if (i >= 1 && line.charAt(i - 1) == '\\')
								continue;
							trailingEndIndex = i;
							break;
						} else
							break;
					}

					if (trailingEndIndex != -1) {
						for (int i = trailingEndIndex - 1; i >= 0; i--) {
							if (line.charAt(i) == '{') {
								if (i >= 1 && line.charAt(i - 1) == '\\')
									continue;
								trailingStartIndex = i + 1;
							}
						}
					}

					int valueStopIndex;
					if (trailingStartIndex == -1 && trailingEndIndex != -1)
						throw new ParseException("Unterminated trailing modifier. " + line);
					else if (trailingStartIndex != -1) {
						valueStopIndex = trailingStartIndex - 1;
						String trailing = line.substring(trailingStartIndex,
								trailingEndIndex).trim();
						nv = new NestedValue();
						getNestedValue(nv, trailing, 0);
					} else
						valueStopIndex = lineEnd;

					String value = line.substring(pair.index + 1, valueStopIndex).trim();
					/*
					 * if (nv != null) System.err.println("nv = "+nv+", value =
					 * |"+value+"|");
					 */
					if (value.length() == 0)
						throw new ParseException("Tag found with no value "+ line);

					if ( isSynonym(name)){
						Synonym synonym = parseSynonym(name,value);
						triggerNewSynonym(synonym);
					} else {
						//System.out.println("new key:" + name + " " + value);
						triggerNewKey(name,value);
					}
					//System.out.println("parsed key: " + name +" value: " + value + " nv: " + nv);



				}
			}
		}

		private boolean isSynonym(String key){
			if ( key.equals(OboFileHandler.SYNONYM) || key.equals(OboFileHandler.EXACT_SYNONYM))
				return true;
			return false;
		}

		/** parse the Synonym String from the Term.
		 * value can be: 
		 * <pre>"ca_bind" RELATED [uniprot:curation]</pre>
		 * @param value
		 * @return the synonym text
		 */
		@SuppressWarnings("unused")
		private Synonym parseSynonym(String key, String value) throws ParseException{
			//System.err.println("PARSE SYNONYM " + key +  " " + value);
			int startIndex = findUnescaped(value, '"', 0, value.length());
			if (startIndex == -1)
				throw new ParseException("Expected \"" +  line + " " + linenum);
			SOPair p = unescape(value, '"', startIndex + 1, value.length(),
					true);
			int defIndex = findUnescaped(value, '[', p.index, value.length());
			boolean has_xrefs = true;
			if (defIndex == -1) {
				// OBO 1.2 has xref list being optional: http://www.geneontology.org/GO.format.obo-1_2.shtml
				defIndex = value.length();
				has_xrefs = false;
			}
			String leftovers = value.substring(p.index + 1, defIndex).trim();
			StringTokenizer tokenizer = new StringTokenizer(leftovers, " \t");
			int scope = Synonym.RELATED_SYNONYM;
			
			if ( key.equals(OboFileHandler.EXACT_SYNONYM))
				scope = Synonym.EXACT_SYNONYM;
			else if ( key.equals(OboFileHandler.BROAD_SYNONYM))
				scope = Synonym.BROAD_SYNONYM;
			else if ( key.equals(OboFileHandler.NARROW_SYNONYM))			
				scope = Synonym.NARROW_SYNONYM;
			
			
			String catID = null;
			for (int i = 0; tokenizer.hasMoreTokens(); i++) {
				String token = tokenizer.nextToken();
				//System.out.println("TOKEN:" +token);
				if (i == 0) {
					// QuickGO appears to ignore case... but I cant find case-sensitivity in the specification...
					if (token.equalsIgnoreCase("RELATED"))
						scope = Synonym.RELATED_SYNONYM;
					else if (token.equalsIgnoreCase("UNSPECIFIED"))
						scope = Synonym.RELATED_SYNONYM;
					else if (token.equalsIgnoreCase("EXACT"))
						scope = Synonym.EXACT_SYNONYM;
					else if (token.equalsIgnoreCase("BROAD"))
						scope = Synonym.BROAD_SYNONYM;
					else if (token.equalsIgnoreCase("NARROW"))
						scope = Synonym.NARROW_SYNONYM;
					else
						throw new ParseException("Found unexpected scope "
								+ "identifier " + token + line);
				} else if (i == 1) {
					catID = token;
				} else
					throw new ParseException("Expected dbxref list,"
							+ " instead found " + token + 	line );
			}

			Synonym synonym = new Synonym();
			synonym.setScope(scope);
			synonym.setCategory(catID);
			synonym.setName(p.str);
			//System.out.println("SYNONYM: " + p.str +" " + synonym.getCategory() + " " + synonym.getScope());

			if (has_xrefs) {
				Map<String,Object>[] refs = getDbxrefList(value,defIndex + 1, value.length());
				
				// set the refs in the synonym
				for (Map<String, Object> ref : refs){
					String xref = (String) ref.get("xref");
					String desc = (String) ref.get("desc");
					//System.out.println(xref + " " + desc);
					NestedValue nv = (NestedValue) ref.get("nv");
					//TODO: add implementation for this...
				}
			}

			return synonym;
		}

		protected Map<String,Object>[] getDbxrefList(String line, int startoffset, int endoffset) throws ParseException {
			Vector<Map<String,Object>> temp = new Vector<Map<String,Object>>();
			boolean stop = false;
			while (!stop) {
				int braceIndex = findUnescaped(line, '{', startoffset, endoffset);
				int endIndex = findUnescaped(line, ',', startoffset, endoffset,
						true);
				boolean trailing = false;
				if (endIndex == -1) {
					endIndex = findUnescaped(line, ']', startoffset, endoffset,
							true);
					if (endIndex == -1) {
						throw new ParseException("Unterminated xref list " + line);
					}
					stop = true;
				}
				if (braceIndex != -1 && braceIndex < endIndex) {
					endIndex = braceIndex;
					trailing = true;
				}

				Map<String, Object> pair = parseXref(line, 
						startoffset,
						endIndex);
				if (pair == null) {
					startoffset++;
					continue;
				}
				NestedValue nv = null;
				if (trailing) {
					nv = new NestedValue();
					endIndex = getNestedValue(nv, line, endIndex + 1);
					if (endIndex == -1) {
						throw new ParseException("Badly formatted "
								+ "trailing properties " + line);
					}
					pair.put("nv",nv);
				}

				temp.add(pair);
				startoffset = endIndex + 1;
			}
			@SuppressWarnings("unchecked")
			Map<String,Object>[] out = new HashMap[temp.size()];
			for (int i = 0; i < temp.size(); i++) {
				Map<String, Object> pair =  temp.get(i);
				out[i] = pair;
			}
			return out;
		}

		protected Map<String,Object> parseXref(String line, 
				int startoffset, int endoffset) throws ParseException {
			String xref_str = null;
			String desc_str = null;

			SOPair xref = unescape(line, '"', startoffset, endoffset, false);
			xref_str = xref.str.trim();
			if (xref_str.length() == 0)
				return null;

			if (xref.index != -1) {
				SOPair desc = unescape(line, '"', xref.index + 1, endoffset, true);
				desc_str = desc.str.trim();
			}


			Map<String, Object> m = new HashMap<String, Object>();
			m.put("xref",xref_str);
			m.put("desc",desc_str);
			return m;
		}



		private void triggerNewStanza(String stanza){
			Iterator<OboFileEventListener> iter = listeners.iterator();
			while (iter.hasNext()){
				OboFileEventListener li = iter.next();
				li.newStanza(stanza);
			}		
		}

		private void triggerNewKey(String key, String value){
			Iterator<OboFileEventListener> iter = listeners.iterator();
			while (iter.hasNext()){
				OboFileEventListener li = iter.next();
				li.newKey(key, value);
			}
		}

		private void triggerNewSynonym(Synonym synonym){
			Iterator<OboFileEventListener> iter = listeners.iterator();
			while (iter.hasNext()){
				OboFileEventListener li = iter.next();
				li.newSynonym(synonym);
			}
		}

		public static String escape(String str, boolean escapespaces) {
			StringBuffer out = new StringBuffer();
			for (int i = 0; i < str.length(); i++) {
				char c = str.charAt(i);
				Object o = unescapeChars.get(new Character(c));
				if (o == null)
					out.append(c);
				else {
					if (escapespaces || (!escapespaces && c != ' ' && c != '\t')) {
						out.append("\\" + o);
					} else
						out.append(c);
				}
			}
			return out.toString();
		}

		public String unescape(String str) throws ParseException {
			return unescape(str, '\0', 0, str.length(), false).str;
		}

		

		protected int getNestedValue(NestedValue nv, String str, int startIndex)
		throws ParseException {
			while (startIndex < str.length()) {
				int equalsIndex = findUnescaped(str, '=', startIndex, str.length());
				if (equalsIndex == -1)
					throw new ParseException("Expected = in trailing modifier " +line);
				String name = str.substring(startIndex, equalsIndex).trim();
				SOPair value = readQuotedString(str, equalsIndex + 1, str.length(),
						',', false, true);

				Properties pv = new Properties();
				pv.setProperty(unescape(name),value.str);


				nv.addPropertyValue(pv);
				startIndex = value.endIndex + 1;
				for (; startIndex < str.length(); startIndex++) {
					if (Character.isWhitespace(str.charAt(startIndex)))
						continue;
					else if (str.charAt(startIndex) == ',') {
						startIndex++;
						break;
					} else {
						System.err.println("found character |"
								+ str.charAt(startIndex) + "|");
						throw new ParseException("Expected comma in trailing modifier. " + 
								line + " linenr: " + linenum);
					}
				}
			}
			return str.length();
		}

	}

	class NestedValue {

		/**
		 * not persisted, just to keep code warning free
		 */
		@SuppressWarnings("unused")
		private static final long serialVersionUID = -7529450225162773796L;
		protected Properties propertyValues = new Properties();
		protected String name;
		protected String suggestedComment;

		public NestedValue() {
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public String toString(){
			String txt = "NestedValue: " ;
			Set keys = propertyValues.keySet();
			Iterator<String> iter = keys.iterator();
			while (iter.hasNext()){
				String key = iter.next();
				String value = (String) propertyValues.get(key);
				txt += " [" + key + ":" + value + "]";
			}


			return txt;
		}

		public String getName() {
			return name;
		}

		public Properties getPropertyValues() {
			return propertyValues;
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		public void addPropertyValue(Properties pv) {
			Set keys = pv.keySet();
			Iterator<String> iter = keys.iterator();
			while (iter.hasNext()){
				String key = iter.next();
				String value = (String) pv.get(key);
				propertyValues.setProperty(key, value); 	
			}

		}

		@Override
		public Object clone() {
			try {
				return super.clone();
			} catch (CloneNotSupportedException ex) {
				// this will never happen
				return null;
			}
		}

		public String getSuggestedComment() {
			return suggestedComment;
		}

		public void setSuggestedComment(String suggestedComment) {
			this.suggestedComment = suggestedComment;
		}
	

}
