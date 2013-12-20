package au.edu.unimelb.plantcell.io.ws.biomart;

import java.awt.Component;
import java.util.ArrayList;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
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
	private String m_value;
	
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
		return m_delegate.shouldSelectCell(arg0);
	}

	@Override
	public boolean stopCellEditing() {
		if (m_delegate == null) {
			fireEditingStopped();
			return true;
		}
		boolean ret = m_delegate.stopCellEditing();
		return ret;
	}

	@Override
	public Component getTableCellEditorComponent(JTable t, Object val, boolean isSelected, int r, int c) {
		m_value = null;
		m_delegate = null;
		FilterTableModel ftm = (FilterTableModel) t.getModel();
		Filter v = ftm.getFilter(r);
		boolean has_type = (v != null && v.getType() != null);
		
		// TODO BUG FIXME: for now multiple select is a combobox, will support proper multi selection later...
		if (has_type && (v.getType().equals("singleSelect") || v.getType().equals("multiSelect"))) {
			ArrayList<String> values = new ArrayList<String>();
			for (FilterData fd : ((Filter)v).getValues().getValue()) {
				values.add(fd.getDisplayName());
			}
			m_delegate = new DefaultCellEditor(new JComboBox<String>(values.toArray(new String[0])));
		} else if (has_type && v.getType().equals("upload")) {
			// pop up a modal dialog box and get the list from the user
			JOptionPane opt = new JOptionPane();
			JTextArea input = new JTextArea();
			input.setRows(20);
			input.setColumns(60);
			input.setEditable(true);
			input.setWrapStyleWord(true);
			m_value = JOptionPane.showInputDialog(null, new JScrollPane(input), "", JOptionPane.OK_CANCEL_OPTION);
			return opt;
		} else {
			// assume JTextField
			JTextField tf = new JTextField();
			m_delegate = new DefaultCellEditor(tf);
		}
		return m_delegate.getTableCellEditorComponent(t, "test", isSelected, r, c);
	}

}
