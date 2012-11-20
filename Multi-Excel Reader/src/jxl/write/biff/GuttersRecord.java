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
import jxl.biff.WritableRecordData;

/**
 * Record which contains the size of the row and column gutters.  These are
 * all set to zero by default
 */
class GuttersRecord extends WritableRecordData
{
  /**
   * The binary data
   */
  private byte[] data;
  /**
   * The rowGutter
   */
  private int rowGutter;
  /**
   * The column gutter
   */
  private int colGutter;
  /**
   * The maximum outline level for the row gutter
   */
  private int maxRowOutline;
  /**
   * The maximum row outline level for the column gutter
   */
  private int maxColOutline;
  
  /**
   * Constructor
   */
  public GuttersRecord()
  {
    super(Type.GUTS);
  }

  /**
   * Gets the binary data for output
   * 
   * @return the binary data
   */
  public byte[] getData()
  {
    data = new byte[8];
    IntegerHelper.getTwoBytes(rowGutter, data, 0);
    IntegerHelper.getTwoBytes(colGutter, data, 2);
    IntegerHelper.getTwoBytes(maxRowOutline, data, 4);
    IntegerHelper.getTwoBytes(maxColOutline, data, 6);
    return data;
  }
}


