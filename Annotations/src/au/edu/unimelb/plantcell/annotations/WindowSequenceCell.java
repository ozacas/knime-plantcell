package au.edu.unimelb.plantcell.annotations;

import org.knime.core.node.InvalidSettingsException;

import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Similar to a {@link SequenceCell} but this modifies <code>getStringValue</code> to return one window at a time.
 * Instances of this class are for in-memory use only, they should never be persisted. Its really more of an interator.
 * 
 * @author andrew.cassin
 *
 */
public class WindowSequenceCell extends SequenceCell {
	/**
	 * not used
	 */
	private static final long serialVersionUID = -4668767149193490953L;
	
	private int m_size, m_step;
	private boolean m_shift;
	private int m_pos;
	private int m_total_windows;
	
	
	public WindowSequenceCell(SequenceValue sv, String window_method, int size, int step) throws InvalidSettingsException {
		super(sv);
		assert(size > 0 && step > 0 && sv != null);
		
		m_size = size;
		m_step = step;
		m_shift= window_method.toLowerCase().equals("overlapping");
		m_pos  = -1;
		int len = sv.getLength();
		
		if (m_shift) {
			// ie. overlapping
			if (len <= m_size) {
				m_total_windows = 1;
			} else {
				m_total_windows = len - m_size + 1;
			}
		} else {
			// discontiguous?
			m_total_windows = len / m_size;
			if (len % m_size > 0) 
				m_total_windows++;
		}
	}
	
	/**
	 * NB: must not side-effect internal state!
	 * @return true if there are more windows, false otherwise
	 */
	public boolean hasNextWindow() {
		return (m_pos < m_total_windows);
	}
	
	/**
	 * returns the next window and not the entire sequence. The returned sequence length may be less than the
	 * window size eg. at the end of the sequence
	 */
	@Override 
	public String getStringValue() {
		String ret = peekStringWindow();
		if (m_pos < 0) {
			m_pos = m_step + 1;
		} else {
			m_pos += m_step;
		}
		return ret;
	}
	
	public int getStart() {
		if (m_pos <= 0)
			return 1;
		return m_pos;
	}
	
	public int getEnd() {
		int end = getStart() + m_size;
		if (end > getLength()) {
			return getLength();
		}
		return end;
	}

	public String peekStringWindow() {
		int start = getStart();
		int end   = getEnd();
		String seq = super.getStringValue();
		return seq.substring(start - 1, end-1);
	}
}
