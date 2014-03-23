package au.edu.unimelb.plantcell.io.read.fasta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

public class ProgressModel {
	private final URL m_u;
	private final URLConnection m_conn;
	private boolean is_likely_compressed;
	private double m_done;
	
	public ProgressModel(final URL u) throws IOException {
		assert(u != null);
		m_u = u;
		m_conn = u.openConnection();
		is_likely_compressed = checkURLforCompressedData(u);
		m_done = 0.0d;
	}
	
	protected InputStream getInputStream() throws IOException {
		InputStream is = m_conn.getInputStream();
		if (is_likely_compressed) {
			return new GZIPInputStream(is, 128*1024);
		}
		return is;
	}
	
	/**
	 * Keep an approximate progress bar (we estimate compression at 2.5x) and the call is meant to be
	 * fast or it will slow down processing of the fasta too much. Users wont mind if its only approx.
	 * @param chars
	 */
	public void done(int chars) {
		if (chars >= 0) {
			double mult = 1.0d;
			if (is_likely_compressed)
				mult = 2.5d;
			m_done += mult * chars;
		}
	}
	  
	/**
	 * Returns how much of the file has been loaded so far
	 * @return always in the range [0,1]
	 */
	public double getFractionProcessed() {
		long length = m_conn.getContentLengthLong();
		if (length <= 0)
			return 0.0d;
		double frac = (m_done / length);
		if (frac > 1.0d)
			frac = 1.0d;
		return frac;
	}
	
	@Override
	public String toString() {
		return "ProgressModel: "+m_u.toString();
	}
	
	/**
	 * Guess whether the data has been compressed before transport, based on the URL path extension. Currently
	 * only likely gzip extensions are supported.
	 * 
	 * @param u
	 * @return
	 */
    public boolean checkURLforCompressedData(final URL u) {
		String path = u.getPath();
		if (path == null || path.length() < 1)
			return false;
		return path.toLowerCase().endsWith(".gz");
	}

    /**
     * Return a suitable reader for the FASTA file type
     * @return
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
	public BufferedReader getBufferedReader() throws UnsupportedEncodingException, IOException {
	    
        /**
         * We define the mapping between FASTA files and java as 7-bit: I dont think
         * any other character encoding is used by these files - maybe UTF8???
         */
 	   return new BufferedReader(new InputStreamReader(getInputStream(), "US-ASCII"));
	}

}
