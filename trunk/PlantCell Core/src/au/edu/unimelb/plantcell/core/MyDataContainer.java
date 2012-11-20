package au.edu.unimelb.plantcell.core;

import org.knime.core.data.DataCell;
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
	
	public void addRow(DataCell[] cells) {
		m_dc.addRowToTable(new DefaultRow(m_prefix+m_id++, cells));
	}

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
