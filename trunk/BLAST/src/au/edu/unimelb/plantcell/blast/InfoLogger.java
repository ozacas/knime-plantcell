package au.edu.unimelb.plantcell.blast;

import org.knime.core.node.NodeLogger;

public class InfoLogger extends ErrorLogger {
	private NodeLogger m_l;
	
	public InfoLogger(NodeLogger l) {
		super(l);
		m_l = l;
	}
	
	@Override
	protected void processLine(String arg0, int arg1) {
		m_l.info(arg0);
	}
}
