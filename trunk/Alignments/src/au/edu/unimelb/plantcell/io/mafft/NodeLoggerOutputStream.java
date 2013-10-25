package au.edu.unimelb.plantcell.io.mafft;

import org.apache.commons.exec.LogOutputStream;
import org.knime.core.node.NodeLogger;

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
