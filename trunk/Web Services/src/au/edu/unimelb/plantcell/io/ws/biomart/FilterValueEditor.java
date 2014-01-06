package au.edu.unimelb.plantcell.io.ws.biomart;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;

import au.edu.unimelb.plantcell.servers.biomart.Filter;
import au.edu.unimelb.plantcell.servers.biomart.FilterData;

/**
 * Supports single selection of a particular value or text editing where appropriate to enable
 * the user to construct the filter in the WYSIWYG way
 * 
 * @author andrew.cassin
 *
 */
public class FilterValueEditor extends AbstractCellEditor implements TableCellEditor {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7809110125724116800L;
	// this delegate is specific to the row being edited so you cannot assume what the 
	// instance is unless you know the row data
	private DefaultCellEditor m_delegate = null;
	private Object m_value;
	
	@Override
	public Object getCellEditorValue() {
		if (m_delegate == null)
			return m_value;
		return m_delegate.getCellEditorValue();
	}

	@Override
	public boolean isCellEditable(EventObject arg0) {
		return true;
	}

	@Override
	public boolean shouldSelectCell(EventObject arg0) {
		if (m_delegate == null)
			return true;
		return m_delegate.shouldSelectCell(arg0);
	}

	@Override
	public boolean stopCellEditing() {
		if (m_delegate == null) {
			fireEditingStopped();
			return true;
		}
		boolean ret = m_delegate.stopCellEditing();
		if (ret)
			fireEditingStopped();
		return ret;
	}
	
	@Override
	public void cancelCellEditing() {
		if (m_delegate == null)
			super.fireEditingCanceled();
		m_delegate.cancelCellEditing();
	}

	@Override
	public Component getTableCellEditorComponent(JTable t, Object val, boolean isSelected, int r, int c) {
		m_value              = null;
		m_delegate           = null;
		FilterTableModel ftm = (FilterTableModel) t.getModel();
		Filter v = ftm.getFilter(r);
		boolean has_type = (v != null && v.getType() != null);
		
		if (has_type && v.getType().equals("singleSelect")) {
			List<String> values = getFilterValues((Filter) v);
			JComboBox<String> cb = new JComboBox<String>(values.toArray(new String[0]));
			if (val != null)
				cb.setSelectedItem(val);
			m_delegate = new DefaultCellEditor(cb);
			return cb;
		} else if (has_type && v.getType().equals("multiSelect")) {
			List<String> values = getFilterValues((Filter) v);
			JList<String> list = new JList<String>();
			list.setListData(values.toArray(new String[0]));
			list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			if (JOptionPane.showInputDialog(null, new JScrollPane(list), 
																	"Select the items", JOptionPane.PLAIN_MESSAGE) != null) {
				List<String> new_values = list.getSelectedValuesList();
				m_value = new_values;
				return new JLabel(new_values.size() > 0 ? new_values.get(0) : "");
			}
			return null;
		} else if (has_type && v.getType().equals("upload")) {
			// pop up a modal dialog box and get the list from the user
			JTextArea input = new JTextArea();
			input.setRows(20);
			input.setColumns(60);
			input.setEditable(true);
			input.setWrapStyleWord(true);
			if (val != null)
				input.setText(val.toString());
			
			// cancel not chosen?
			if (JOptionPane.showInputDialog(null, new JScrollPane(input), null, JOptionPane.OK_CANCEL_OPTION) != null) 
				m_value = input.getText();
			
			return new JLabel(m_value.toString());
		} else {
			// assume JTextField
			JTextField tf = new JTextField();
			m_delegate    = new DefaultCellEditor(tf);
			return tf;
		}
	}

	private List<String> getFilterValues(Filter v) {
		ArrayList<String> values = new ArrayList<String>();
		for (FilterData fd : ((Filter)v).getValues().getValue()) {
			values.add(fd.getDisplayName());
		}
		Collections.sort(values);
		
		return values;
	}

}
