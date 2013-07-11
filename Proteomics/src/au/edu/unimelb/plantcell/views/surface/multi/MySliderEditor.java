package au.edu.unimelb.plantcell.views.surface.multi;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.TableCellEditor;

public class MySliderEditor extends AbstractCellEditor implements TableCellEditor {
	private JSpinner m_slider;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4657578671212907738L;

	@Override
	public Component getTableCellEditorComponent(JTable arg0, Object arg1, boolean arg2, int arg3, int arg4) {
		Integer new_value = (Integer) arg1;
		m_slider = new JSpinner(new SpinnerNumberModel(new_value.intValue(), 1, 100, 10));
		return m_slider;
	}

	@Override
	public Object getCellEditorValue() {
		return (Integer) m_slider.getValue();
	}

}
