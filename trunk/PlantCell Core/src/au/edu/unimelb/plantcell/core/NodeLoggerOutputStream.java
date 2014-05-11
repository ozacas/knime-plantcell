package au.edu.unimelb.plantcell.core;

import org.apache.commons.exec.LogOutputStream;
import org.knime.core.node.NodeLogger;

/**
 * Convenience wrapper around command execution which sends logging data to the specified KNIME logger.
 * 
 * @author acassin
 *
 */
public class NodeLoggerOutputStream extends LogOutputStream {
	private NodeLogger logger;
	
	public NodeLoggerOutputStream(NodeLogger nl) {
		assert(nl != null);
		logger = nl;
	}
	
	@Override
	protected void processLine(String arg0, int arg1) {
		logger.info(arg0);
	}
	
}
