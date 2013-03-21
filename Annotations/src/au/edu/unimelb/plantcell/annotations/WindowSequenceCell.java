package au.edu.unimelb.plantcell.annotations;

import org.knime.core.node.InvalidSettingsException;

import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Similar to a {@link SequenceCell} but this modifies <code>getStringValue()</code> and <code>getLength()</code> to return one window at a time.
 * Instances of this class are for in-memory use only, they should never be persisted. Its really more of an iterator, but needs to derive
 * from a cell to be compatible with the KNIME row API.
 * 
 * @author andrew.cassin
 *
 */
public class WindowSequenceCell extends SequenceCell {
	/**
	 * not used
	 */
	private static final long serialVersionUID = -4668767149193490953L;
	private SequenceValue m_sv;
	private int m_size, m_step;
	private int m_pos;	
	
	public WindowSequenceCell(SequenceValue sv, int size, int step) throws InvalidSettingsException {
		super(sv);
		assert(size > 0 && step > 0 && sv != null);
	
		m_size = size;
		m_step = step;
		m_sv   = sv;
		m_pos  = -1;
	}
	
	/**
	 * NB: must not side-effect internal state!
	 * @return true if there are more windows, false otherwise
	 */
	public boolean hasNextWindow() {
		// cant call getLength() as it will recurse infinitely
		return (m_pos < m_sv.getLength());
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
	
	/**
	 * Returns the length of the current window (NOT the whole sequence)
	 * @return
	 */
	@Override
	public int getLength() {
		return peekStringWindow().length();
	}
	
	public int getStart() {
		if (m_pos <= 0)
			return 1;
		return m_pos;
	}
	
	public int getEnd() {
		int end = getStart() + m_size;
		if (end > m_sv.getLength()) {
			return m_sv.getLength();
		}
		return end;
	}

	public String peekStringWindow() {
		int start = getStart() - 1;
		int end   = getEnd() - 1;
		String seq = m_sv.getStringValue();
		return seq.substring(start, end);
	}
}
