package au.edu.unimelb.plantcell.misc.biojava;

import java.util.Iterator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;


public class ColumnIterator implements Iterator<DataCell> {
	private RowIterator m_it;
	private int m_idx;
	private DataRow m_last_row;
	
	public ColumnIterator(RowIterator it, int column_index) {
		m_it = it;
		m_idx= column_index;
		m_last_row = null;
	}

	@Override
	public boolean hasNext() {
		return m_it.hasNext();
	}

	@Override
	public DataCell next() {
		m_last_row = m_it.next();
		return m_last_row.getCell(m_idx);
	}

	@Override
	public void remove() {		
	}

	public String lastRowID() {
		return m_last_row.getKey().getString();
	}

	// abstraction violation: should not be exposed but need to improve the BioJava node interals first!
	public DataRow lastRow() {
		return m_last_row;
	}
	
}
