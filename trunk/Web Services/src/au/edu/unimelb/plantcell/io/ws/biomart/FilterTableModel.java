package au.edu.unimelb.plantcell.io.ws.biomart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import au.edu.unimelb.plantcell.servers.biomart.Filter;
import au.edu.unimelb.plantcell.servers.biomart.FilterData;

/**
 * 
 * @author andrew.cassin
 *
 */
public class FilterTableModel extends AbstractTableModel implements TableModel {
	/**
	 * not used
	 */
	private static final long serialVersionUID = 7319973817591482606L;
	
	/**
	 * data for the table: a list of user filters
	 */
	private final List<Filter>       m_user_filters = new ArrayList<Filter>();
	private final Map<Filter,Object> m_user_values  = new HashMap<Filter,Object>();
	private Vector<TableColumn>      cached_columns = null;


	public Filter getFilter(int r) {
		return m_user_filters.get(r);
	}
	
	public void append(Filter f) {
		if (f != null) {
			m_user_filters.add(f);
			fireTableDataChanged();
		}
	}
	
	public void clear() {
		m_user_filters.clear();
		m_user_values.clear();
		fireTableDataChanged();
	}
	
	public void remove(final Filter f) {
		if (f != null) {
			m_user_filters.remove(f);
			m_user_values.remove(f);
			fireTableDataChanged();
		}
	}

	private Vector<TableColumn> get_columns() {
		if (cached_columns != null)
			return cached_columns;
		
		Vector<TableColumn> cols = new Vector<TableColumn>();
		for (int i=0; i<6; i++) {
			TableColumn tc = new TableColumn();
			tc.setPreferredWidth(80);
			tc.setResizable(true);
			switch (i) {
			case 0:
				tc.setHeaderValue("Name"); break;
			case 1:
				tc.setHeaderValue("Description"); 
				tc.setMinWidth(0); tc.setMaxWidth(0); tc.setWidth(0); break;
			case 2:
				tc.setHeaderValue("Type"); break;
			case 3:
				tc.setHeaderValue("Attribute"); break;
			case 4:
				tc.setHeaderValue("Depends on"); 
				tc.setMinWidth(0); tc.setMaxWidth(0); tc.setWidth(0);
				break;
			case 5:
				tc.setHeaderValue("Value"); 
				tc.setCellEditor(new FilterValueEditor());
				break;
			}
			cols.add(tc);
		}
		cached_columns = cols;
		
		return cols;
	}
	
	@Override
	public String getColumnName(int c) {
		Vector<TableColumn> cols = get_columns();
		return cols.get(c).getHeaderValue().toString();
	}
	
	@Override
	public Class<?> getColumnClass(int c) {
		return (c == 5) ? Filter.class : String.class;
	}
	
	@Override
	public boolean isCellEditable(int r, int c) {
		return (c == get_columns().size() - 1);
	}

	@Override
	public int getRowCount() {
		return m_user_filters.size();
	}

	@Override
	public int getColumnCount() {
		return get_columns().size();
	}

	@Override
	public Object getValueAt(int r, int c) {
		assert(c < getColumnCount() && r < getRowCount());
		
		Filter f = m_user_filters.get(r);
		if (f == null)
			return "";
		switch (c) {
		case 0:
			return f.getDisplayName();
		case 1:
			return f.getDescription();
		case 2:
			return f.getType();
		case 3:
			return f.getAttribute();
		case 4:
			return f.getDependsOn();
		case 5:
			Object val = m_user_values.get(f);
			if (val == null) {
				StringBuilder sb = new StringBuilder();
				for (FilterData fd : f.getValues().getValue()) {
					sb.append(fd.getDisplayName());
					sb.append(" ");
				}
				return sb.toString().trim();
			} else {
				return val;
			}
		default:
				return "";
		}
	}

	@Override
	public void setValueAt(Object new_val, int r, int c) {
		if (c == 5) {
			Filter f = m_user_filters.get(r);
			String name = f.getName();
			if (name == null)
				name = "?";
			Logger.getAnonymousLogger().info("Set filter value to "+new_val.toString()+" for "+name+" rc="+r+","+c);

			if (f != null) {
				m_user_values.put(f, new_val);
				fireTableDataChanged();
			}
		}
	}

	/*
	 * Returns the user-specified value for a filter or null if there is no value for the chosen filter
	 */
	public Object getFilterUserValue(int r) {
		assert(r >= 0 && r < getRowCount());
		Filter f = m_user_filters.get(r);
		Object val = m_user_values.get(f);
		return (val != null) ? val.toString() : null;
	}

}
