package au.edu.unimelb.plantcell.algorithms.orthology;

/**
 * Classes must implement this interface to have the necessary methods
 * to be called when filtering of blast records and selection of putative orthologs
 * during the node execution. 
 * 
 * @author andrew.cassin
 *
 */
public interface OrthologueFilterInterface {
	public boolean accept_as_best(BLASTRecord br);
	
	public boolean accept_as_ortholog(BLASTBucket b1, BLASTBucket b2, 
										BLASTRecord r1, BLASTRecord r2);
}
