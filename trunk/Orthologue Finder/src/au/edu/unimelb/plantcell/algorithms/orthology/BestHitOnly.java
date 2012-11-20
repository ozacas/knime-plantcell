package au.edu.unimelb.plantcell.algorithms.orthology;

public class BestHitOnly implements OrthologueFilterInterface {
	
	@Override
	public boolean accept_as_best(BLASTRecord br) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean accept_as_ortholog(BLASTBucket b1, BLASTBucket b2,
			BLASTRecord r1, BLASTRecord r2) {
		// TODO Auto-generated method stub
		return false;
	}

}
