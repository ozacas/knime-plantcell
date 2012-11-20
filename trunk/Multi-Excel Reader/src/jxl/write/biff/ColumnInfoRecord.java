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


import jxl.biff.WritableRecordData;
import jxl.biff.XFRecord;
import jxl.biff.IntegerHelper;
import jxl.biff.Type;
import jxl.biff.FormattingRecords;
import jxl.biff.IndexMapping;

/**
 * Describes the column formatting for a particular column
 */
class ColumnInfoRecord extends WritableRecordData
{
  /**
   * The binary data
   */
  private byte[] data;
  /**
   * The column number which this format applies to
   */
  private int column;
  /**
   * The style for the column
   */
  private XFRecord style;
  /**
   * The index for the style of this column
   */
  private int xfIndex;

  /**
   * The width of the column in 1/256 of a character
   */
  private int width;

  /**
   * Flag to indicate the hidden status of this column
   */
  private boolean hidden;

  /**
   * Constructor used when setting column information from the user
   * API
   * 
   * @param w the width of the column in characters
   * @param col the column to format
   * @param xf the style for the column
   */
  public ColumnInfoRecord(int col, int w, XFRecord xf)
  {
    super(Type.COLINFO);

    column  = col;
    width   = w;
    style   = xf;
    xfIndex = style.getXFIndex();
    hidden = false;
  }

  /**
   * Constructor used when copying an existing spreadsheet
   * 
   * @param col the column number
   * @param cir the column info record read in
   * @param fr  the format records
   */
  public ColumnInfoRecord(jxl.read.biff.ColumnInfoRecord cir, 
                          int col,
                          FormattingRecords fr)
  {
    super(Type.COLINFO);

    column  = col;
    width   = cir.getWidth();
    xfIndex = cir.getXFIndex();
    style   = fr.getXFRecord(xfIndex);
  }

  /**
   * Gets the column this format applies to
   * 
   * @return the column which is formatted
   */
  public int getColumn()
  {
    return column;
  }

  /**
   * Increments the column.  Called when inserting a new column into
   * the sheet
   */
  public void incrementColumn()
  {
    column++;
  }

  /**
   * Decrements the column.  Called when removing  a  column from
   * the sheet
   */
  public void decrementColumn()
  {
    column--;
  }

  /**
   * Accessor for the width
   * 
   * @return the width
   */
  int getWidth()
  {
    return width;
  }

  /**
   * Gets the binary data to be written to the output file
   * 
   * @return the data to write to file
   */
  public byte[] getData()
  {
    data = new byte[0x0c];

    IntegerHelper.getTwoBytes(column, data, 0);
    IntegerHelper.getTwoBytes(column, data, 2);
    IntegerHelper.getTwoBytes(width,  data, 4);
    IntegerHelper.getTwoBytes(xfIndex, data, 6);

    //    int options = 0x2;
    int options = 0x6;
    if (hidden)
    {
      options |= 0x1;
    }
    IntegerHelper.getTwoBytes(options, data, 8);
    //    IntegerHelper.getTwoBytes(2, data, 10);

    return data;
  }

  /**
   * Gets the cell format associated with this column info record
   *
   * @retun the cell format for this column
   */
  public XFRecord getCellFormat()
  {
    return style;
  }

  /**
   * Rationalizes the sheets xf index mapping
   * @param xfmapping the index mapping
   */
  void rationalize(IndexMapping xfmapping)
  {
    xfIndex = xfmapping.getNewIndex(xfIndex);
  }

  /**
   * Sets this column to be hidden (or otherwise)
   *
   * @param h TRUE if the column is to be hidden, FALSE otherwise
   */
  void setHidden(boolean h)
  {
    hidden = h;
  }

  /**
   * Accessor for the hidden flag
   * 
   * @return TRUE if this column is hidden, FALSE otherwise
   */
  boolean getHidden()
  {
    return hidden;
  }

  /**
   * Standard equals method
   *
   * @return TRUE if these objects are equal, FALSE otherwise
   */
  public boolean equals(Object o)
  {
    if (o == this)
    {
      return true;
    }

    if (!(o instanceof ColumnInfoRecord))
    {
      return false;
    }

    ColumnInfoRecord cir = (ColumnInfoRecord) o;

    int col2 = cir.column;

    if (column  != cir.column ||
        xfIndex != cir.xfIndex ||
        width   != cir.width ||
        hidden  != cir.hidden)
    {
      return false;
    }

    if ((style == null && cir.style != null) || 
        (style != null && cir.style == null))
    {
      return false;
    }
    
    return style.equals(cir.style);
  }

  /**
   * Standard hashCode method
   *
   * @return the hashCode
   */
  public int hashCode()
  {
    int hashValue = 137;
    int oddPrimeNumber = 79;
    
    hashValue = hashValue * oddPrimeNumber + column;
    hashValue = hashValue * oddPrimeNumber + xfIndex;
    hashValue = hashValue * oddPrimeNumber + width;
    hashValue = hashValue * oddPrimeNumber + (hidden ? 1:0);

    if (style != null)
    {
      hashValue ^= style.hashCode();
    }

    return hashValue;
  }
}
