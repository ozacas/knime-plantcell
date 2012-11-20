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

import jxl.biff.Type;
import jxl.biff.IntegerHelper;
import jxl.biff.StringHelper;
import jxl.biff.DoubleHelper;
import jxl.biff.WritableRecordData;

/**
 * A name record.  Simply takes the binary data from the name
 * record read in
 */
class NameRecord extends WritableRecordData
{
  /**
   * The binary data for output to file
   */
  private byte[] data;

  /**
   * The name
   */
  private String name;

  /**
   * The index into the name table
   */
  private int index;
  
  /**
   * The 0-based index sheet reference for a record name
   *     0 is for a global reference
   */
  private int sheetRef = 0;

  /** 
   * A nested class to hold range information
   */
  class NameRange
  {
    private int columnFirst;
    private int rowFirst;
    private int columnLast;
    private int rowLast;
    private int externalSheet;

    NameRange(jxl.read.biff.NameRecord.NameRange nr)
    {
      columnFirst = nr.getFirstColumn();
      rowFirst = nr.getFirstRow();
      columnLast = nr.getLastColumn();
      rowLast = nr.getLastRow();
      externalSheet = nr.getExternalSheet();
    }
    
    /**
     * Create a new range for the name record.
     */
    NameRange(int theSheet, 
              int theStartRow, 
              int theEndRow,
              int theStartCol, 
              int theEndCol)
    {
      columnFirst = theStartCol;
      rowFirst = theStartRow;
      columnLast = theEndCol;
      rowLast = theEndRow;
      externalSheet = theSheet;
    }

    int getFirstColumn() {return columnFirst;}
    int getFirstRow() {return rowFirst;}
    int getLastColumn() {return columnLast;}
    int getLastRow() {return rowLast;}
    int getExternalSheet() { return externalSheet;}

    byte[] getData()
    {
      byte[] d = new byte[10];

      // Sheet index
      IntegerHelper.getTwoBytes(sheetRef, d, 0);

      // Starting row
      IntegerHelper.getTwoBytes(rowFirst, d, 2);
      
      // End row
      IntegerHelper.getTwoBytes(rowLast, d, 4);
      
      // Start column
      IntegerHelper.getTwoBytes(columnFirst & 0xff, d, 6);

      // End columns
      IntegerHelper.getTwoBytes(columnLast & 0xff, d, 8);

      return d;
    }

  }

  /**
   * The ranges covered by this name
   */
  private NameRange[] ranges;

  // Constants which refer to the parse tokens after the string
  private static final int cellReference = 0x3a;
  private static final int areaReference = 0x3b;
  private static final int subExpression = 0x29;
  private static final int union         = 0x10;

  /**
   * Constructor - used when copying sheets
   *
   * @param index the index into the name table
   */
  public NameRecord(jxl.read.biff.NameRecord sr, int ind)
  {
    super(Type.NAME);

    data = sr.getData();
    name = sr.getName();
    sheetRef = sr.getSheetRef();
    index = ind;

    // Copy the ranges
    jxl.read.biff.NameRecord.NameRange[] r = sr.getRanges();
    ranges = new NameRange[r.length];
    for (int i = 0 ; i < ranges.length ; i++)
    {
      ranges[i] = new NameRange(r[i]);
    }
  }

  /**
   * Create a new name record with the given information.
   * 
   * @param theName      Name to be created.
   * @param theIndex     Index of this name.
   * @param theSheet     Sheet this name refers to.
   * @param theStartRow  First row this name refers to.
   * @param theEndRow    Last row this name refers to.
   * @param theStartCol  First column this name refers to.
   * @param theEndCol    Last column this name refers to.
   */
  NameRecord(String theName, 
             int theIndex, 
             int theSheet, 
             int theStartRow, 
             int theEndRow, 
             int theStartCol, 
             int theEndCol)
  { 
    super(Type.NAME);

    name = theName;
    index = theIndex;  // Name index, not stored in data
    sheetRef = 0;      // Global name.

    ranges = new NameRange[1];
    ranges[0] = new NameRange(theSheet, 
                              theStartRow, 
                              theEndRow, 
                              theStartCol, 
                              theEndCol);
  }

  /**
   * Gets the binary data for output to file
   *
   * @return the binary data
   */
  public byte[] getData()
  {
    if (data != null)
    {
      // this is a copy
      return data;
    }

    final int NAME_HEADER_LENGTH = 15;
    final byte AREA_RANGE_LENGTH = 11;
    final byte AREA_REFERENCE = 0x3b;
    
    data = new byte[NAME_HEADER_LENGTH + 
                    name.length() + 
                    AREA_RANGE_LENGTH];

    // Options
    int options = 0;
    IntegerHelper.getTwoBytes(options, data, 0);

    // Keyboard shortcut
    data[2] = 0;

    // Length of the name in chars
    data[3] = (byte) name.length();    

    // Size of the definitions
    IntegerHelper.getTwoBytes(AREA_RANGE_LENGTH, data, 4);

    // Sheet index
    IntegerHelper.getTwoBytes(ranges[0].externalSheet, data, 6);
    IntegerHelper.getTwoBytes(ranges[0].externalSheet, data, 8);

    // Byte 10-13 are optional lengths [0,0,0,0]    
    // Byte 14 is length of name which is not used.    

    // The name
    StringHelper.getBytes(name, data, 15);

    
    // The actual range definition.
    int pos = name.length() + 15;

    // Range format - area
    data[pos] = areaReference;

    // The range data
    byte[] rd = ranges[0].getData();
    System.arraycopy(rd, 0, data, pos+1, rd.length);

    return data;
  }

  /**
   * Accessor for the name 
   *
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * Accessor for the index of this name in the name table
   *
   * @return the index of this name in the name table
   */
  public int getIndex()
  {
    return index;
  }
  
  /**
   * The 0-based index sheet reference for a record name
   *     0 is for a global reference
   *
   * @return the sheet reference for name formula
   */
  public int getSheetRef()
  {
    return sheetRef;
  }
  
  /**
   * Set the index sheet reference for a record name
   *     0 is for a global reference
   *
   */
  public void setSheetRef(int i)
  {
    sheetRef = i;
    IntegerHelper.getTwoBytes(sheetRef, data, 8);
  }

  /**
   * Gets the array of ranges for this name
   * @return the ranges
   */
  public NameRange[] getRanges()
  {
    return ranges;
  }
}

