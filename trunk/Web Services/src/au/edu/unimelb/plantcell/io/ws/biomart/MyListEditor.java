package au.edu.unimelb.plantcell.io.ws.biomart;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public class MyListEditor extends AbstractCellEditor implements TableCellEditor,ActionListener {
	/**
	 * not used internally
	 */
	private static final long serialVersionUID = -4265263942740761569L;
	
	private static final String EDIT = "edit";
	private JButton button;
	private Object new_user_input;		// may be a collection of items (if multi select)
	private Object old_user_input;	
	private JList<String> my_list;
	
	public MyListEditor(final JList<String> my_list) {
		button = new JButton();
		button.addActionListener(this);
		button.setActionCommand(EDIT);
		button.setBorderPainted(false);
		this.my_list = my_list;
	}
	
	/*********************** TableCellEditor methods ******************************/
	@Override
	public Object getCellEditorValue() {
		return new_user_input;
	}

	@Override
	public Component getTableCellEditorComponent(final JTable table, final Object value, boolean isSelected, int r, int c) {
		new_user_input = value;
		old_user_input = value;
		return button;
	}
	
	
	/********************** ActionListener methods **********************/
	@Override
	public void actionPerformed(ActionEvent ev) {
		if (EDIT.equals(ev.getActionCommand())) {
			 if (JOptionPane.showInputDialog(null, new JScrollPane(my_list),
                     "Select the items", JOptionPane.PLAIN_MESSAGE) != null) {
				 	new_user_input = my_list.getSelectedValuesList();
					// FALLTHRU
			 } else {
				 new_user_input = old_user_input;
			 }
			 fireEditingStopped();
		}
	}

}
