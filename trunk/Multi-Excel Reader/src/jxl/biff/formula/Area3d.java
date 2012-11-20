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

package jxl.biff.formula;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import common.Assert;
import common.Logger;

import jxl.biff.RecordData;
import jxl.biff.StringHelper;
import jxl.biff.IntegerHelper;
import jxl.biff.CellReferenceHelper;

/** 
 * A nested class to hold range information
 */
class Area3d extends Operand implements ParsedThing
{
  /**
   * The logger
   */
  private static Logger logger = Logger.getLogger(Area3d.class);

  private int sheet;
  private int columnFirst;
  private int rowFirst;
  private int columnLast;
  private int rowLast;
  private boolean columnFirstRelative;
  private boolean rowFirstRelative;
  private boolean columnLastRelative;
  private boolean rowLastRelative;
  
  /** 
   * A handle to the workbook
   */
  private ExternalSheet workbook;

  Area3d(ExternalSheet es)
  {
    workbook = es;
  }

  /**
   * Constructor invoked when parsing a string formula
   *
   * @param s the string to parse
   */
  Area3d(String s, ExternalSheet es) throws FormulaException
  {
    workbook = es;
    int seppos = s.lastIndexOf(":");
    Assert.verify(seppos != -1);
    String startcell = s.substring(0, seppos);
    String endcell = s.substring(seppos+1);

    // Get the the start cell details
    int sep = s.indexOf('!');
    String cellString = s.substring(sep+1, seppos);
    columnFirst = CellReferenceHelper.getColumn(cellString);
    rowFirst = CellReferenceHelper.getRow(cellString);

    // Get the sheet index
    String sheetName = s.substring(0, sep);
    int sheetNamePos = sheetName.lastIndexOf(']');

    // Remove single quotes, if they exist
    if (sheetName.charAt(0) == '\'' && 
        sheetName.charAt(sheetName.length()-1) == '\'')
    {
      sheetName = sheetName.substring(1, sheetName.length() - 1);
    }

    sheet = es.getExternalSheetIndex(sheetName);

    if (sheet < 0)
    {
      throw new FormulaException(FormulaException.sheetRefNotFound, sheetName);
    }

    // Get the last cell index
    columnLast = CellReferenceHelper.getColumn(endcell);
    rowLast = CellReferenceHelper.getRow(endcell);

    columnFirstRelative = true;
    rowFirstRelative = true;
    columnLastRelative = true;
    rowLastRelative = true;
  }

  int getFirstColumn() {return columnFirst;}
  int getFirstRow() {return rowFirst;}
  int getLastColumn() {return columnLast;}
  int getLastRow() {return rowLast;}

  /** 
   * Reads the ptg data from the array starting at the specified position
   *
   * @param data the RPN array
   * @param pos the current position in the array, excluding the ptg identifier
   * @return the number of bytes read
   */
  public int read(byte[] data, int pos)
  {
    sheet = IntegerHelper.getInt(data[pos], data[pos+1]);
    rowFirst =  IntegerHelper.getInt(data[pos+2], data[pos+3]);
    rowLast  = IntegerHelper.getInt(data[pos+4], data[pos+5]);
    int columnMask = IntegerHelper.getInt(data[pos+6], data[pos+7]);
    columnFirst = columnMask & 0x00ff;
    columnFirstRelative = ((columnMask & 0x4000) != 0);
    rowFirstRelative = ((columnMask & 0x8000) != 0);
    columnMask = IntegerHelper.getInt(data[pos+8], data[pos+9]);
    columnLast = columnMask & 0x00ff;
    columnLastRelative = ((columnMask & 0x4000) != 0);
    rowLastRelative = ((columnMask & 0x8000) != 0);

    return 10;
  }

  public void getString(StringBuffer buf)
  {
    CellReferenceHelper.getCellReference
      (sheet, columnFirst, rowFirst, workbook, buf);
    buf.append(':');
    CellReferenceHelper.getCellReference(columnLast, rowLast, buf);
  }

  /**
   * Gets the token representation of this item in RPN
   *
   * @return the bytes applicable to this formula
   */
  byte[] getBytes()
  {
    byte[] data = new byte[11];
    data[0] = Token.AREA3D.getCode();

    IntegerHelper.getTwoBytes(sheet, data, 1);

    IntegerHelper.getTwoBytes(rowFirst, data, 3);
    IntegerHelper.getTwoBytes(rowLast, data, 5);

    int grcol = columnFirst;

    // Set the row/column relative bits if applicable
    if (rowFirstRelative)
    {
      grcol |= 0x8000;
    }

    if (columnFirstRelative)
    {
      grcol |= 0x4000;
    }

    IntegerHelper.getTwoBytes(grcol, data, 7);

    grcol = columnLast;

    // Set the row/column relative bits if applicable
    if (rowLastRelative)
    {
      grcol |= 0x8000;
    }

    if (columnLastRelative)
    {
      grcol |= 0x4000;
    }

    IntegerHelper.getTwoBytes(grcol, data, 9);

    return data;
  }

  /**
   * Adjusts all the relative cell references in this formula by the
   * amount specified.  Used when copying formulas
   *
   * @param colAdjust the amount to add on to each relative cell reference
   * @param rowAdjust the amount to add on to each relative row reference
   */
  public void adjustRelativeCellReferences(int colAdjust, int rowAdjust)
  {
    if (columnFirstRelative)
    {
      columnFirst += colAdjust;
    }

    if (columnLastRelative)
    {
      columnLast += colAdjust;
    }
    
    if (rowFirstRelative)
    {
      rowFirst += rowAdjust;
    }

    if (rowLastRelative)
    {
      rowLast += rowAdjust;
    }
  }

  /**
   * Called when a column is inserted on the specified sheet.  Tells
   * the formula  parser to update all of its cell references beyond this
   * column
   *
   * @param sheetIndex the sheet on which the column was inserted
   * @param col the column number which was inserted
   * @param currentSheet TRUE if this formula is on the sheet in which the
   * column was inserted, FALSE otherwise
   */
  public void columnInserted(int sheetIndex, int col, boolean currentSheet)
  {
    if (sheetIndex != sheet)
    {
      return;
    }

    if (columnFirst >= col)
    {
      columnFirst++;
    }

    if (columnLast >= col)
    {
      columnLast++;
    }
  }

  /**
   * Called when a column is inserted on the specified sheet.  Tells
   * the formula  parser to update all of its cell references beyond this
   * column
   *
   * @param sheetIndex the sheet on which the column was removed
   * @param col the column number which was removed
   * @param currentSheet TRUE if this formula is on the sheet in which the
   * column was inserted, FALSE otherwise
   */
  void columnRemoved(int sheetIndex, int col, boolean currentSheet)
  {
    if (sheetIndex != sheet)
    {
      return;
    }

    if (col < columnFirst)
    {
      columnFirst--;
    }

    if (col <= columnLast)
    {
      columnLast--;
    }
  }

  /**
   * Called when a column is inserted on the specified sheet.  Tells
   * the formula  parser to update all of its cell references beyond this
   * column
   *
   * @param sheetIndex the sheet on which the row was inserted
   * @param row the row number which was inserted
   * @param currentSheet TRUE if this formula is on the sheet in which the
   * column was inserted, FALSE otherwise
   */
  void rowInserted(int sheetIndex, int row, boolean currentSheet)
  {
    if (sheetIndex != sheet)
    {
      return;
    }

    if (rowLast == 0xffff)
    {
      // area applies to the whole column, so nothing to do
      return;
    }

    if (row <= rowFirst)
    {
      rowFirst++;
    }

    if (row <= rowLast)
    {
      rowLast++;
    }
  }

  /**
   * Called when a column is inserted on the specified sheet.  Tells
   * the formula  parser to update all of its cell references beyond this
   * column
   *
   * @param sheetIndex the sheet on which the row was removed
   * @param row the row number which was removed
   * @param currentSheet TRUE if this formula is on the sheet in which the
   * column was inserted, FALSE otherwise
   */
  void rowRemoved(int sheetIndex, int row, boolean currentSheet)
  {
    if (sheetIndex != sheet)
    {
      return;
    }

    if (rowLast == 0xffff)
    {
      // area applies to the whole column, so nothing to do
      return;
    }

    if (row < rowFirst)
    {
      rowFirst--;
    }

    if (row <= rowLast)
    {
      rowLast--;
    }
  }

  /**
   * Used by subclasses columns/row range to set the range information
   */
  protected void setRangeData(int sht,
                              int colFirst, int colLast, 
                              int rwFirst, int rwLast,
                              boolean colFirstRel, boolean colLastRel,
                              boolean rowFirstRel, boolean rowLastRel)
  {
    sheet = sht;
    columnFirst = colFirst;
    columnLast = colLast;
    rowFirst = rwFirst;
    rowLast = rwLast;
    columnFirstRelative = colFirstRel;
    columnLastRelative = colLastRel;
    rowFirstRelative = rowFirstRel;
    rowLastRelative = rowLastRel;
  }
}








