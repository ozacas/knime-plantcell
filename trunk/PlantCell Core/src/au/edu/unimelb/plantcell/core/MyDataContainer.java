package au.edu.unimelb.plantcell.core;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;

/**
 * A data container which wraps the KNIME data type to provide uniquely prefix'ed rows without violating abstraction.
 * 
 * @author andrew.cassin
 *
 */
public class MyDataContainer {
	private BufferedDataContainer m_dc;
	private int m_id;
	private String m_prefix;
	
	public MyDataContainer(BufferedDataContainer bdc, String id_prefix) {
		assert(bdc != null);
		/*Logger l = Logger.getLogger("MyDataContainer");
		l.info("Found "+bdc.getTableSpec().getNumColumns()+" columns in spec with rows prefixed with: "+id_prefix);
		for (int i=0; i<bdc.getTableSpec().getNumColumns(); i++) {
			DataColumnSpec cs = bdc.getTableSpec().getColumnSpec(i);
			l.info("Column: "+cs.getName()+" has type "+cs.getType());
		}*/
		m_dc = bdc;
		m_id = 1;
		if (id_prefix == null)
			id_prefix = "Row";
		m_prefix = id_prefix;
	}
	
	/**
	 * add a row to the DataContainer, using the row key specified by the container (and NOT the input row)
	 */
	public void addRow(DataRow dr) {
		assert(dr != null);
		
		m_dc.addRowToTable(new DefaultRow(m_prefix+m_id++, dr));
	}
	
	/**
	 * Add a row to the container with the default row ID naming scheme (as established by the constructor called)
	 * 
	 * @param cells
	 */
	public void addRow(DataCell[] cells) {
		m_dc.addRowToTable(new DefaultRow(m_prefix+m_id++, cells));
	}

	/**
	 * Similar to <code>addRow()</code>. An error will be thrown if the rowid is not unique so be
	 * careful using this implementation.
	 * 
	 * @param rowid
	 * @param cells
	 */
	public void addRowWithID(String rowid, DataCell[] cells) {
		m_dc.addRowToTable(new DefaultRow(rowid, cells));
	}
	
	/**
	 * Returns the prefix of the last row id. It does not return the last row id if you called
	 * addRowWithID() instead of addRow().
	 * 
	 * @return
	 */
	public String lastRowID() {
		return m_prefix + (m_id-1);
	}
	
	public BufferedDataTable close() {
		m_dc.close();
		return m_dc.getTable();
	}

	public DataTableSpec getTableSpec() {
		return m_dc.getTableSpec();
	}
}
