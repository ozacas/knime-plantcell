package au.edu.unimelb.plantcell.io.read.fasta;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.knime.core.node.InvalidSettingsException;

import au.edu.unimelb.plantcell.core.cells.Comment;
import au.edu.unimelb.plantcell.core.cells.CommentType;
import au.edu.unimelb.plantcell.core.cells.SequenceImpl;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;



/***
 * 
 * WARNING: buggy do not use!
 * @author andrew.cassin
 *
 */
public class FastaIterator implements Iterable<SequenceValue>, Iterator<SequenceValue> {
	private BufferedReader m_rdr;

	// state during iteration
	private SequenceType m_st;
	private String m_saved;
	private String m_id;
	private String m_descr;
	private String m_seq;
	
	private static Pattern m_re_accsn = Pattern.compile("^(\\S+)\\b");
	private static Pattern m_re_descr = Pattern.compile("^\\S+\\s*(.*)$");
	
	
	/**
	 * Recommended constructor which supports gzip compressed FASTA files (.gz extension)
	 * or uncompressed files.
	 * 
	 * @param fasta_file
	 * @param st 
	 * @throws Exception
	 */
	public FastaIterator(File fasta_file, SequenceType st) throws Exception {
		this(fasta_file.getName().toLowerCase().endsWith(".gz") ?
				new GZIPInputStream(new FileInputStream(fasta_file)) : 
				new FileInputStream(fasta_file),
				st);
		
		if (fasta_file.length() < 1) {
			if (m_rdr != null)
				m_rdr.close();
			throw new InvalidSettingsException("Cannot process empty file!");
		}
	}
	
	public FastaIterator(InputStream is, SequenceType st) throws Exception {
		/**
         * We define the mapping between FASTA files and java as 7-bit: I dont think
         * any other character encoding is used by these files - maybe UTF8???
         */
		this(new InputStreamReader(is, "US-ASCII"), st);
	}
	
	public FastaIterator(Reader rdr, SequenceType st) throws Exception {
		m_st  = st;
		m_rdr = new BufferedReader(rdr); 
	}
	
	private boolean next_seq() {
         String line;
         StringBuilder seq = new StringBuilder(10 * 1024);

         try {
		     while ((line = getLine()) != null) {
		    	 String tline = line.trim();
		    	 if (tline.startsWith(">")) {
		    		 boolean ret = true;
		    		 if (seq.length() > 0) {
		    			 ret     = grok_seq(seq, m_id);
		    			 m_saved = line;
		    		 } else {
		    			 m_id    = tline.substring(1);
		    			 m_saved = null;
		    			 continue;
		    		 }
	    			 return ret;
		    	 } else {
		    		 seq.append(tline);
		    	 }
		     }
		     
		     // handle last sequence in input
		     boolean ret = false;
		     if (seq.length() > 0) {
		    	 ret = grok_seq(seq, m_id);
		    	 // fallthru
		     }
		     
		     m_rdr.close();
		     m_rdr = null;
		     return ret;
         } catch (Exception ioe) {
        	 ioe.printStackTrace();
        	 return false;
         } 
	}
	
	private String getLine() throws IOException {
		if (m_saved != null) {
			String ret = m_saved;
			m_saved = null;
			return ret;
		}
		return m_rdr.readLine();
	}
	
	private boolean grok_seq(StringBuilder sb, String accsn_descr) throws Exception {
		m_seq            = sb.toString();
		String[] entries = accsn_descr.split("\\x01");
		String[] accsns  = parse_accession(m_re_accsn, entries);
		if (accsns == null || accsns.length < 1) {
			m_seq = null;
			return false;
		}
		
		m_id = accsns[0];		// TODO: only first accession for a given sequence for now
		String[] descr = parse_description(m_re_descr, entries);
		if (descr == null || descr.length < 1) {
			m_seq = null;
			return false;
		}
		m_descr = descr[0];
		return true;
	}
	
	protected String[] parse_accession(Pattern p, String[] entries) throws Exception {
    	int cnt = 0;
    	String[] accsns = new String[entries.length];
    	for (String entry : entries) {
    		Matcher m = p.matcher(entry);
	    	if (m.find()) {
	    		if (m.groupCount() != 1) {
	    			throw new Exception("You must use capturing parentheses () to match an accession only once!");
	    		}
	    		accsns[cnt] = m.group(1);
	    		cnt++;
	    	} 
    	}
    	if (cnt < entries.length) {
    		accsns[cnt] = null; // make sure array has null after last match
    	}
     	return (cnt > 0) ? accsns : null;
    }
    
    protected String[] parse_description(Pattern matcher, String[] entries) throws Exception {
    	int cnt = 0;
    	String[] descrs = new String[entries.length];
    	for (String entry : entries) {
    		Matcher m = matcher.matcher(entry);
    		if (m.find()) {
    			if (m.groupCount() != 1) {
        			throw new Exception("You must use capturing parentheses() to match a sequence description only once!");
        		}
    			descrs[cnt] = m.group(1);
    			cnt++;
    		}
    	}
    	if (cnt < entries.length) {
    		descrs[cnt] = null;
    	}
    	return (cnt > 0) ? descrs : null;
    }
    
	@Override
	public Iterator<SequenceValue> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		if (m_rdr == null)		// keep returning false at EOF (needed by BatchFastaIterator)
			return false;
		return next_seq();
	}

	@Override
	public SequenceValue next() {
		try {
			SequenceValue sv = new SequenceImpl(m_st, m_id, m_seq);
			if (m_descr.length() > 0) {
				sv.addComment(new Comment(CommentType.Description, m_descr));
			}
			m_id    = null;
			m_descr = null;
			m_seq   = null;
			return sv;
		} catch (InvalidSettingsException ise) {
			ise.printStackTrace();
			return null;
		}
	}

	/**
	 * Not supported
	 */
	@Override
	public void remove() {
	}

}
