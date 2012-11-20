/*********************************************************************
*
*      Copyright (C) 2002 Andrew Khan
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
***************************************************************************/

package jxl.write.biff;

import java.io.IOException;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ArrayList;

import common.Assert;
import common.Logger;

import jxl.Cell;
import jxl.LabelCell;
import jxl.NumberCell;
import jxl.CellType;
import jxl.Sheet;
import jxl.LabelCell;
import jxl.DateCell;
import jxl.BooleanCell;
import jxl.Hyperlink;
import jxl.Range;
import jxl.SheetSettings;
import jxl.WorkbookSettings;
import jxl.CellView;
import jxl.Image;
import jxl.HeaderFooter;
import jxl.format.CellFormat;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.PageOrientation;
import jxl.format.PaperSize;
import jxl.write.Blank;
import jxl.write.WritableWorkbook;
import jxl.write.WritableSheet;
import jxl.write.WritableCell;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.DateTime;
import jxl.write.WriteException;
import jxl.write.Boolean;
import jxl.write.WritableHyperlink;
import jxl.write.WritableImage;

import jxl.biff.IntegerHelper; 
import jxl.biff.WritableRecordData;
import jxl.biff.FormattingRecords;
import jxl.biff.EmptyCell;
import jxl.biff.XFRecord;
import jxl.biff.NumFormatRecordsException;
import jxl.biff.FormulaData;
import jxl.biff.SheetRangeImpl;
import jxl.biff.IndexMapping;
import jxl.biff.WorkspaceInformationRecord;

import jxl.biff.drawing.Chart;
import jxl.biff.drawing.DrawingGroupObject;
import jxl.biff.drawing.Drawing;

/**
 * A writable sheet.  This class contains implementation of all the
 * writable sheet methods which may be invoke by the API
 */
class WritableSheetImpl implements WritableSheet
{
  /**
   * The logger
   */
  private static Logger logger = Logger.getLogger(WritableSheetImpl.class);
    
  /**
   * The name of this sheet
   */
  private String name;
  /**
   * A handle to the output file which the binary data is written to
   */
  private File outputFile;
  /**
   * The rows within this sheet
   */
  private RowRecord[] rows;
  /**
   * A handle to workbook format records
   */
  private FormattingRecords formatRecords;
  /**
   * A handle to the shared strings used by this workbook
   */
  private SharedStrings     sharedStrings;

  /**
   * The list of non-default column formats
   */
  private TreeSet columnFormats;

  /**
   * The list of hyperlinks
   */
  private ArrayList hyperlinks;

  /**
   * The list of merged ranged
   */
  private MergedCells mergedCells;

  /**
   * A number of rows.  This is a count of the maximum row number + 1
   */
  private int numRows;

  /**
   * The number of columns.  This is a count of the maximum column number + 1
   */
  private int numColumns;

  /**
   * The environment specific print record, copied from the read spreadsheet
   */
  private PLSRecord plsRecord;

  /**
   * The buttons property set
   */
  private ButtonPropertySetRecord buttonPropertySet;

  /**
   * A flag indicating that this sheet is a chart only
   */
  private boolean chartOnly;

  /**
   * The data validations on this page
   */
  private DataValidation dataValidation;

  /**
   * Array of row page breaks
   */
  private ArrayList rowBreaks;

  /**
   * The drawings on this sheet
   */
  private ArrayList drawings;

  /**
   * The images on this sheet.  This is a subset of the drawings list
   */
  private ArrayList images;

  /**
   * Drawings modified flag.  Set to true if the drawings list has
   * been modified
   */
  private boolean drawingsModified;

  /**
   * The settings for this sheet
   */
  private SheetSettings settings;

  /**
   * The sheet writer engine
   */
  private SheetWriter sheetWriter;

  /**
   * The settings for the workbook
   */
  private WorkbookSettings workbookSettings;

  /**
   * The workbook
   */
  private WritableWorkbookImpl workbook;

  /**
   * The amount by which to grow the rows array
   */
  private final static int rowGrowSize = 10;

  /**
   * The maximum number of rows excel allows in a worksheet
   */
  private final static int numRowsPerSheet = 65536;

  /**
   * The maximum number of characters permissible for a sheet name
   */ 
  private final static int maxSheetNameLength = 31;

  /**
   * The illegal characters for a sheet name
   */
  private final static char[] illegalSheetNameCharacters = 
    new char[] {'*', ':', '?', '\\'}; 

  /**
   * The supported file types
   */
  private static final String[] imageTypes = new String[] {"png"};

  /**
   * The comparator for column info record
   */
  private static class ColumnInfoComparator implements Comparator
  {
    /**
     * Equals method
     * 
     * @param o the object to compare
     * @return TRUE if equal, FALSE otherwise
     */
    public boolean equals(Object o)
    {
      return o == this;
    }

    /**
     * Comparison function for to ColumnInfoRecords
     * 
     * @param o2 first object to compare
     * @param o1 second object to compare
     * @return the result of the comparison
     */
    public int compare(Object o1, Object o2)
    {
      if (o1 == o2)
      {
        return 0;
      }

      Assert.verify(o1 instanceof ColumnInfoRecord);
      Assert.verify(o2 instanceof ColumnInfoRecord);
      
      ColumnInfoRecord ci1 = (ColumnInfoRecord) o1;
      ColumnInfoRecord ci2 = (ColumnInfoRecord) o2;

      return ci1.getColumn() - ci2.getColumn();
    }
  }

  /**
   * Constructor
   * 
   * @param fr the formatting records used by the workbook
   * @param of the output file to write the binary data
   * @param f the fonts used by the workbook
   * @param n the name of this sheet
   * @param ss the shared strings used by the workbook
   * @param ws the workbook settings
   */
  public WritableSheetImpl(String n, 
                           File of, 
                           FormattingRecords fr, 
                           SharedStrings ss,
                           WorkbookSettings ws,
                           WritableWorkbookImpl ww)
  {
    name = validateName(n);
    outputFile = of;
    rows = new RowRecord[0];
    numRows = 0;
    numColumns = 0;
    chartOnly = false;
    workbook = ww;

    formatRecords = fr;
    sharedStrings = ss;
    workbookSettings = ws;
    drawingsModified = false;
    columnFormats   = new TreeSet(new ColumnInfoComparator());
    hyperlinks      = new ArrayList();
    mergedCells     = new MergedCells(this);
    rowBreaks       = new ArrayList();
    drawings        = new ArrayList();
    images          = new ArrayList();
    settings        = new SheetSettings();


    sheetWriter = new SheetWriter(outputFile,
                                  this, 
                                  workbookSettings);
  }

  /**
   * Returns the cell specified at this row and at this column
   * 
   * @param column the column number
   * @param row the row number
   * @return the cell at the specified co-ordinates
   */
  public Cell getCell(int column, int row)
  {
    return getWritableCell(column, row);
  }

  /**
   * Returns the cell specified at this row and at this column
   * 
   * @param column the column number
   * @param row the row number
   * @return the cell at the specified co-ordinates
   */
  public WritableCell getWritableCell(int column, int row)
  {
    WritableCell c = null;

    if (row < rows.length && rows[row] != null)
    {
      c = rows[row].getCell(column);
    }

    if (c == null)
    {
      c = new EmptyCell(column, row);
    }
    
    return c;
  }

  /**
   * Returns the number of rows in this sheet
   * 
   * @return the number of rows in this sheet
   */
  public int getRows()
  {
    return numRows;
  }

  /**
   * Returns the number of columns in this sheet
   * 
   * @return the number of columns in this sheet
   */
  public int getColumns()
  {
    return numColumns;
  }

  /**
   * Gets the cell whose contents match the string passed in.
   * If no match is found, then null is returned.  The search is performed
   * on a row by row basis, so the lower the row number, the more
   * efficiently the algorithm will perform
   * 
   * @param contents the string to match
   * @return the Cell whose contents match the parameter, null if not found
   */
  public Cell findCell(String contents)
  {
    Cell cell = null;
    boolean found = false;
    
    for (int i = 0 ; i < getRows() && found == false; i++)
    {
      Cell[] row = getRow(i);
      for (int j = 0 ; j < row.length && found == false; j++)
      {
        if (row[j].getContents().equals(contents))
        {
          cell = row[j];
          found = true;
        }
      }
    }

    return cell;
  }

  /**
   * Gets the cell whose contents match the string passed in.
   * If no match is found, then null is returned.  The search is performed
   * on a row by row basis, so the lower the row number, the more
   * efficiently the algorithm will perform.  This method differs
   * from the findCell methods in that only cells with labels are
   * queried - all numerical cells are ignored.  This should therefore
   * improve performance.
   * 
   * @param contents the string to match
   * @return the Cell whose contents match the paramter, null if not found
   */
  public LabelCell findLabelCell(String contents)
  {
    LabelCell cell = null;
    boolean found = false;
    
    for (int i = 0 ; i < getRows() && found == false; i++)
    {
      Cell[] row = getRow(i);
      for (int j = 0 ; j < row.length && found == false; j++)
      {
        if ( ( row[j].getType() == CellType.LABEL ||
               row[j].getType() == CellType.STRING_FORMULA) &&
            row[j].getContents().equals(contents))
        {
          cell = (LabelCell) row[j];
          found = true;
        }
      }
    }

    return cell;
  }

  /**
   * Gets all the cells on the specified row
   * 
   * @param row the rows whose cells are to be returned
   * @return the cells on the given row
   */
  public Cell[] getRow(int row)
  {
    // Find the last non-null cell
    boolean found = false;
    int col = numColumns - 1;
    while (col >= 0 && !found)
    {
      if (getCell(col, row).getType() != CellType.EMPTY)
      {
        found = true;
      }
      else
      {
        col--;
      }
    }

    // Only create entries for non-empty cells
    Cell[] cells = new Cell[col+1];

    for (int i = 0; i <= col; i++)
    {
      cells[i] = getCell(i, row);
    }
    return cells;
  }

  /**
   * Gets all the cells on the specified column
   * 
   * @param col the column whose cells are to be returned
   * @return the cells on the specified column
   */
  public Cell[] getColumn(int col)
  {
    // Find the last non-null cell
    boolean found = false;
    int row = numRows - 1;

    while (row >= 0 && !found)
    {
      if (getCell(col, row).getType() != CellType.EMPTY)
      {
        found = true;
      }
      else
      {
        row--;
      }
    }

    // Only create entries for non-empty cells
    Cell[] cells = new Cell[row+1];

    for (int i = 0; i <= row; i++)
    {
      cells[i] = getCell(col, i);
    }
    return cells;
  }

  /**
   * Gets the name of this sheet
   * 
   * @return the name of the sheet
   */
  public String getName()
  {
    return name;
  }

  /**
   * Inserts a blank row into this spreadsheet.  If the row is out of range
   * of the rows in the sheet, then no action is taken
   *
   * @param row the row to insert
   */
  public void insertRow(int row)
  {
    if (row < 0 || row >= numRows)
    {
      return;
    }

    // Create a new array to hold the new rows.  Grow it if need be
    RowRecord[] oldRows = rows;

    if (numRows == rows.length)
    {
      rows = new RowRecord[oldRows.length + rowGrowSize];
    }
    else
    {
      rows = new RowRecord[oldRows.length];
    }

    // Copy in everything up to the new row
    System.arraycopy(oldRows, 0, rows, 0, row);
    
    // Copy in the remaining rows
    System.arraycopy(oldRows, row, rows, row+1, numRows - row);

    // Increment all the internal row number by one
    for (int i = row+1; i <= numRows; i++)
    {
      if (rows[i] != null)
      {
        rows[i].incrementRow();
      }
    }

    // Adjust any hyperlinks
    HyperlinkRecord hr = null;
    Iterator i = hyperlinks.iterator();
    while (i.hasNext())
    {
      hr = (HyperlinkRecord) i.next();
      hr.insertRow(row);
    }

    // Adjust any data validations
    if (dataValidation != null)
    {
      dataValidation.insertRow(row);
    }

    // Adjust any merged cells
    mergedCells.insertRow(row);

    // Adjust any page breaks
    ArrayList newRowBreaks = new ArrayList();
    Iterator ri = rowBreaks.iterator();
    while (ri.hasNext())
    {
      int val = ( (Integer) ri.next()).intValue();
      if (val >= row)
      {
        val++;
      }

      newRowBreaks.add(new Integer(val));
    }
    rowBreaks = newRowBreaks;

    // Handle interested cell references on the main workbook
    if (workbookSettings.getFormulaAdjust())
    {
      workbook.rowInserted(this, row);
    }

    // Adjust the maximum row record
    numRows++;
  }

  /**
   * Inserts a blank column into this spreadsheet.  If the column is out of 
   * range of the columns in the sheet, then no action is taken
   *
   * @param col the column to insert
   */
  public void insertColumn(int col)
  {
    if (col < 0 || col >= numColumns)
    {
      return;
    }

    // Iterate through all the row records adding in the column
    for (int i = 0 ; i < numRows ; i++)
    {
      if (rows[i] != null)
      {
        rows[i].insertColumn(col);
      }
    }

    // Adjust any hyperlinks
    HyperlinkRecord hr = null;
    Iterator i = hyperlinks.iterator();
    while (i.hasNext())
    {
      hr = (HyperlinkRecord) i.next();
      hr.insertColumn(col);
    }

    // Iterate through the column views, incrementing the column number
    i = columnFormats.iterator();
    while (i.hasNext())
    {
      ColumnInfoRecord cir = (ColumnInfoRecord) i.next();

      if (cir.getColumn() >= col)
      {
        cir.incrementColumn();
      }
    }

    // Handle any data validations
    if (dataValidation != null)
    {
      dataValidation.insertColumn(col);
    }

    // Adjust any merged cells
    mergedCells.insertColumn(col);

    // Handle interested cell references on the main workbook
    if (workbookSettings.getFormulaAdjust())
    {
      workbook.columnInserted(this, col);
    }

    numColumns++;
  }

  /**
   * Removes a column from this spreadsheet.  If the column is out of range
   * of the columns in the sheet, then no action is taken
   *
   * @param col the column to remove
   */
  public void removeColumn(int col)
  {
    if (col < 0 || col >= numColumns)
    {
      return;
    }

    // Iterate through all the row records removing the column
    for (int i = 0 ; i < numRows ; i++)
    {
      if (rows[i] != null)
      {
        rows[i].removeColumn(col);
      }
    }

    // Adjust any hyperlinks
    HyperlinkRecord hr = null;
    Iterator i = hyperlinks.iterator();
    while (i.hasNext())
    {
      hr = (HyperlinkRecord) i.next();

      if (hr.getColumn()      == col &&
          hr.getLastColumn()  == col)
      {
        // The row with the hyperlink on has been removed, so get
        // rid of it from the list
        hyperlinks.remove(hyperlinks.indexOf(hr));
      }
      else
      {
        hr.removeColumn(col);
      }
    }

    // Adjust any data validations
    if (dataValidation != null)
    {
      dataValidation.removeColumn(col);
    }

    // Adjust any merged cells
    mergedCells.removeColumn(col);

    // Iterate through the column views, decrementing the column number
    i = columnFormats.iterator();
    ColumnInfoRecord removeColumn = null;
    while (i.hasNext())
    {
      ColumnInfoRecord cir = (ColumnInfoRecord) i.next();

      if (cir.getColumn() == col)
      {
        removeColumn = cir;
      }
      else if (cir.getColumn() > col)
      {
        cir.decrementColumn();
      }
    }

    if (removeColumn != null)
    {
      columnFormats.remove(removeColumn);
    }

    // Handle interested cell references on the main workbook
    if (workbookSettings.getFormulaAdjust())
    {
      workbook.columnRemoved(this, col);
    }

    numColumns--;
  }

  /**
   * Removes a row from this spreadsheet.  If the row is out of 
   * range of the columns in the sheet, then no action is taken
   *
   * @param row the row to remove
   */
  public void removeRow(int row)
  {
    if (row < 0 || row >= numRows)
    {
      return;
    }

    // Create a new array to hold the new rows.  Grow it if need be
    RowRecord[] oldRows = rows;

    rows = new RowRecord[oldRows.length];

    // Copy in everything up to the row to be removed
    System.arraycopy(oldRows, 0, rows, 0, row);
    
    // Copy in the remaining rows
    System.arraycopy(oldRows, row + 1, rows, row, numRows - (row + 1));

    // Decrement all the internal row numbers by one
    for (int i = row; i < numRows; i++)
    {
      if (rows[i] != null)
      {
        rows[i].decrementRow();
      }
    }

    // Adjust any hyperlinks
    HyperlinkRecord hr = null;
    Iterator i = hyperlinks.iterator();
    while (i.hasNext())
    {
      hr = (HyperlinkRecord) i.next();

      if (hr.getRow()      == row &&
          hr.getLastRow()  == row)
      {
        // The row with the hyperlink on has been removed, so get
        // rid of it from the list
        i.remove();
      }
      else
      {
        hr.removeRow(row);
      }
    }

    // Adjust any data validations
    if (dataValidation != null)
    {
      dataValidation.removeRow(row);
    }

    // Adjust any merged cells
    mergedCells.removeRow(row);

    // Adjust any page breaks
    ArrayList newRowBreaks = new ArrayList();
    Iterator ri = rowBreaks.iterator();
    while (ri.hasNext())
    {
      int val = ( (Integer) ri.next()).intValue();

      if (val != row)
      {
        if (val > row)
        {
          val--;
        }
        
        newRowBreaks.add(new Integer(val));
      }
    }

    rowBreaks = newRowBreaks;

    // Handle interested cell references on the main workbook
    if (workbookSettings.getFormulaAdjust())
    {
      workbook.rowRemoved(this, row);
    }

    // Adjust the maximum row record
    numRows--;
  }

  /**
   * Adds the cell to this sheet.  If the cell has already been added to 
   * this sheet or another sheet, a WriteException is thrown.  If the
   * position to be occupied by this cell is already taken, the incumbent
   * cell is replaced.
   * The cell is then marked as referenced, and its formatting information 
   * registered with the list of formatting records updated if necessary
   * The RowsExceededException may be caught if client code wishes to
   * explicitly trap the case where too many rows have been written
   * to the current sheet.  If this behaviour is not desired, it is
   * sufficient simply to handle the WriteException, since this is a base
   * class of RowsExceededException
   * 
   * @exception WriteException 
   * @exception RowsExceededException
   * @param cell the cell to add
   */
  public void addCell(WritableCell cell) 
    throws WriteException, RowsExceededException
  {
    if (cell.getType() == CellType.EMPTY)
    {
      if (cell != null && cell.getCellFormat() == null)
      {
        // return if it's a blank cell with no particular cell formatting
        // information
        return;
      }
    }
    
    CellValue cv = (CellValue) cell;

    if (cv.isReferenced())
    {
      throw new JxlWriteException(JxlWriteException.cellReferenced);
    }

    int row = cell.getRow();
    RowRecord rowrec = getRowRecord(row);
    rowrec.addCell(cv);

    // Adjust the max rows and max columns accordingly
    numRows = Math.max(row+1, numRows);
    numColumns = Math.max(numColumns, rowrec.getMaxColumn());

    // Indicate this cell is now part of a worksheet, so that it can't be
    // added anywhere else
    cv.setCellDetails(formatRecords, sharedStrings, this);
  }

  /** 
   * Gets the row record at the specified row number, growing the
   * array as needs dictate
   * 
   * @param row the row number we are interested in
   * @return the row record at the specified row
   * @exception RowsExceededException
   */
  private RowRecord getRowRecord(int row) throws RowsExceededException
  {
    if (row >= numRowsPerSheet)
    {
      throw new RowsExceededException();
    }

    // Grow the array of rows if needs be
    // Thanks to Brendan for spotting the flaw in merely adding on the
    // grow size
    if (row >= rows.length)
    {
      RowRecord[] oldRows = rows;
      rows = new RowRecord[Math.max(oldRows.length + rowGrowSize, row+1)];
      System.arraycopy(oldRows, 0, rows, 0, oldRows.length);
      oldRows = null;
    }

    RowRecord rowrec = rows[row];

    if (rowrec == null)
    {
      rowrec = new RowRecord(row);
      rows[row] = rowrec;
    }

    return rowrec;
  }

  /**
   * Gets the row record for the specified row
   * 
   * @param r the row
   * @return the row record
   */
  RowRecord getRowInfo(int r)
  {
    if (r < 0 || r > rows.length)
    {
      return null;
    }

    return rows[r];
  }

  /**
   * Gets the column info record for the specified column
   *
   * @param c the column
   * @return the column record
   */
  ColumnInfoRecord getColumnInfo(int c)
  {
    Iterator i = columnFormats.iterator();
    ColumnInfoRecord cir = null;
    boolean stop = false;

    while (i.hasNext() && !stop)
    {
      cir = (ColumnInfoRecord) i.next();

      if (cir.getColumn() >= c)
      {
        stop = true;    
      }
    }

    if (!stop)
    {
      return null;
    }

    return cir.getColumn() == c ? cir : null;
  }

  /**
   * Sets the name of this worksheet
   * 
   * @param n the name of this sheet
   */
  public void setName(String n)
  {
    name = n;
  }

  /**
   * Sets the hidden status of this sheet
   * 
   * @param h the hiden flag
   * @deprecated use the settings bean instead
   */
  public void setHidden(boolean h)
  {
    settings.setHidden(h);
  }

  /**
   * Indicates whether or not this sheet is protected
   * 
   * @param prot protected flag
   * @deprecated use the settings bean instead
   */
  public void setProtected(boolean prot)
  {
    settings.setProtected(prot);
  }

  /**
   * Sets this sheet as selected
   * @deprecated use the settings bean
   */
  public void setSelected()
  {
    settings.setSelected();
  }
  
  /**
   * Retrieves the hidden status of this sheet
   * 
   * @return TRUE if hidden, FALSE otherwise
   * @deprecated in favour of the getSettings() method
   */
  public boolean isHidden()
  {
    return settings.isHidden();
  }

  /**
   * Sets the width (in characters) for a particular column in this sheet
   * 
   * @param col the column whose width to set
   * @param width the width of the column in characters
   */
  public void setColumnView(int col, int width)
  {
    CellView cv = new CellView();
    cv.setSize(width * 256);
    setColumnView(col, cv);
  }

  /**
   * Sets the width (in characters) and format options for a 
   * particular column in this sheet
   * 
   * @param col the column to set
   * @param width the width in characters
   * @param format the formt details for the column
   */
  public void setColumnView(int col, int width, CellFormat format)
  {
    CellView cv = new CellView();
    cv.setSize(width * 256);
    cv.setFormat(format);
    setColumnView(col, cv);
  }

  /** 
   * Sets the view for this column
   *
   * @param col the column on which to set the view
   * @param view the view to set
   */
  public void setColumnView(int col, CellView view)
  {
    XFRecord xfr =  (XFRecord) view.getFormat();
    if (xfr == null)
    {
      Styles styles = getWorkbook().getStyles();
      xfr = (XFRecord) styles.getNormalStyle();
    }

    try
    {
      if (!xfr.isInitialized())
      {
        formatRecords.addStyle(xfr);
      }
      
      int width = view.depUsed() ? view.getDimension() * 256 : view.getSize();

      ColumnInfoRecord cir = new ColumnInfoRecord(col, 
                                                  width, 
                                                  xfr);

      if (view.isHidden())
      {
        cir.setHidden(true);
      }

      if (!columnFormats.contains(cir))
      {
        columnFormats.add(cir);
      }
      else
      {
        boolean removed = columnFormats.remove(cir);
        columnFormats.add(cir);
      }
    }
    catch (NumFormatRecordsException e)
    {
      logger.warn("Maximum number of format records exceeded.  Using " +
                  "default format.");

      ColumnInfoRecord cir = new ColumnInfoRecord
        (col, view.getDimension()*256, 
         (XFRecord) WritableWorkbook.NORMAL_STYLE);
      if (!columnFormats.contains(cir))
      {
        columnFormats.add(cir);
      }
    }
  }


  /**
   * Sets the height of the specified row, as well as its collapse status
   *
   * @param row the row to be formatted
   * @param height the row height in points
   * @exception RowsExceededException
   */
  public void setRowView(int row, int height) throws RowsExceededException
  {
    setRowView(row, height, false);
  }

  /**
   * Sets the height of the specified row, as well as its collapse status
   *
   * @param row the row to be formatted
   * @param collapsed indicates whether the row is collapsed
   * @exception jxl.write.biff.RowsExceededException
   */
  public void setRowView(int row, boolean collapsed)
    throws RowsExceededException
  {
    RowRecord rowrec = getRowRecord(row);
    rowrec.setCollapsed(collapsed);
  }

  /**
   * Sets the height of the specified row, as well as its collapse status
   *
   * @param row the row to be formatted
   * @param height the row height in 1/20th of a point
   * @param collapsed indicates whether the row is collapsed
   * @param zeroHeight indicates that the row has zero height
   * @exception RowsExceededException
   */
  public void setRowView(int row, int height, 
                         boolean collapsed)
                         throws RowsExceededException
  {
    RowRecord rowrec = getRowRecord(row);
    rowrec.setRowHeight(height);
    rowrec.setCollapsed(collapsed);
  }

  /**
   * Writes out this sheet.  This functionality is delegated off to the 
   * SheetWriter class in order to reduce the bloated nature of this source
   * file
   *
   * @exception IOException 
   */
  public void write() throws IOException
  {
    boolean dmod = drawingsModified;
    if (workbook.getDrawingGroup() != null)
    {
      dmod |= workbook.getDrawingGroup().hasDrawingsOmitted();
    }

    sheetWriter.setWriteData(rows, 
                             rowBreaks, 
                             hyperlinks, 
                             mergedCells, 
                             columnFormats );
    sheetWriter.setDimensions(getRows(), getColumns());
    sheetWriter.setSettings(settings);
    sheetWriter.setPLS(plsRecord);
    sheetWriter.setDrawings(drawings, dmod);
    sheetWriter.setButtonPropertySet(buttonPropertySet);
    sheetWriter.setDataValidation(dataValidation);
    
    sheetWriter.write();
  }

  /** 
   * Copies the cell contents from the specified sheet into this one
   *
   * @param s the sheet to copy
   */
  private void copyCells(Sheet s)
  {
    // Copy the cells
    int cells = s.getRows();
    Cell[] row = null;
    Cell cell = null;
    for (int i = 0;  i < cells; i++)
    {
      row = s.getRow(i);

      for (int j = 0; j < row.length; j++)
      {
        cell = row[j];
        CellType ct = cell.getType();

        // Encase the calls to addCell in a try-catch block
        // These should not generate any errors, because we are
        // copying from an existing spreadsheet.  In the event of
        // errors, catch the exception and then bomb out with an
        // assertion
        try
        {
          if (ct == CellType.LABEL)
          {
            Label l = new Label((LabelCell) cell);
            addCell(l);
          }
          else if (ct == CellType.NUMBER)
          {
            Number n = new Number((NumberCell) cell);
            addCell(n);
          }
          else if (ct == CellType.DATE)
          {
            DateTime dt = new DateTime((DateCell) cell);
            addCell(dt);
          }
          else if (ct == CellType.BOOLEAN)
          {
            Boolean b = new Boolean((BooleanCell) cell);
            addCell(b);
          }
          else if (ct == CellType.NUMBER_FORMULA)
          {
            ReadNumberFormulaRecord fr = new ReadNumberFormulaRecord
              ((FormulaData) cell);
            addCell(fr);
          }
          else if (ct == CellType.STRING_FORMULA)
          {
            ReadStringFormulaRecord fr = new ReadStringFormulaRecord
              ((FormulaData) cell);
            addCell(fr);
          }
          else if( ct == CellType.BOOLEAN_FORMULA)
          {
            ReadBooleanFormulaRecord fr = new ReadBooleanFormulaRecord
              ((FormulaData) cell);
            addCell(fr);
          }
          else if (ct == CellType.DATE_FORMULA)
          {
            ReadDateFormulaRecord fr = new ReadDateFormulaRecord
              ((FormulaData) cell);
            addCell(fr);
          }
          else if(ct == CellType.FORMULA_ERROR)
          {
            ReadErrorFormulaRecord fr = new ReadErrorFormulaRecord
              ((FormulaData) cell);
            addCell(fr);
          }
          else if (ct == CellType.EMPTY)
          {
            if (cell.getCellFormat() != null)
            {
              // It is a blank cell, rather than an empty cell, so
              // it may have formatting information, so
              // it must be copied
              Blank b = new Blank(cell);
              addCell(b);
            }
          }
        }
        catch (WriteException e)
        {
          Assert.verify(false);
        }
      }
    }
  }

  /** 
   * Copies the cell contents from the specified sheet into this one.  This
   * differs from the previous method in that it uses deep copies to copy
   * each cell
   * Used for creating duplicates of WritableSheets
   *
   * @param s the sheet to copy
   */
  private void copyCells(WritableSheet s)
  {
    // Copy the cells
    int cells = s.getRows();
    Cell[] row = null;
    Cell cell = null;
    for (int i = 0;  i < cells; i++)
    {
      row = s.getRow(i);

      for (int j = 0; j < row.length; j++)
      {
        cell = row[j];
        CellType ct = cell.getType();

        try
        {
          WritableCell wc = ( (WritableCell) cell).copyTo(cell.getColumn(), 
                                                          cell.getRow());
          addCell(wc);
        }
        catch (WriteException e)
        {
          Assert.verify(false);
        }
      }
    }
  }

  /**
   * Copies the specified sheet, row by row and cell by cell
   * 
   * @param s the sheet to copy
   */
  void copy(Sheet s)
  {
    // Copy the settings
    settings = new SheetSettings(s.getSettings());

    copyCells(s);

    // Copy the column info records
    jxl.read.biff.SheetImpl si = (jxl.read.biff.SheetImpl) s;
    jxl.read.biff.ColumnInfoRecord[] readCirs = si.getColumnInfos();

    for (int i = 0 ; i < readCirs.length; i++)
    {
      jxl.read.biff.ColumnInfoRecord rcir = readCirs[i];
      for (int j = rcir.getStartColumn(); j <= rcir.getEndColumn() ; j++) 
      {
        ColumnInfoRecord cir = new ColumnInfoRecord(rcir, j, 
                                                    formatRecords);
        cir.setHidden(rcir.getHidden());
        columnFormats.add(cir);
      }
    }

    // Copy the hyperlinks
    Hyperlink[] hls = s.getHyperlinks();
    for (int i = 0 ; i < hls.length; i++)
    {
      WritableHyperlink hr = new WritableHyperlink
        (hls[i], this);
      hyperlinks.add(hr);
    }

    // Copy the merged cells
    Range[] merged = s.getMergedCells();

    for (int i = 0; i < merged.length; i++)
    {
      mergedCells.add(new SheetRangeImpl((SheetRangeImpl)merged[i], this));
    }

    // Copy the row properties
    try
    {
      jxl.read.biff.RowRecord[] rowprops  = si.getRowProperties();

      for (int i = 0; i < rowprops.length; i++)
      {
        RowRecord rr = getRowRecord(rowprops[i].getRowNumber());
        XFRecord format = rowprops[i].hasDefaultFormat() ? 
          formatRecords.getXFRecord(rowprops[i].getXFIndex()) : null;
        rr.setRowDetails(rowprops[i].getRowHeight(), 
                         rowprops[i].matchesDefaultFontHeight(),
                         rowprops[i].isCollapsed(),
                         format);
      }
    }
    catch (RowsExceededException e)
    {
      // Handle the rows exceeded exception - this cannot occur since
      // the sheet we are copying from will have a valid number of rows
      Assert.verify(false);
    }

    // Copy the headers and footers
    //    sheetWriter.setHeader(new HeaderRecord(si.getHeader()));
    //    sheetWriter.setFooter(new FooterRecord(si.getFooter()));

    // Copy the page breaks
    int[] rowbreaks = si.getRowPageBreaks();

    if (rowbreaks != null)
    {
      for (int i = 0; i < rowbreaks.length; i++)
      {
        rowBreaks.add(new Integer(rowbreaks[i]));
      }
    }

    // Copy the data validations
    jxl.read.biff.DataValidation rdv = si.getDataValidation();
    if (rdv != null)
    {
      dataValidation = new DataValidation(rdv, workbook, workbookSettings);
    }

    // Copy the charts
    sheetWriter.setCharts(si.getCharts());

    // Copy the drawings
    DrawingGroupObject[] dr = si.getDrawings();
    for (int i = 0 ; i < dr.length ; i++)
    {
      if (dr[i] instanceof jxl.biff.drawing.Drawing)
      {
        WritableImage wi = new WritableImage(dr[i], 
                                             workbook.getDrawingGroup());
        drawings.add(wi);
        images.add(wi);
      }
      else if (dr[i] instanceof jxl.biff.drawing.Comment)
      {
        jxl.biff.drawing.Comment c = 
          new jxl.biff.drawing.Comment(dr[i], 
                                       workbook.getDrawingGroup(),
                                       workbookSettings);
        drawings.add(c);
        
        // Set up the reference on the cell value
        CellValue cv = (CellValue) getWritableCell(c.getColumn(), c.getRow());
        Assert.verify(cv.getCellFeatures() != null);
        cv.getWritableCellFeatures().setCommentDrawing(c);
      }
      else if (dr[i] instanceof jxl.biff.drawing.Button)
      {
        jxl.biff.drawing.Button b = 
          new jxl.biff.drawing.Button(dr[i], 
                                      workbook.getDrawingGroup(),
                                      workbookSettings);
        drawings.add(b);
      }

    }

    // Copy the workspace options
    sheetWriter.setWorkspaceOptions(si.getWorkspaceOptions());


    // Set a flag to indicate if it contains a chart only
    if (si.getSheetBof().isChart())
    {
      chartOnly = true;
      sheetWriter.setChartOnly();
    }

    // Copy the environment specific print record
    if (si.getPLS() != null)
    {
      if (si.getWorkbookBof().isBiff7())
      {
        logger.warn("Cannot copy Biff7 print settings record - ignoring");
      }
      else
      {
        plsRecord = new PLSRecord(si.getPLS());
      }
    }

    // Copy the button property set
    if (si.getButtonPropertySet() != null)
    {
      buttonPropertySet = new ButtonPropertySetRecord
        (si.getButtonPropertySet());
    }
  }

  /**
   * Copies the specified sheet, row by row and cell by cell
   * 
   * @param s the sheet to copy
   */
  void copy(WritableSheet s)
  {
    settings = new SheetSettings(s.getSettings());

    copyCells(s);

    // Copy the column formats
    columnFormats = ( (WritableSheetImpl) s).columnFormats;

    // Copy the merged cells
    Range[] merged = s.getMergedCells();

    for (int i = 0; i < merged.length; i++)
    {
      mergedCells.add(new SheetRangeImpl((SheetRangeImpl)merged[i], this));
    }

    // Copy the row properties
    try
    {
      RowRecord[] copyRows = ( (WritableSheetImpl) s).rows;
      RowRecord row = null;
      for (int i = 0; i < copyRows.length ; i++)
      {
        row = copyRows[i];
        
        if (row != null &&
            (!row.isDefaultHeight() ||
             row.isCollapsed()))
        {
          RowRecord rr = getRowRecord(i);
          rr.setRowDetails(row.getRowHeight(), 
                           row.matchesDefaultFontHeight(),
                           row.isCollapsed(), 
                           row.getStyle());
        }
      }
    }
    catch (RowsExceededException e)
    {
      // Handle the rows exceeded exception - this cannot occur since
      // the sheet we are copying from will have a valid number of rows
      Assert.verify(false);
    }

    // Copy the headers and footers
    WritableSheetImpl si = (WritableSheetImpl) s;

    //    sheetWriter.setHeader(si.getHeader());
    //    sheetWriter.setFooter(si.getFooter());

    // Copy the horizontal page breaks
    rowBreaks = new ArrayList(si.rowBreaks);

    // Copy the data validations
    DataValidation rdv = si.dataValidation;
    if (rdv != null)
    {
      dataValidation = new DataValidation(rdv, 
                                          workbook, 
                                          workbookSettings);
    }

    // Copy the charts 
    sheetWriter.setCharts(si.getCharts());

    // Copy the drawings
    DrawingGroupObject[] dr = si.getDrawings();
    for (int i = 0 ; i < dr.length ; i++)
    {
      if (dr[i] instanceof jxl.biff.drawing.Drawing)
      {
        WritableImage wi = new WritableImage(dr[i], 
                                             workbook.getDrawingGroup());
        drawings.add(wi);
        images.add(wi);
      }

      // Not necessary to copy the comments, as they will be handled by
      // the deep copy of the individual cells
    }

    // Copy the workspace options
    sheetWriter.setWorkspaceOptions(si.getWorkspaceOptions());

    // Copy the environment specific print record
    if (si.plsRecord != null)
    {
      plsRecord = new PLSRecord(si.plsRecord);
    }

    // Copy the button property set
    if (si.buttonPropertySet != null)
    {
      buttonPropertySet = new ButtonPropertySetRecord(si.buttonPropertySet);
    }
  }

  /**
   * Gets the header.  Called when copying sheets
   *
   * @return the page header
   */
  final HeaderRecord getHeader()
  {
    return sheetWriter.getHeader();
  }

  /**
   * Gets the footer.  Called when copying sheets
   *
   * @return the page footer
   */
  final FooterRecord getFooter()
  {
    return sheetWriter.getFooter();
  }
  /**
   * Determines whether the sheet is protected
   *
   * @return whether or not the sheet is protected
   * @deprecated in favour of getSettings() api
   */
  public boolean isProtected()
  {
    return settings.isProtected();
  }

  /**
   * Gets the hyperlinks on this sheet
   *
   * @return an array of hyperlinks
   */
  public Hyperlink[] getHyperlinks()
  {
    Hyperlink[] hl = new Hyperlink[hyperlinks.size()];

    for (int i = 0; i < hyperlinks.size(); i++)
    {
      hl[i] = (Hyperlink) hyperlinks.get(i);
    }

    return hl;
  }

  /**
   * Gets the cells which have been merged on this sheet
   *
   * @return an array of range objects
   */
  public Range[] getMergedCells()
  {
    return mergedCells.getMergedCells();
  }

  /**
   * Gets the writable  hyperlinks on this sheet
   *
   * @return an array of hyperlinks
   */
  public WritableHyperlink[] getWritableHyperlinks()
  {
    WritableHyperlink[] hl = new WritableHyperlink[hyperlinks.size()];

    for (int i = 0; i < hyperlinks.size(); i++)
    {
      hl[i] = (WritableHyperlink) hyperlinks.get(i);
    }

    return hl;
  }

  /**
   * Removes the specified hyperlink.  Note that if you merely set the
   * cell contents to be an Empty cell, then the cells containing the 
   * hyperlink will still be active.  The contents of the cell which
   * activate the hyperlink are removed.
   * The hyperlink passed in must be a hyperlink retrieved using the 
   * getHyperlinks method
   *
   * @param h the hyperlink to remove.
   * @param preserveLabel if TRUE preserves the label contents, if FALSE
   * removes them
   */
  public void removeHyperlink(WritableHyperlink h)
  {
    removeHyperlink(h, false);
  }

  /**
   * Removes the specified hyperlink.  Note that if you merely set the
   * cell contents to be an Empty cell, then the cells containing the 
   * hyperlink will still be active.
   * If the preserveLabel field is set, the cell contents of the 
   * hyperlink are preserved, although the hyperlink is deactivated.  If
   * this value is FALSE, the cell contents are removed
   * The hyperlink passed in must be a hyperlink retrieved using the 
   * getHyperlinks method
   *
   * @param h the hyperlink to remove.
   * @param preserveLabel if TRUE preserves the label contents, if FALSE
   * removes them
   */
  public void removeHyperlink(WritableHyperlink h, boolean preserveLabel)
  {
    // Remove the hyperlink
    hyperlinks.remove(hyperlinks.indexOf(h));

    if (!preserveLabel)
    {
      // Set the cell contents for the hyperlink - including any formatting
      // information - to be empty
      Assert.verify(rows.length > h.getRow() && rows[h.getRow()] != null);
      rows[h.getRow()].removeCell(h.getColumn());
    }
  }

  /**
   * Adds the specified hyperlink
   * 
   * @param the hyperlink
   * @exception WriteException
   * @exception RowsExceededException
   */
  public void addHyperlink(WritableHyperlink h) 
    throws WriteException, RowsExceededException
  {
    // First set the label on the sheet
    Cell c = getCell(h.getColumn(), h.getRow());

    String contents = null;
    if (h.isFile() || h.isUNC())
    {
      String cnts = ( (HyperlinkRecord) h).getContents();
      if (cnts == null)
      {
        contents = h.getFile().getPath();
      }
      else
      {
        contents = cnts;
      }
    }
    else if (h.isURL())
    {
      String cnts = ( (HyperlinkRecord) h).getContents();
      if (cnts == null)
      {
        contents = h.getURL().toString();
      }
      else
      {
        contents=cnts;
      }
    }
    else if (h.isLocation())
    {
      contents = ( (HyperlinkRecord) h).getContents();
    }

    if (c.getType() == CellType.LABEL)
    {
      Label l = (Label) c;
      l.setString(contents);
      l.setCellFormat(WritableWorkbook.HYPERLINK_STYLE);
    }
    else
    {
      Label l = new Label(h.getColumn(), h.getRow(), contents, 
                          WritableWorkbook.HYPERLINK_STYLE);
      addCell(l);
    }
    
    // Set all other cells within range to be empty
    for (int i = h.getRow(); i <= h.getLastRow(); i++)
    {
      for (int j = h.getColumn(); j <= h.getLastColumn(); j++)
      {
        if (i != h.getRow() && j != h.getColumn())
        {
          // Set the cell to be empty
          if (rows[i] != null)
          {
            rows[i].removeCell(j);
          }
        }
      }
    }

    ((HyperlinkRecord) h).initialize(this);
    hyperlinks.add(h);
  }

  /**
   * Merges the specified cells.  Any clashes or intersections between 
   * merged cells are resolved when the spreadsheet is written out
   *
   * @param col1 the column number of the top left cell
   * @param row1 the row number of the top left cell
   * @param col2 the column number of the bottom right cell
   * @param row2 the row number of the bottom right cell
   * @return the Range object representing the merged cells
   * @exception jxl.write..WriteException
   * @exception jxl.write.biff.RowsExceededException
   */
  public Range mergeCells(int col1, int row1, int col2, int row2)
    throws WriteException, RowsExceededException
  {
    // First check that the cells make sense
    if (col2 < col1 || row2 < row1)
    {
      logger.warn("Cannot merge cells - top left and bottom right "+
                  "incorrectly specified");
    }

    // Make sure the spreadsheet is up to size
    if (col2 >= numColumns || row2 >= numRows)
    {
      addCell(new Blank(col2, row2));
    }

    SheetRangeImpl range = new SheetRangeImpl(this, col1, row1, col2, row2);
    mergedCells.add(range);

    return range;
  }

  /**
   * Unmerges the specified cells.  The Range passed in should be one that
   * has been previously returned as a result of the getMergedCells method
   *
   * @param r the range of cells to unmerge
   */
  public void unmergeCells(Range r)
  {
    mergedCells.unmergeCells(r);
  }

  /**
   * Sets the header for this page
   *
   * @param l the print header to print on the left side
   * @param c the print header to print in the centre
   * @param r the print header to print on the right hand side
   * @deprecated use the sheet settings beant
   */
  public void setHeader(String l, String c, String r)
  {
    HeaderFooter header = new HeaderFooter();
    header.getLeft().append(l);
    header.getCentre().append(c);
    header.getRight().append(r);
    settings.setHeader(header);
  }

  /**
   * Sets the footer for this page
   *
   * @param l the print header to print on the left side
   * @param c the print header to print in the centre
   * @param r the print header to print on the right hand side
   * @deprecated use the sheet settings bean
   */
  public void setFooter(String l, String c, String r)
  {
    HeaderFooter footer = new HeaderFooter();
    footer.getLeft().append(l);
    footer.getCentre().append(c);
    footer.getRight().append(r);
    settings.setFooter(footer);
  }

  /**
   * Sets the page setup details
   *
   * @param p  the page orientation
   * @deprecated use the SheetSettings bean
   */
  public void setPageSetup(PageOrientation p)
  {
    settings.setOrientation(p);
  }

  /**
   * Sets the page setup details
   *
   * @param p  the page orientation
   * @param hm the header margin, in inches
   * @param fm the footer margin, in inches
   * @deprecated use the SheetSettings bean
   */
  public void setPageSetup(PageOrientation p, double hm, double fm)
  {
    settings.setOrientation(p);
    settings.setHeaderMargin(hm);
    settings.setFooterMargin(fm);
  }

  /**
   * Sets the page setup details
   *
   * @param p  the page orientation
   * @param ps the paper size
   * @param hm the header margin, in inches
   * @param fm the footer margin, in inches
   * @deprecated use the SheetSettings bean
   */
  public void setPageSetup(PageOrientation p, PaperSize ps, 
                           double hm, double fm)
  {
    settings.setPaperSize(ps);
    settings.setOrientation(p);
    settings.setHeaderMargin(hm);
    settings.setFooterMargin(fm);
  }

  /** 
   * Gets the settings for this sheet
   *
   * @return the page settings bean
   */
  public SheetSettings getSettings()
  {
    return settings;
  }

  /**
   * Gets the workbook settings
   */
  WorkbookSettings getWorkbookSettings()
  {
    return workbookSettings;
  }

  /**
   * Forces a page break at the specified row
   * 
   * @param row the row to break at
   */
  public void addRowPageBreak(int row)
  {
    // First check that the row is not already present
    Iterator i = rowBreaks.iterator();
    boolean found = false;

    while (i.hasNext() && !found)
    {
      if (( (Integer) i.next()).intValue() == row)
      {
        found = true;
      }
    }

    if (!found)
    {
      rowBreaks.add(new Integer(row));
    }
  }

  /**
   * Accessor for the charts.  Used when copying
   *
   * @return the charts on this sheet
   */
  private Chart[] getCharts()
  {
    return sheetWriter.getCharts();
  }

  /**
   * Accessor for the drawings.  Used when copying
   *
   * @return the drawings on this sheet
   */
  private DrawingGroupObject[] getDrawings()
  {
    DrawingGroupObject[] dr = new DrawingGroupObject[drawings.size()];
    dr = (DrawingGroupObject[]) drawings.toArray(dr);
    return dr;
  }

  /**
   * Check all the merged cells for borders.  Although in an OO sense the
   * logic should belong in this class, in order to reduce the bloated 
   * nature of the source code for this object this logic has been delegated
   * to the SheetWriter
   */
  void checkMergedBorders()
  {
    sheetWriter.setWriteData(rows, 
                             rowBreaks, 
                             hyperlinks, 
                             mergedCells, 
                             columnFormats );
    sheetWriter.setDimensions(getRows(), getColumns());
    sheetWriter.checkMergedBorders();
  }

  /**
   * Accessor for the workspace options
   *
   * @return the workspace options
   */
  private WorkspaceInformationRecord getWorkspaceOptions()
  {
    return sheetWriter.getWorkspaceOptions();
  }

  /**
   * Rationalizes the sheets xf index mapping
   * @param xfMapping the index mapping for XFRecords
   * @param fontMapping the index mapping for fonts
   * @param formatMapping the index mapping for formats
   */
  void rationalize(IndexMapping xfMapping, 
                   IndexMapping fontMapping, 
                   IndexMapping formatMapping)
  {
    // Rationalize the column formats
    for (Iterator i = columnFormats.iterator() ; i.hasNext() ;)
    {
      ColumnInfoRecord cir = (ColumnInfoRecord) i.next();
      cir.rationalize(xfMapping);
    }

    // Rationalize the row formats
    for (int i = 0; i < rows.length ; i++)
    {
      if (rows[i] != null)
      {
        rows[i].rationalize(xfMapping);
      }
    }

    // Rationalize any data that appears on the charts
    Chart[] charts = getCharts();
    for (int c = 0; c < charts.length; c++)
    {
      charts[c].rationalize(xfMapping, fontMapping, formatMapping);
    }    
  }

  /**
   * Accessor for the workbook
   * @return the workbook
   */
  WritableWorkbookImpl getWorkbook()
  {
    return workbook;
  }

  /**
   * Gets the column format for the specified column
   *
   * @param col the column number
   * @return the column format, or NULL if the column has no specific format
   * @deprecated use getColumnView instead
   */
  public CellFormat getColumnFormat(int col)
  {
    return getColumnView(col).getFormat();
  }

  /**
   * Gets the column width for the specified column
   *
   * @param col the column number
   * @return the column width, or the default width if the column has no
   *         specified format
   * @deprecated use getColumnView instead
   */
  public int getColumnWidth(int col)
  {
    return getColumnView(col).getDimension();
  }

  /**
   * Gets the column width for the specified column
   *
   * @param row the column number
   * @return the row height, or the default height if the column has no
   *         specified format
   * @deprecated use getRowView instead
   */
  public int getRowHeight(int row)
  {
    return getRowView(row).getDimension();
  }

  /**
   * Accessor for the chart only method
   * 
   * @return TRUE if this is a chart only, FALSE otherwise
   */
  boolean isChartOnly()
  {
    return chartOnly;
  }

  /**
   * Gets the row view for the specified row
   *
   * @param col the row number
   * @return the row format, or the default format if no override is
             specified
   */
  public CellView getRowView(int row)
  {
    CellView cv = new CellView();

    try
    {
      RowRecord rr = getRowRecord(row);

      if (rr == null || rr.isDefaultHeight())
      {
        cv.setDimension(settings.getDefaultRowHeight());
        cv.setSize(settings.getDefaultRowHeight());
      }
      else if (rr.isCollapsed())
      {
        cv.setHidden(true);
      }
      else
      {
        cv.setDimension(rr.getRowHeight());
        cv.setSize(rr.getRowHeight());
      }
      return cv;
    }
    catch (RowsExceededException e)
    {
      // Simple return the default
      cv.setDimension(settings.getDefaultRowHeight());
      cv.setSize(settings.getDefaultRowHeight());
      return cv;
    }
  }

  /**
   * Gets the column width for the specified column
   *
   * @param col the column number
   * @return the column format, or the default format if no override is
             specified
   */
  public CellView getColumnView(int col)
  {
    ColumnInfoRecord cir = getColumnInfo(col);
    CellView cv = new CellView();

    if (cir != null)
    {
      cv.setDimension(cir.getWidth()/256);
      cv.setSize(cir.getWidth());
      cv.setHidden(cir.getHidden());
      cv.setFormat(cir.getCellFormat());
    }
    else
    {
      cv.setDimension(settings.getDefaultColumnWidth()/256);
      cv.setSize(settings.getDefaultColumnWidth());
    }

    return cv;
  }

  /**
   * Adds an image to this sheet
   *
   * @param image the image to add
   */
  public void addImage(WritableImage image)
  {
    boolean supported = false;
    java.io.File imageFile = image.getImageFile();
    String fileType = "?";

    if (imageFile != null)
    {
    
      String fileName = imageFile.getName();
      int fileTypeIndex = fileName.lastIndexOf('.');
      fileType = fileTypeIndex != -1 ? 
        fileName.substring(fileTypeIndex+1) : "";
      
      for (int i = 0 ; i < imageTypes.length && !supported ; i++)
      {
        if (fileType.equalsIgnoreCase(imageTypes[i]))
        {
          supported = true;
        }
      }
    }
    else
    {
      supported = true;
    }

    if (supported)
    {
      workbook.addDrawing(image);
      drawings.add(image);
      images.add(image);
    }
    else
    {
      StringBuffer message = new StringBuffer("Image type ");
      message.append(fileType);
      message.append(" not supported.  Supported types are ");
      message.append(imageTypes[0]);
      for (int i = 1 ; i < imageTypes.length ; i++)
      {
        message.append(", ");
        message.append(imageTypes[i]);
      }
      logger.warn(message.toString());
    }
  }

  /**
   * Gets the number of images on this sheet
   *
   * @return the number of images on this sheet
   */
  public int getNumberOfImages()
  {
    return images.size();
  }

  /**
   * Accessor for a particular image on this sheet
   *
   * @param i the 0-based image index number
   * @return the image with the specified index number
   */
  public WritableImage getImage(int i)
  {
    return (WritableImage) images.get(i);
  }

  /**
   * Accessor for a particular image on this sheet
   *
   * @param i the 0-based image index number
   * @return the image with the specified index number
   */
  public Image getDrawing(int i)
  {
    return (Image) images.get(i);
  }

  /**
   * Removes the specified image from this sheet.  The image passed in
   * must be the same instance as that retrieved from a getImage call
   *
   * @param wi the image to remove
   */
  public void removeImage(WritableImage wi)
  {
    boolean removed = drawings.remove(wi);
    images.remove(wi);
    drawingsModified = true;
    workbook.removeDrawing(wi);
  }

  /**
   * Validates the sheet name
   */
  private String validateName(String n)
  {
    if (n.length() > maxSheetNameLength)
    {
      logger.warn("Sheet name " + n + " too long - truncating");
      n = n.substring(0, maxSheetNameLength);
    }

    if (n.charAt(0) == '\'')
    {
      logger.warn("Sheet naming cannot start with \' - removing");
      n = n.substring(1);
    }

    for (int i = 0 ; i < illegalSheetNameCharacters.length ; i++)
    {
      String newname = n.replace(illegalSheetNameCharacters[i], '@');
      if (n != newname)
      {
        logger.warn(illegalSheetNameCharacters[i] + 
        " is not a valid character within a sheet name - replacing");
      }
      n = newname;
    }

    return n;
  }

  /**
   * Adds a drawing to the list - typically used for comments
   *
   * @param the drawing to add
   */
  void addDrawing(DrawingGroupObject o)
  {
    drawings.add(o);
    Assert.verify(!(o instanceof Drawing));
  }

  /**
   * Removes a drawing to the list - typically used for comments
   *
   * @param the drawing to add
   */
  void removeDrawing(DrawingGroupObject o)
  {
    int origSize = drawings.size();
    drawings.remove(o);
    int newSize = drawings.size();
    drawingsModified = true;
    Assert.verify(newSize == origSize -1);
  }
}
