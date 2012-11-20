 package au.edu.unimelb.plantcell.io.ws.multialign;

import java.util.Properties;

import org.knime.core.data.DataCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 * Base-class for proxy stubs to interface to multiple alignment algorithms.
 * Each subclass is responsible for ensuring the right data is sent to the server
 * and for processing results.
 * 
 * @author andrew.cassin
 *
 */
public abstract class AbstractProxy {
	private static NodeLogger m_logger;
	
	public AbstractProxy(NodeLogger l) {
		assert(l != null);
		m_logger = l;
	}
	
	protected NodeLogger getLogger() {
		return m_logger;
	}
	
	/**
	 * Returns a concrete subclass appropriate for the specified algorithm
	 * @param algorithm
	 * @return
	 * @throws InvalidSettingsException if the alignment algorithm is unknown
	 */
	public static AbstractProxy makeProxy(NodeLogger logger, String algorithm) throws InvalidSettingsException {
		m_logger = logger;
		if (algorithm.toLowerCase().indexOf("muscle") >= 0) {
			return new MUSCLEProxy(m_logger);
		} else if (algorithm.toLowerCase().indexOf("omega") >= 0) {
			return new ClustalOProxy(m_logger);
		} else if (algorithm.toLowerCase().indexOf("coffee") >= 0) {
			return new TCoffeeProxy(m_logger);
		}
		throw new InvalidSettingsException("unknown algorithm: "+algorithm);
	}
	
	public abstract void prepare(String fasta);
	
	/**
	 * Unique jobID is returned
	 * 
	 * @return
	 */
	public abstract String run(final Properties props) throws Exception;
	
	public abstract boolean wait_for_completion(ExecutionContext exec, String jobid) throws Exception;
	
	protected boolean wait_for_completion(ExecutionContext exec, final StatusGetter proxy, String jobid) throws Exception {
    	String status = "PENDING";
    	int delay = 30;
    	getLogger().info("Waiting for job to complete... "+jobid);
    	while (status.startsWith("PEND") || status.startsWith("WAIT") ||
    			status.startsWith("Q") || status.startsWith("INCOMPLET") ||
    			status.startsWith("RUN")) {
    			exec.checkCanceled();
    			if (delay > 30) {
    				getLogger().info("Waiting "+delay+" seconds for EBI job to complete: "+jobid);
	    			for (int i=0; i<10; i++) {
	    				Thread.sleep((delay/10) * 1000);
	    				exec.checkCanceled();
	    			}
    			}
    			status = proxy.getStatus(jobid);
    			if (status == null) {
    				throw new Exception("Unable to get job status for job from EBI: "+jobid);
    			}
    			status = status.toUpperCase();
    			getLogger().info("While waiting we got status: "+status+" for job "+jobid);
    			delay += 30;
    	}
    	return (status.startsWith("COMPLETE") || status.startsWith("DONE") ||
    			status.startsWith("FINISH"));
	}

	public abstract DataCell[] get_results(ExecutionContext exec, String jobid) throws Exception;
}
