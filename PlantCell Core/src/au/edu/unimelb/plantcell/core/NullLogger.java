package au.edu.unimelb.plantcell.core;

import org.apache.commons.exec.LogOutputStream;

/**
 * Throws away all log messages
 * @author andrew.cassin
 *
 */
public class NullLogger extends LogOutputStream {

	@Override
	protected void processLine(String arg0, int arg1) {
		// NO-OP
	}

}
