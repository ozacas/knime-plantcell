package au.edu.unimelb.plantcell.io.ws.biomart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.apache.commons.lang.ArrayUtils;

import au.edu.unimelb.plantcell.servers.biomart.Filter;
import au.edu.unimelb.plantcell.servers.biomart.FilterData;

/**
 * 
 * @author andrew.cassin
 *
 */
public class FilterTableModel extends AbstractTableModel implements TableModel,RowEditorModel {
	/**
	 * not used
	 */
	private static final long serialVersionUID = 7319973817591482606L;
	
	/**
	 * data for the table: a list of user filters
	 */
	private final List<Filter>       m_user_filters = new ArrayList<Filter>();			// index is row
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
	
	/**
	 * Same as per {@link append(Filter)} except that the value is also initialised.
	 * Used to load user-configured settings during configure dialog
	 * 
	 * @param f
	 * @param val
	 */
	public void appendWithValue(Filter f, Object val) {
		assert(f != null && val != null);
		m_user_values.put(f, val);
		append(f);
	}

	public void remove(Collection<Filter> to_be_removed) {
		for (Filter f : to_be_removed) {
			m_user_filters.remove(f);
			m_user_values.remove(f);
		}
		fireTableDataChanged();
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
		if (c == 5 && r < getRowCount()) {
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
		if (val != null) {
			return val;
		}
		return null;
	}
	
	private List<String> getFilterValues(Filter v) {
        ArrayList<String> values = new ArrayList<String>();
        for (FilterData fd : ((Filter)v).getValues().getValue()) {
                values.add(fd.getName());
        }
        Collections.sort(values);
       
        return values;
	}


	/*************************** RowEditorModel interface methods *****************************/
	
	@Override
	public void addEditorForRow(int row, TableCellEditor e) {
		// NO-OP for this class
	}

	@Override
	public void removeEditorForRow(int row) {
		// NO-OP for this class
	}

	@SuppressWarnings("unchecked")
	@Override
	public TableCellEditor getEditor(int row) {
		Logger.getAnonymousLogger().info("Filters: "+m_user_filters.size()+" "+row);
		Filter f = m_user_filters.get(row);
		String type = f.getType();
		// no biomart filter type defined? Ok... we hope default is good enough?
		if (type == null)
			return new DefaultCellEditor(new JTextField(m_user_values.get(f).toString()));
		
		// biomart filter suitable for a combobox?
		if (type.equals("singleSelect")) {
			List<String>  values = getFilterValues(f);
			JComboBox<String> cb = new JComboBox<String>(values.toArray(new String[0]));
			return new DefaultCellEditor(cb);
		} else if (type.equals("multiSelect")) {
			// pop up a modal dialog box and get the desired list from the user
			List<String> values = getFilterValues(f);
            JList<String> list = new JList<String>();
            list.setListData(values.toArray(new String[0]));
            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            Object val = m_user_values.get(f);
            if (val instanceof Collection) {
            	ArrayList<Integer> indices = new ArrayList<Integer>();
            	for (String s : ((Collection<String>)val)) {
                     indices.add(values.indexOf(s));
                }
            	list.setSelectedIndices(ArrayUtils.toPrimitive(indices.toArray(new Integer[0])));
            }
            return new MyListEditor(list);
		} else if (type.equals("upload")) {
			// pop up a modal dialog box and get the uploaded text from the user
			return new MyTextEditor(m_user_values.get(f));
		} else /* assume JTextField is ok */ {
			Object val = m_user_values.get(f);
			JTextField tf = new JTextField(val.toString());
			return new DefaultCellEditor(tf);
		}
	}

	/**
	 * Returns true if the specified filter is already present in the specified table model
	 * @param f must not be null
	 * @return true if found, false otherwise
	 */
	public boolean alreadyHasFilter(Filter f) {
		assert(f != null);
		return (m_user_filters.indexOf(f) >= 0);
	}



	
}
