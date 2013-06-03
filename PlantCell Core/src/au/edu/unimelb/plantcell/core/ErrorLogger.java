package au.edu.unimelb.plantcell.core;

import org.apache.commons.exec.LogOutputStream;
import org.knime.core.node.NodeLogger;

/**
 * Logs an errors from BLAST to the KNIME console at ERROR level, using the apache commons exec API. Often used by other
 * external tool handling nodes, so it appears in the core package for now.
 * 
 * @author andrew.cassin
 *
 */
public class ErrorLogger extends LogOutputStream {
	private NodeLogger m_l;
	private boolean m_keywords;
	
	/**
	 * Default constructor. All messages logged at an error level
	 * @param l
	 */
	public ErrorLogger(NodeLogger l) {
		super();
		m_l = l;
		m_keywords = false;
	}
	
	/**
	 * 
	 * @param l
	 * @param require_keywords iff true only those log messages with error|problem|correct|fix|... will be logged at an error level, all else warning level
	 */
	public ErrorLogger(NodeLogger l, boolean require_keywords) {
		m_keywords = require_keywords;
		m_l = l;
	}
	
	@Override
	protected void processLine(String arg0, int arg1) {
		if (!m_keywords) {
			m_l.error(arg0);
		} else {
			String str = arg0.toLowerCase();
			if (str.indexOf("error") >= 0 || str.indexOf("problem") >= 0 || 
					str.indexOf("wrong") >= 0|| str.indexOf("problem") >= 0 || str.indexOf("fix") >= 0) {
				m_l.error(arg0);
			} else {
				m_l.warn(arg0);
			}
		}
	}

}
