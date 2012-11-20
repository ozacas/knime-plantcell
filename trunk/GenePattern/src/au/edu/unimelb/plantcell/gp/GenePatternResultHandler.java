package au.edu.unimelb.plantcell.gp;

import org.genepattern.webservice.JobResult;

public interface GenePatternResultHandler {
	public void processResult(JobResult res);
}
