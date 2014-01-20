package au.edu.unimelb.plantcell.io.ws.biomart;

import java.util.Vector;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * This table is like the superclass except that it has a model for the editor to use at a given (r,c)
 * rather than being limited to a single editor class per column. Based on code at http://www.javaworld.com/article/2077465/learn-java/java-tip-102--add-multiple-jtable-cell-editors-per-column.html
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class MyTable extends JTable {
	/**
	 * not used internally
	 */
	private static final long serialVersionUID = -7039195694589988550L;
	
	protected RowEditorModel rm;
	
	public MyTable()
	{
		super();
		rm = null;
	}
	
	public MyTable(TableModel tm)
	{
		super(tm);
		rm = null;
	}

	public MyTable(TableModel tm, TableColumnModel cm)
	{
		super(tm,cm);
		rm = null;
	}
	
	public MyTable(TableModel tm, TableColumnModel cm, ListSelectionModel sm)
	{
		super(tm,cm,sm);
		rm = null;
	}
	
	public MyTable(int rows, int cols)
	{
		super(rows,cols);
		rm = null;
	}
	
	@SuppressWarnings("rawtypes")
	public MyTable(final Vector rowData, final Vector columnNames)
	{
		super(rowData, columnNames);
		rm = null;
	}

	public MyTable(final Object[][] rowData, final Object[] colNames)
	{
		super(rowData, colNames);
		rm = null;
	}
	
	// new constructor
	public MyTable(TableModel tm, RowEditorModel rm)
	{
		super(tm,null,null);
		this.rm = rm;
	}

	public void setRowEditorModel(RowEditorModel rm)
	{
		this.rm = rm;
	}

	public RowEditorModel getRowEditorModel() {
		return rm;
	}
	
	@Override
	public TableCellEditor getCellEditor(int row, int col)
	{
		TableCellEditor tmpEditor = null;
		if (rm!=null)
			tmpEditor = rm.getEditor(row);
		if (tmpEditor!=null)
			return tmpEditor;
		return super.getCellEditor(row,col);
	}
}
