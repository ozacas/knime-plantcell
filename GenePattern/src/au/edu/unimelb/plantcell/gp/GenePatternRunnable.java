package au.edu.unimelb.plantcell.gp;

import org.apache.log4j.Logger;
import org.genepattern.client.GPClient;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;

public class GenePatternRunnable implements Runnable {
	private GPClient m_gp;
	private String m_lsid;
	private Parameter[] m_params;
	private GenePatternResultHandler m_hndlr;
	private boolean m_last_run_ok;
	
	public GenePatternRunnable(GPClient gpc, GenePatternResultHandler hndlr, String lsid, Parameter[] params) {
		assert(gpc != null && hndlr != null && lsid != null && params != null);
		m_gp = gpc;
		m_lsid = lsid;
		m_params = params;
		m_hndlr = hndlr;
		m_last_run_ok = false;
	}
	
	@Override
	public void run() {
		try {
			m_last_run_ok = false;
			JobResult res = m_gp.runAnalysis(m_lsid, m_params);
			if (res.getJobNumber() >= 0) {
				m_last_run_ok = true;
				m_hndlr.processResult(res);
			} else {
				m_last_run_ok = false;
				Logger.getLogger(AbstractGPNodeModel.class).warn("Job failed!");
			}
		} catch (Exception e) {
			m_last_run_ok = false;
			Logger.getLogger(AbstractGPNodeModel.class).error(e);
			e.printStackTrace();
		}
	}

	public synchronized boolean lastRunOk() {
		return m_last_run_ok;
	}
}
