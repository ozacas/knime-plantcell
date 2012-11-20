package au.edu.unimelb.plantcell.io.jemboss.simple;

import org.apache.commons.exec.LogOutputStream;
import org.knime.core.node.NodeLogger;

public class StringLogger extends LogOutputStream {
	private StringBuffer m_sb;
	private boolean m_log_as_error;
	private NodeLogger m_logger;
	
	public StringLogger() {
		m_sb     = new StringBuffer(100 * 1024);
		m_logger = null;
	}
	
	public StringLogger(NodeLogger logger, boolean log_as_error) {
		m_logger       = logger;
		m_log_as_error = log_as_error;
	}

	@Override
	protected void processLine(String arg0, int arg1) {
		if (m_sb != null)
			m_sb.append(arg0);
		else if (m_logger != null){
			if (m_log_as_error) {
				m_logger.error(arg0);
			} else {
				m_logger.warn(arg0);
			}
		}
	}

	@Override 
	public String toString() {
		return m_sb.toString();
	}
}
