package au.edu.unimelb.plantcell.algorithms.orthology;

import java.math.BigDecimal;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

public class BLASTRecord implements Comparable<BLASTRecord> {
	private String m_query_id, m_subject_id;
	private double m_identity, m_bitscore;
	private BigDecimal m_evalue;
	private int    m_align_length, m_mismatches, m_gapopens;
	private int    m_q_start, m_q_end, m_s_start, m_s_end;
	private boolean m_is_a;
	private static int m_id = 1;

	/**
	 * Blast fields must be in -m 7 output format (tab separated CSV format)
	 * @param fields
	 * @param is_A if true, the record has come from a QUERY from A rather than a QUERY from B
	 */
	public BLASTRecord(String[] fields, boolean is_A) {
		assert(fields != null && fields.length == 12);
		// fields[0] is query accsn (string)
		// fields[1] is subject accsn (string)
		// fields[2] is %identity (double)
		// fields[3] is alignment length (int)
		// fields[4] is mismatches (int)
		// fields[5] is gap opens (int)
		// fields[6] is query start (int)
		// fields[7] is query end (int)
		// fields[8] is subject start (int)
		// fields[9] is subject end (int)
		// fields[10] is e-value (double)
		// fields[11] is bit score (double)
		m_query_id = fields[0];
		m_subject_id=fields[1];
		m_identity = new Double(fields[2]);
		m_evalue   = new BigDecimal(fields[10]);
		m_bitscore = new Double(fields[11].trim());
		m_align_length = new Integer(fields[3]);
		m_mismatches   = new Integer(fields[4]);
		m_gapopens     = new Integer(fields[5]);
		m_q_start  = new Integer(fields[6]);
		m_q_end    = new Integer(fields[7]);
		m_s_start  = new Integer(fields[8]);
		m_s_end    = new Integer(fields[9]);
		m_is_a     = is_A;
	}

	/**
	 * Return a KNIME-defined <code>DataRow</code> instance with the format as expected
	 * by <code>OrthologueFinderNodeModel.make_output_spec()</code> for the raw blast record
	 * @return row data ready for insertion into KNIME container
	 */
	public DataRow asRow() {
		DataCell[] cells = new DataCell[12];
		for (int i=0; i<cells.length; i++) {
			cells[i] = DataType.getMissingCell();
		}
		cells[0] = new StringCell(m_query_id);
		cells[1] = new StringCell(m_subject_id);
		cells[2] = new DoubleCell(m_identity);
		cells[3] = new IntCell(m_align_length);
		cells[4] = new IntCell(m_mismatches);
		cells[5] = new IntCell(m_gapopens);
		cells[6] = new IntCell(m_q_start);
		cells[7] = new IntCell(m_q_end);
		cells[8] = new IntCell(m_s_start);
		cells[9] = new IntCell(m_s_end);
		cells[10]= new DoubleCell(m_evalue.doubleValue());
		cells[11]= new DoubleCell(m_bitscore);
		String prefix = m_is_a ? "A" : "B";
		return new DefaultRow(new RowKey(prefix+m_id++), cells);
	}
	
	public String getQueryAccsn() {
		return m_query_id;
	}
	
	public String getSubjectAccsn() {
		return m_subject_id;
	}
	
	public int getBitScore() {
		return (int) Math.round(m_bitscore);
	}
	
	@Override
	public int compareTo(BLASTRecord o) {
		// use bitscore first (descending), then fallback to e-value (ascending)
		if (this.m_bitscore < o.m_bitscore) {
			return 1;
		} else if (this.m_bitscore > o.m_bitscore) {
			return -1;
		} else {
			// bitscore is equal so sort by ascending e-value
			return this.m_evalue.compareTo(o.m_evalue);
		}
	}

	public String getOtherKey(String key) {
		if (key.equals(m_subject_id)) {
			return m_query_id;
		} else {
			return m_subject_id;
		}
	}

	/**
	 * Is there a record in the bucket which involves the same query accession?
	 * @param bb
	 * @return
	 */
	public boolean involves(BLASTBucket bb) {
		for (BLASTRecord br : bb) {
			if (br.hasKey(this.getQueryAccsn())) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean hasKey(String k) {
		return (m_subject_id.equals(k) || m_query_id.equals(k));
	}
	
	public boolean isA() {
		return m_is_a;
	}
	
	@Override 
	public String toString() {
		return m_query_id + " -> "+m_subject_id +": "+m_evalue+" %id "+m_identity+", bitscore "+m_bitscore+"\n";
	}

	public BigDecimal getEvalue() {
		return m_evalue;
	}
	
	public boolean accept(BLASTRecord br, Map<String,BLASTBucket> hits) {
		return hits.get(br.getQueryAccsn()).isFirst(br);
	}

	/**
	 * Two BLAST records are considered equal if they involve the same accessions, regardless of other state
	 * (must be this way for reporting minimum output)
	 */
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof BLASTRecord)) {
			return false;
		}
		BLASTRecord b = (BLASTRecord) o;
		return ((this.m_query_id.equals(b.m_query_id) && this.m_subject_id.equals(b.m_subject_id)) || 
				(this.m_query_id.equals(b.m_subject_id) && this.m_subject_id.equals(b.m_query_id)) );
	}
	
	/**
	 * Must override <code>equals()</code> and <code>hashCode()</code> together. So...
	 */
	@Override
	public int hashCode() {
		return m_query_id.hashCode() + m_subject_id.hashCode();
	}
}
