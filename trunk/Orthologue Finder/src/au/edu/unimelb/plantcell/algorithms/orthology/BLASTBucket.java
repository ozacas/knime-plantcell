package au.edu.unimelb.plantcell.algorithms.orthology;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstracts the details of the hits associated with exactly one sequence. Everything kept private
 * from the model.
 * 
 * @author andrew.cassin
 *
 */
public class BLASTBucket implements Iterable<BLASTRecord> {
	public static final BigDecimal DEFAULT_EPSILON = new BigDecimal("1e-80");
	
	private final ArrayList<BLASTRecord> m_l = new ArrayList<BLASTRecord>();
	private boolean m_sorted = false;
	private BigDecimal m_epsilon = DEFAULT_EPSILON; // only hits with e-values between [best_eval, best_eval+epsilon] in the bucket will be considered a suitable hit
	private String m_key;
	
	public BLASTBucket() {
	}
	
	public BLASTBucket(BigDecimal epsilon) {
		this();
		m_epsilon = epsilon;
	}
	
	public BLASTBucket(BLASTRecord first_hit) {
		this();
		add(first_hit);
		setMyKey(first_hit.getQueryAccsn());
	}
	
	public BLASTBucket(BigDecimal epsilon, BLASTRecord first_hit) {
		this(epsilon);
		add(first_hit);
		setMyKey(first_hit.getQueryAccsn());
	}
	
	public void add(BLASTRecord r) {
		if (r != null) {
			m_sorted = false;
			m_l.add(r);
		}
	}
	
	protected void setMyKey(String k) {
		assert(k != null && k.length() > 0);
		m_key = k;
	}
	
	public String getMyKey() {
		return m_key;
	}
	
	/** 
	 * Returns true if the specified record should be considered for orthology, false otherwise.
	 * 
	 * @param r
	 * @return
	 */
	protected boolean accept(BLASTRecord r) {
		// wierd: should be something in the bucket... throw?
		if (m_l.size() == 0)
			return false;
		return accept_pair(r, m_l.get(0));
	}
	
	/**
	 * Accepts <code>r</code> for further processing if it has the same e-value as <code>r2</code> in which
	 * case it returns true, false otherwise.
	 * @param r
	 * @param r2
	 * @return true if <code>r</code> is "good enough" to be considered a top hit, false otherwise
	 */
	protected boolean accept_pair(BLASTRecord r, BLASTRecord r2) {
		assert(r != null && r2 != null);
		BigDecimal diff = r.getEvalue().subtract(r2.getEvalue()).abs();
		if (diff.compareTo(m_epsilon) < 0)  {
			return true;
		}
		return false;
	}
	
	/**
	 * Returns true if the specified node is considered "first" ie. equal-top bitscore
	 * using the method as described in the bioinformatics paper:
	 * "Choosing BLAST options for better detection of orthologs as reciprocal best hits" by Gabriel Moreno-Hagelsieb and Kristen Latimer
	 * 
	 * @param br
	 * @return
	 */
	public boolean isFirst(BLASTRecord br) {
		BLASTRecord first = m_l.get(0);
		int top_bitscore = first.getBitScore();
	
		for (int i=0; i<m_l.size() && m_l.get(i).getBitScore() == top_bitscore; i++) {
			BLASTRecord tmp = m_l.get(i);
			if (accept_pair(br, tmp))
				return true;
		}
		return false;
	}
	
	
	protected BLASTBucket getSuitableHits() {
		BLASTBucket bb = new BLASTBucket();
		
		if (!m_sorted) {
			Collections.sort(m_l);
			m_sorted = true;
		}
		assert(m_sorted);
		
		if (m_l.size() > 0) {
			for (BLASTRecord r : m_l) {
				if (accept(r))  {	
					bb.add(r);
				}
			}
		}
		return bb;
	}
	
	public List<BLASTBucket> getBest(Map<String,BLASTBucket> hits) {
		ArrayList<BLASTBucket> l = new ArrayList<BLASTBucket>();
		BLASTBucket recs = getSuitableHits();
		
		for (BLASTRecord br : recs) {
			String accsn = br.getSubjectAccsn();
			BLASTBucket relative = hits.get(accsn);
			if (relative != null)
				l.add(relative);
		}
		
		return l;
	}

	public PutativeOrthologue[] getOrthologues(OrthologueFilterInterface ofi, BLASTBucket b2, Map<String, BLASTBucket> hits) throws Exception {
		HashSet<PutativeOrthologue> ret = new HashSet<PutativeOrthologue>();
		
		// if the buckets are the same then no speciation event can have occurred...
		if (b2 != this) {
			BLASTBucket r1 = this.getSuitableHits();
			BLASTBucket r2 = b2.getSuitableHits();
			
			for (BLASTRecord br1 : r1) {
				for (BLASTRecord br2 : r2) {
					if (br1.equals(br2)) {		// a->b and b->a?
						if (ofi.accept_as_ortholog(r1, r2, br1, br2)) {	// top hit?
							PutativeOrthologue o = new PutativeOrthologue(br1, br2);
							if (!ret.contains(o)) {
								ret.add(o);
							}
						}
					}
				}
			}
		}
	
		return ret.toArray(new PutativeOrthologue[0]);
	}

	protected boolean isOrtholog(BLASTBucket r1, BLASTBucket r2,
			BLASTRecord br1, BLASTRecord br2) {
		if (r1.isFirst(br1) && r2.isFirst(br2)) {
			return true;
		}
		return false;
	}

	@Override
	public Iterator<BLASTRecord> iterator() {
		return m_l.iterator();
	}
}
