package au.edu.unimelb.plantcell.io.ws.mascot.msms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.soap.SOAPException;
import javax.xml.ws.soap.SOAPFaultException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.servers.mascotee.endpoints.SearchService;

/**
 * Responsible for waiting until the specified Mascot searches are complete. May take considerable time
 * and must be carefully written to handle temporary network failures.
 * 
 * @author acassin
 *
 */
public class JobCompletionManager {
	private NodeLogger logger;
	private final ExecutionContext exec;
	
	
	public JobCompletionManager(final NodeLogger logger, final ExecutionContext exec) {
		assert(logger != null);
		this.logger = logger;
		this.exec = exec;
	}

	/**
     * Returns the list of mascot dat files comprising the results for each of the specified jobs to the specified MascotEE server
     * 
     * @param ss MascotEE service instance to use
     * @param job_ids jobs queued which we are to wait for completion
     * @return only successfully completed jobs are returned
	 * @throws Exception 
     * @throws SOAPException 
     */
    public List<String> waitForAllJobsCompleted(final SearchService ss, final List<String> job_ids) throws Exception {
    	assert(job_ids != null && job_ids.size() > 0 && ss != null);
    	
		int retries = 0;
		int max_retries = 5;
		HashSet<String> dat_file_numbers = new HashSet<String>();
		logger.info("Waiting for jobs to complete: "+job_ids.size()+" mascot jobs.");
		for (String job_id : job_ids) {
			logger.info("Getting job status for "+job_id);
			while (true) {
				try {
					exec.checkCanceled();
					String status = ss.getStatus(job_id);
					logger.info("Got status "+status+" for job "+job_id);
					if (status.startsWith("PENDING") || status.startsWith("QUEUED") || status.startsWith("WAITING") || status.startsWith("RUNNING")) {
						waitFor(60);
						continue;
					}
					if (status.equals("UNKNOWN")) {
						throw new InvalidSettingsException("Got unknown job status from MascotEE for job "+job_id+"... aborting!");
					}
					String dat_file = ss.getResultsDatFile(job_id);
					if (dat_file == null || !dat_file.endsWith(".dat")) {
						throw new InvalidSettingsException("bogus mascot .dat file: "+dat_file);
					}
					dat_file_numbers.add(dat_file);
					retries = 0;
					break;
				} catch (SOAPFaultException|CanceledExecutionException do_not_retry_these) {
					throw do_not_retry_these;
				} catch (InvalidSettingsException ise) {
					// and ISE here means that there were no results from mascot... this is probably bad search parameters
					// But we only slow down a little bit for this one
					logger.warn("Did not get any mascot results for "+job_id+", check your search parameters!");
					throw ise;
				} catch (Exception e) {
					e.printStackTrace();
					waitFor((retries * 100)+100);
					retries++;
					if (retries > 5) {
						break;
					}
				}
			}
			
			if (retries > max_retries) {
				break;
			}
		}
		
		ArrayList<String> ret = new ArrayList<String>();
		ret.addAll(dat_file_numbers);
		return ret;
	}

	private void waitFor(int i) {
		assert(i > 0);
		try {
			Thread.sleep(i * 1000);
		} catch (InterruptedException ie) {
			// silence
		}
	}

	public List<String> waitForJobCompleted(final SearchService ss, final String jobID) throws Exception {
		if (jobID == null) {
			throw new InvalidSettingsException("No jobID specified!");
		}
		ArrayList<String> l = new ArrayList<String>();
		l.add(jobID);
		return waitForAllJobsCompleted(ss, l);
	}
}
