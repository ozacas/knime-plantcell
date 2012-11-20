package au.edu.unimelb.plantcell.io.read.fasta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;

import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * This class iterates over the input table, returning batches of the input sequences.
 * It does not support removal from the underlying table.
 * 
 * @author andrew.cassin
 *
 */
public class BatchSequenceRowIterator implements Iterator<List<SequenceValue>> {
	private RowIterator m_it = null;
	private int m_seq = -1;
	private int nseqs_limit = 25;
	private int seq_size_limit = 0;
	private DataRow m_last_row;
	private SequenceProcessor m_sp;
	private boolean m_save_rows;
	private List<DataRow> m_row_batch;	// only non-null if m_save_rows == true
	
	/**
	 * Equivalent to <code>BatchSequenceRowIterator(it, seq_cell_idx, 25, BatchFastaIterator.DEFAULT_TOTAL_MB_LIMIT)</code>
	 * 
	 * @param it
	 * @param seq_cell_idx
	 */
	public BatchSequenceRowIterator(RowIterator it, int seq_cell_idx) {
		this(it, seq_cell_idx, 25, BatchFastaIterator.DEFAULT_TOTAL_MB_LIMIT, null);
	}
	
	/**
	 * Constructs the iterator with all the necessary parameters. If a {@link SequenceProcessor}
	 * is specified, it will be called for each candidate sequence. If it returns non-null, the
	 * sequence will be added to the batch.
	 */
	public BatchSequenceRowIterator(RowIterator it, int seq_cell_idx, int batch_size, int total_size_limit, SequenceProcessor sequenceProcessor) {
		assert(seq_cell_idx >= 0 && it != null);
		m_it           = it;
		m_seq          = seq_cell_idx;
		nseqs_limit    = batch_size;
		seq_size_limit = total_size_limit;
		m_last_row     = null;
		m_sp           = sequenceProcessor;
		m_save_rows    = false;
		m_row_batch    = null;
	}
	
	@Override
	public boolean hasNext() {
		return m_it.hasNext();
	}

	@Override
	public List<SequenceValue> next() {
		List<SequenceValue> batch = new ArrayList<SequenceValue>();
		int total_size_bytes = 0;
		if (m_save_rows) {
			m_row_batch = new ArrayList<DataRow>();
		}
		
		// we know that there must be a row: hasNext() last returned true
		do {
			m_last_row = m_it.next();
			DataCell c = m_last_row.getCell(m_seq);
			if (c != null && !c.isMissing() && (c instanceof SequenceValue)) {
				SequenceValue sv = (SequenceValue) c;
				// sequence acceptable for inclusion into the batch?
				if (m_sp != null) {
					sv = m_sp.process(sv);
				}
				if (sv != null) {
					total_size_bytes += sv.getLength();
					batch.add(sv);
					if (m_save_rows && m_row_batch != null) {
						m_row_batch.add(m_last_row);
					}
				}
			}
			if (seq_size_limit > 0 && total_size_bytes > seq_size_limit)
				break;
		} while (batch.size() < nseqs_limit && hasNext());
		
		// user convenience: return null if batch is empty at EOF
		if (batch.size() < 1) 
			return null;
		
		return batch;
	}

	@Override
	public void remove() {
		// NO-OP: not implemented
	}

	public Map<UniqueID,SequenceValue> nextAsMap() {
		List<SequenceValue> batch = next();
		if (batch == null)
			return null;
		HashMap<UniqueID,SequenceValue> ret = new HashMap<UniqueID,SequenceValue>();
		boolean first = true;
		for (SequenceValue sv : batch) {
			ret.put(new UniqueID(first), sv);
			first = false;
		}
		return ret;
	}

	
	public void setSaveRows(boolean b) {
		m_save_rows = b;
	}

	/**
	 * Only non-null if setSaveRows(true) is in effect
	 * @return
	 */
	public List<DataRow> lastBatchRows() {
		return m_row_batch;
	}
}
