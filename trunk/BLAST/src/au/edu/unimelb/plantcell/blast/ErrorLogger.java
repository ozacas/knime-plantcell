package au.edu.unimelb.plantcell.blast;

import org.apache.commons.exec.LogOutputStream;
import org.knime.core.node.NodeLogger;

/**
 * Logs an errors from BLAST to the KNIME console at ERROR level
 * @author andrew.cassin
 *
 */
public class ErrorLogger extends LogOutputStream {
	private NodeLogger m_l;
	
	public ErrorLogger(NodeLogger l) {
		super();
		m_l = l;
	}
	
	@Override
	protected void processLine(String arg0, int arg1) {
		m_l.error(arg0);
	}

}
