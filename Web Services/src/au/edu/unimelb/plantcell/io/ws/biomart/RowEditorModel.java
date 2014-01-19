package au.edu.unimelb.plantcell.io.ws.biomart;

import javax.swing.table.TableCellEditor;

/**
 * 
 * @author pcbrc.admin
 *
 */
public interface RowEditorModel {
	
	public void addEditorForRow(int row, TableCellEditor e );
	
	public void removeEditorForRow(int row);
	
	public TableCellEditor getEditor(int row);
}
