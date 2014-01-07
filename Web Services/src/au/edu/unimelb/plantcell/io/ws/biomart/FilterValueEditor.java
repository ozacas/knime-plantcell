package au.edu.unimelb.plantcell.io.ws.biomart;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
public class FilterValueEditor extends AbstractCellEditor implements TableCellEditor,ActionListener {
	/**
	 * not used by this code
	 */
	private static final long serialVersionUID = -7809110125724116800L;

	/* action commands used by SOME editors for various supported biomart filters */
	private final String COMBO_ACTION = "combobox-edit";
	private final String TEXTFIELD_ACTION = "textfield-edit";
	
	/**
	 * editor must keep track of current value chosen by user during edit - note Object NOT String!
	 */
	private Object m_value = null;
	
	@Override
	public boolean isCellEditable(EventObject arg0) {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Component getTableCellEditorComponent(JTable t, Object val, boolean isSelected, int r, int c) {
		FilterTableModel ftm = (FilterTableModel) t.getModel();
		Filter v = ftm.getFilter(r);
		boolean has_type = (v != null && v.getType() != null);
		
		if (has_type && v.getType().equals("singleSelect")) {
			List<String> values = getFilterValues((Filter) v);
			JComboBox<String> cb = new JComboBox<String>(values.toArray(new String[0]));
			cb.addActionListener(this);
			cb.setActionCommand(COMBO_ACTION);
			if (val != null)
				cb.setSelectedItem(val);
			return cb;
		} else if (has_type && v.getType().equals("multiSelect")) {
			List<String> values = getFilterValues((Filter) v);
			JList<String> list = new JList<String>();
			list.setListData(values.toArray(new String[0]));
			list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			if (val instanceof Collection) {
				for (String s : ((Collection<String>)val)) {
					list.setSelectedValue(s, false);
				}
			}
			JComponent ret = null;
			if (JOptionPane.showInputDialog(null, new JScrollPane(list), 
																	"Select the items", JOptionPane.PLAIN_MESSAGE) != null) {
				List<String> new_values = list.getSelectedValuesList();
				m_value = new_values;
				ret = new JLabel(new_values.size() > 0 ? new_values.get(0) : "");
				// FALLTHRU
			}
			fireEditingStopped();
			return ret;
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
			if (JOptionPane.showInputDialog(null, new JScrollPane(input), null, JOptionPane.OK_CANCEL_OPTION) != null) {
				m_value = input.getText();
				fireEditingStopped();
			}
			return new JLabel(m_value.toString());
		} else {
			// assume JTextField
			JTextField tf = new JTextField();
			tf.addActionListener(this);
			tf.setActionCommand(TEXTFIELD_ACTION);
			tf.setText(val.toString());
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

	@Override
	public Object getCellEditorValue() {
		return m_value;
	}

	/**
	 * table editors for biomart filters that use ActionListener come here to update the value
	 * and stop editing. For example text fields where the user presses enter to accept the value come here.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed(ActionEvent e) {
		if (COMBO_ACTION.equals(e.getActionCommand())) {
			m_value = ((JComboBox<String>)e.getSource()).getSelectedItem();
		} else if (TEXTFIELD_ACTION.equals(e.getActionCommand())) {
			m_value = ((JTextField)e.getSource()).getText();
		}
		fireEditingStopped();
	}

}
