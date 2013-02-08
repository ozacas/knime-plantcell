package au.edu.unimelb.plantcell.io.ws.predgpi;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/*
 * Implements CGI to the following HTML code at the http://csbl1.bmb.uga.edu/GolgiP/
 * Abstracts away the CGI from the caller.
 * 
 
<form name="form1" method="POST"  enctype=multipart/form-data  onSubmit="return OnSubmitForm();">

<h3>Choose the omega-site prediction model</h3>
<p class="contenuto">
<input type="radio" name="tipo_hmm" value="0" checked > General model (recommended) <br>

<input type="radio" name="tipo_hmm" value="1"> Conservative model
</p>

<h3>Submit sequence(s) in FASTA format:</h3>
<p class="contenuto">
Sequences must be at least 40 residues long.<br>Paste the complete aminoacidic sequence(s):</p>
<p class="contenuto">
<textarea name="SEQ" cols="60" rows="10" wrap="OFF" id="textarea"></textarea></p>
<p class="contenuto">Or upload a file: <input type="file" name="upfile">
</p>
<p class="contenuto">If you want to receive the results via e-mail, please fill in this field and click on Download:<br> <input type="text" name="email">
</p>

<p class="contenuto">Please submit a <strong> maximum of 500 sequences</strong> per time.</p>
<p class="contenuto">
 <input type="submit" name="Display" value="Display" onClick="document.pressed=this.value">
 <input type="submit" name="Download" value="Download" onClick="document.pressed=this.value">
 <input name="Reset" type="reset" id="Reset" value="Reset">
</p>
</form>
 */
public class Form {
	private final String CRLF = "\r\n"; // Line separator required by multipart/form-data.

	public class WrappedSequence {
		private final StringBuffer sb = new StringBuffer(10 * 1024);
		private final int line_length_limit = 80;
		
		public WrappedSequence(String seq) {
			for (int i=0; i<seq.length(); i+= line_length_limit) {
				int end = i+line_length_limit;
				if (end > seq.length()) {
					end = seq.length();
				}
				sb.append(seq.substring(i, end));
				sb.append(CRLF);
			}
		}
		
		@Override 
		public String toString() {
			String ret = sb.toString();
			if (!ret.endsWith(CRLF))
				return ret + CRLF;
			else
				return ret;
		}
	}


	private boolean m_use_general;	// true is recommended ie. use the general model
	
	public Form() {
		setModel(true);
	}
	
	public void setModel(boolean use_general) {
		m_use_general = use_general;
	}
	
	public void setModel(String model) {
		if (model.toLowerCase().startsWith("general")) {
			setModel(true);
		} else {
			setModel(false);
		} 
	}
	
	public void process(List<SequenceValue> sequences, URL server, Callback cb) throws Exception {
		assert(server != null && sequences != null && cb != null);
		
		HttpClient client = new HttpClient();
		PostMethod   http_post = new PostMethod(server.toString());
		
		int id = 1;
	    StringBuffer sb2 = new StringBuffer();
	    Map<String,SequenceValue> map = new HashMap<String,SequenceValue>();
	    for (SequenceValue sv : sequences) {
	    	String s_id = "S"+id++;
	    	sb2.append(">"+s_id);
	    	sb2.append(CRLF);
	    	sb2.append(new WrappedSequence(sv.getStringValue()).toString());		// toString() guarantees to always end with CRLF
	    	map.put(s_id, sv);
	    }
	    
		Part[] parts = {
				new StringPart("SEQ", sb2.toString()),
				new StringPart("tipo_hmm", m_use_general ? "0" : "1"),
				new StringPart("submit", "Display")
		};
		
		http_post.setRequestEntity(
			      new MultipartRequestEntity(parts, http_post.getParams())
			      );
		int status = client.executeMethod(http_post);
	    if (status >= 200 && status < 300) {
	    	process_predictions(http_post.getResponseBodyAsString(), map, cb);
	    } else {
	    	throw new IOException("PredGPI server down? HTTP status "+status);
	    }
	}

	private void process_predictions(String responseBodyAsString,
			Map<String,SequenceValue> map, Callback cb) {
		
		responseBodyAsString = responseBodyAsString.replaceAll("[\\r\\n]+", " ");
		Pattern p = Pattern.compile("<table>\\s*<caption>Proteins\\s*predicted\\s*</caption>(.*?)</table>");
		Matcher m = p.matcher(responseBodyAsString);
		
		// recognise what to screenscrape?
		if (m.find()) {
			String protein_table = m.group(1);
			String[] rows = protein_table.split("</tr>");
			p = Pattern.compile("<td\\s+class=\"([^\"]+?)\">([^<]+?)</td>");
			for (int i=1; i<rows.length; i++) {		// skip table header row
				String row = rows[i].trim().replaceAll("[\\r\\n]+", " ");
				Matcher m2 = p.matcher(row);
				HashMap<String,String> props = new HashMap<String,String>();
				while (m2.find()) {
					props.put(m2.group(1), m2.group(2));
				}
				
				if (props.containsKey("name") && props.containsKey("omega")) {
					String name = props.get("name").trim();
					SequenceValue sv = map.get(name);
					if (sv != null) {
						for (String key : props.keySet()) {
							if (!key.startsWith("FP_"))
								continue;
							String fp = props.get(key).trim();
							if (fp.endsWith("%")) {
								fp = fp.substring(0, fp.length()-1);
							}
							cb.addPrediction(sv, props.get("omega"), fp, key.substring("FP_".length()));
							break;
						}
					} else {
						cb.warn("No sequence with ID: "+name);
					}
					map.remove(name);
				}
			}
		}
		
		// should not happen...
		if (map.size() > 0) {
			for (SequenceValue sv : map.values()) {
				cb.warn("No prediction available for "+sv.getID());
			}
		}
		map.clear();
	}
	
	
}
