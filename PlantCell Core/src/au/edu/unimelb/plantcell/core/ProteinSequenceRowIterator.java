package au.edu.unimelb.plantcell.core;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;


/**
 * Iterator over {@link DataRow}'s where you are interested primarily in a single cell
 * which contains a protein sequence. Other cells in the row are available too, but this
 * implementation is not thread-safe. The iterator supports filtering of rows to suppress
 * sequence not suitable for iteration, or correction of sequence data on-the-fly using 
 * <code>nextSequence()</code>
 * 
 * @author andrew.cassin
 *
 */
public class ProteinSequenceRowIterator implements Iterator<DataRow> {
	private RowIterator m_it;
	private NodeLogger  m_logger;
	private int m_seq_col, m_rejected_count;
	private SequenceProcessor m_sp;
	private DataRow m_next_row;
	private SequenceValue  m_next_seq;
	private boolean m_want_next;
	private boolean m_cur_hasnext;
	
	public ProteinSequenceRowIterator(RowIterator it, int seq_idx) throws InvalidSettingsException {
		this(it, seq_idx, null, null);
	}
	
	public ProteinSequenceRowIterator(RowIterator it, int seq_idx, 
			NodeLogger logger, SequenceProcessor sp) throws InvalidSettingsException {
		m_logger  = logger;
		m_seq_col = seq_idx;
		if (seq_idx < 0)
			throw new InvalidSettingsException("Cannot find desired sequence column!");
		if (it == null) 
			throw new InvalidSettingsException("Programmer error: nothing to iterate!");
		m_it = it;
		m_sp = sp;
		m_rejected_count = 0;
		m_want_next = false;
		m_cur_hasnext = true;
	}

	@Override
	public boolean hasNext() {
		if (m_want_next) {
			return m_cur_hasnext;
		}
		while (m_it.hasNext()) {
			m_next_row = m_it.next();
			if (m_next_row == null) {
				m_want_next = true;
				m_cur_hasnext = false;
				return false;
			}
			DataCell c = m_next_row.getCell(m_seq_col);	
			if (c == null || c.isMissing()) {	
				if (m_logger != null) {
					m_logger.warn("Ignoring missing cell on row "+m_next_row.getKey().getString());
					continue;
				}
			}
			if (!(c instanceof SequenceValue)) {
				if (m_logger != null) {
					m_logger.warn("Encountered non-SequenceValue... ignored.");
					continue;
				}
			}
			SequenceValue sv = (SequenceValue) c;
			
			if (m_sp != null) {
				sv = m_sp.process(sv);
				if (sv == null) {
					m_rejected_count++;
					continue;		//	sequence processor says no to the current row so...
				}
				// FALL-THRU...
			}
			m_next_seq    = sv;
			m_want_next   = true;
			m_cur_hasnext = true;
			return true;
		}
		
		m_want_next = true;
		m_cur_hasnext = false;
		return false;
	}

	/**
	 * Where only one column (the sequence column) is required during iteration this
	 * method can be used to retrieve it. The sequence returned 
	 * is the result of calling the
	 * {@link SequenceProcessor}, which may modify the raw data, from the user-specified input cell
	 * 
	 * @return
	 * @throws Exception
	 */
	public SequenceValue nextSequence() {
		m_want_next = false;
		return m_next_seq;
	}
	

	/**
	 * Provides the full row, unmodified, from the input table. Rows which the {@link SequenceProcessor}
	 * rejects will not be considered further
	 * 
	 * @return
	 * @throws Exception
	 */
	@Override
	public DataRow next() {
		m_want_next = false;
		return m_next_row;
	}

	@Override
	public void remove() {
		// NO-OP: this implementation does not support mutation 
	}
	
	public int getRejectedRowCount() {
		return m_rejected_count;
	}
}
