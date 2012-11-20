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

import jxl.Cell;
import jxl.biff.IntegerHelper;
import jxl.biff.CellReferenceHelper;

/**
 * A cell reference in a formula
 */
class SharedFormulaCellReference extends Operand implements ParsedThing
{
  /**
   * Indicates whether the column reference is relative or absolute
   */
  private boolean columnRelative;

  /**
   * Indicates whether the row reference is relative or absolute
   */
  private boolean rowRelative;

  /**
   * The column reference
   */
  private int column;

  /**
   * The row reference
   */
  private int row;

  /**
   * The cell containing the formula.  Stored in order to determine
   * relative cell values
   */
  private Cell relativeTo;

  /**
   * Constructor
   *
   * @param the cell the formula is relative to
   */
  public SharedFormulaCellReference(Cell rt)
  {
    relativeTo = rt;
  }

  /** 
   * Reads the ptg data from the array starting at the specified position
   *
   * @param data the RPN array
   * @param pos the current position in the array, excluding the ptg identifier
   * @return the number of bytes read
   */
  public int read(byte[] data, int pos)
  {
    // Preserve signage on column and row values, because they will
    // probably be relative
    row = IntegerHelper.getShort(data[pos], data[pos+1]);

    int columnMask = IntegerHelper.getInt(data[pos+2], data[pos+3]);

    column = (byte) (columnMask & 0xff);
    columnRelative = ((columnMask & 0x4000) != 0);
    rowRelative = ((columnMask & 0x8000) != 0);

    if (columnRelative)
    {
      column = relativeTo.getColumn() + column;
    }

    if (rowRelative)
    {
      row = relativeTo.getRow() + row;
    }

    return 4;
  }

  public int getColumn()
  {
    return column;
  }

  public int getRow()
  {
    return row;
  }

  public void getString(StringBuffer buf)
  {
    CellReferenceHelper.getCellReference(column, row, buf);
  }

  /**
   * Gets the token representation of this item in RPN
   *
   * @return the bytes applicable to this formula
   */
  byte[] getBytes()
  {
    byte[] data = new byte[5];
    data[0] = Token.REF.getCode();
    
    IntegerHelper.getTwoBytes(row, data, 1);

    int columnMask = column;

    if (columnRelative)
    {
      columnMask |= 0x4000;
    }

    if (rowRelative)
    {
      columnMask |= 0x8000;
    }

    IntegerHelper.getTwoBytes(columnMask, data, 3);

    return data;
  }
}









