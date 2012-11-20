package au.edu.unimelb.plantcell.io.ws.golgip;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
 <form name="form1" method="post" action="GolgiP_run.php" enctype="multipart/form-data"> 
  <table width="66%" border="0" align="center">
     <tr>
       <td> <p align="left"><strong>Copy &amp; paste your sequences here (in FASTA
          format):</strong> <a href="example.php">[Example sequences]</a><br>
          <textarea name="seq" cols="100" rows="12" id="seq">
</textarea>
<br>
          <strong>Or upload your sequences from text file (in FASTA format):</strong> <br>
          <input name="fastafile" type="file" id="fastafile" size="130">
	</p></td></tr>
    <tr>
      <td> <p align="left"><strong>Choose an organism type:</strong><br>
		<input type="radio" name=GolgiPOrganism value="GolgiPPlant" checked>Plant<br>
           </p></td></tr><br>
    <tr>
      <td> <div align="left"><strong>Check the model that you want to perform the prediction:<br>
	 (some models may take longer time to get the results)</strong></div>
        <blockquote>
          <div align="left">
<!--                <form name=form2>  -->
                <input type="radio" name=GolgiP value="GolgiPAll" checked>GolgiP Comprehensive model<br> 
                <input type="radio" name=GolgiP value="GolgiPFunc">GolgiP Functional domain model<br>
                <input type="radio" name=GolgiP value="GolgiPDiAA">GolgiP Dipeptide composition model<br>
                <input type="radio" name=GolgiP value="GolgiPTransMem">GolgiP Transmembrane domain model<br>
<!--                </form>  -->
        </p></div>
        </blockquote></td>
    </tr>

     <tr>
      <td> <div align="center">
          <input type="submit" name="Submit" value="Submit">
          <input type="reset" name="Submit2" value="Reset">
        </div></td></tr>

  </table>
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

	public final int MDL_COMPREHENSIVE        =1;
	public final int MDL_FUNCTIONAL           =2;
	public final int MDL_TRANSMEMBRANE        =3;
	public final int MDL_DIPEPTIDE_COMPOSITION=4;

	/*
	 * internal state passed to the CGI server
	 */
	private int          m_mdl;

	public Form() {
		m_mdl = MDL_COMPREHENSIVE;
	}
	
	public void setModel(int model) {
		assert(model >= MDL_COMPREHENSIVE && model <= MDL_DIPEPTIDE_COMPOSITION);
		m_mdl = model;
	}
	
	public void setModel(String model) {
		if (model.toLowerCase().startsWith("function")) {
			setModel(MDL_FUNCTIONAL);
		} else if (model.toLowerCase().startsWith("Transmembrane")) {
			setModel(MDL_TRANSMEMBRANE);
		} else if (model.toLowerCase().startsWith("Dipeptide")) {
			setModel(MDL_DIPEPTIDE_COMPOSITION);
		} else {
			setModel(MDL_COMPREHENSIVE);
		}
	}
	
	public void process(List<SequenceValue> sequences, URL server, Callback cb) throws Exception {
		assert(server != null && sequences != null && cb != null);
		
		HttpClient client = new HttpClient();
		PostMethod   http_post = new PostMethod(server.toString());
		int id = 1;
	    StringBuffer sb2 = new StringBuffer();
	    for (SequenceValue sv : sequences) {
	    	sb2.append(">S"+id++);
	    	sb2.append(CRLF);
	    	sb2.append(new WrappedSequence(sv.getStringValue()).toString());		// toString() guarantees to always end with CRLF
	    }
	    String mdl = "GolgiPAll";
	    switch (m_mdl) {
	    case MDL_FUNCTIONAL:
	    		mdl = "GolgiPFunc"; break;
	    case MDL_DIPEPTIDE_COMPOSITION:
	    		mdl = "GolgiPDiAll"; break;
	    case MDL_TRANSMEMBRANE:
	    		mdl = "GolgiPTransMem"; break;
	    }
		Part[] parts = {
				new StringPart("seq", sb2.toString()),
				new StringPart("GolgiPOrganism", "GolgiPPlant"),
				new StringPart("GolgiP", mdl),
				new StringPart("Submit", "Submit")
		};
		
		http_post.setRequestEntity(
			      new MultipartRequestEntity(parts, http_post.getParams())
			      );
		int status = client.executeMethod(http_post);
	    if (status >= 200 && status < 300) {
	    	process_predictions(http_post.getResponseBodyAsString(), sequences, cb);
	    } else {
	    	throw new IOException("GolgiP server down? HTTP status "+status);
	    }
	}
	
	protected void process_predictions(String response, List<SequenceValue> sequences, Callback cb) throws Exception {
		response = response.replaceAll("[\\r\\n]+", " ");
		String[]       divs = response.split("<div ");
		// the last two divisions contain the predictions
		Pattern row_pattern = Pattern.compile("<tr[^>]*?>(.*?)</tr>");
		
		// 1. go thru the prediction table
		Matcher m = row_pattern.matcher(divs[divs.length-2]);
		int found = 0;		// GolgiP should offer predictions for every sequence given
		SequenceValue sv = null;
		while (m.find()) {
			String table_row = m.group(1);
			String[]    tmp  = table_row.split("</t[hd]>");
			ArrayList<String> fields = new ArrayList<String>();
			for (String s : tmp) {
				s = s.replaceAll("<[a-zA-Z]+[^>]*?>", "").trim();
				s = s.replaceAll("</\\w+>", "");
				fields.add(s);
			}
			if (fields.get(0).startsWith("Query")) // skip over header line
				continue;
			
			// replace eg. 's1' by the first sequence for the output table...
			if (fields.get(0).startsWith("S")) {
				Integer idx = new Integer(fields.get(0).substring(1));
				sv = sequences.get(idx.intValue()-1);
				found++;
				fields.remove(0);
			} else {
				continue;
			}
			if (sv != null) 
				cb.process_predictions(sv, fields.toArray(new String[0]));
		}
		if (found != sequences.size()) {
			cb.warn("GolgiP did not predict all sequences in batch - something wrong?");
		}
		
		// 2. go thru the conserved domains table
		m = row_pattern.matcher(divs[divs.length-1]);
		while (m.find()) {
			String table_row = m.group(1);
			String[]    tmp  = table_row.split("</t[hd]>");
			ArrayList<String> fields = new ArrayList<String>();
			for (String s : tmp) {
				s = s.replaceAll("<[a-zA-Z]+[^>]*?>", "").trim();
				s = s.replaceAll("</\\w+>", "");
				fields.add(s);
			}
			if (fields.get(0).startsWith("Query")) // skip over header line
				continue;
			
			// replace eg. 's1' by the first sequence for the output table...
			if (fields.get(0).startsWith("S")) {
				Integer idx = new Integer(fields.get(0).substring(1));
				fields.set(0, sequences.get(idx.intValue()-1).getID());
			} else {
				continue;
			}
			cb.process_conserved_domains(fields.toArray(new String[0]));
		}
	}
}
