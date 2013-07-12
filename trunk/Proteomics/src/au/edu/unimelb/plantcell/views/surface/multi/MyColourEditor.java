package au.edu.unimelb.plantcell.views.surface.multi;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.jzy3d.colors.Color;

public class MyColourEditor extends AbstractCellEditor implements
		TableCellEditor, ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3372557714991121823L;


	private JButton button;
	private JColorChooser colorChooser;
	private JDialog dialog;
	protected static final String EDIT = "edit";
	private Color currentColor;
	
	public MyColourEditor() {		
		button = new JButton();
		button.setActionCommand(EDIT);
		button.addActionListener(this);
		//button.setBorderPainted(false);
		
		//Set up the dialog that the button brings up.
		colorChooser = new JColorChooser();
		dialog = JColorChooser.createDialog(button,
		                   "Pick a Color",
		                   true,  //modal
		                   colorChooser,
		                   this,  //OK button handler
		                   null); //no CANCEL button handler
	}

	public void actionPerformed(ActionEvent e) {
		if (EDIT.equals(e.getActionCommand())) {
			//The user has clicked the cell, so
			//bring up the dialog.
			
			java.awt.Color awt = new java.awt.Color(currentColor.r/255, currentColor.g/255, currentColor.b/255);
			button.setBackground(awt);
			button.setForeground(awt);
			colorChooser.setColor(awt);
			dialog.setVisible(true);
			
			fireEditingStopped(); //Make the renderer reappear.
			
		} else { //User pressed dialog's "OK" button.
			currentColor = new Color(colorChooser.getColor());
		}
	}

	public boolean isCellEditable(EventObject eo) {
		return true;
	}
	
	//Implement the one CellEditor method that AbstractCellEditor doesn't.
	public Object getCellEditorValue() {
		return currentColor;
	}

	//Implement the one method defined by TableCellEditor.
	public Component getTableCellEditorComponent(JTable table,
	                            Object value,
	                            boolean isSelected,
	                            int row,
	                            int column) {
		currentColor = (Color)value;
		return button;
	}

}
