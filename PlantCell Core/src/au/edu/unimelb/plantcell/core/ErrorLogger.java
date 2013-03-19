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
	
	public ErrorLogger(NodeLogger l) {
		super();
		m_l = l;
	}
	
	@Override
	protected void processLine(String arg0, int arg1) {
		m_l.error(arg0);
	}

}
