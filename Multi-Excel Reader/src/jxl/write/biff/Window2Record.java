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

import jxl.SheetSettings;

import jxl.biff.Type;
import jxl.biff.IntegerHelper;
import jxl.biff.WritableRecordData;

/**
 * Contains the window attributes for a worksheet
 */
class Window2Record extends WritableRecordData
{
  /**
   * The binary data for output to file
   */
  private byte[] data;

  /**
   * Constructor
   */
  public Window2Record(SheetSettings settings)
  {
    super(Type.WINDOW2);

    int selected = settings.isSelected() ? 0x06 : 0x0;

    int options = 0;
    
    options |= 0x0; // display formula values, not formulas

    if (settings.getShowGridLines())
    {
      options |= 0x02;
    }

    options |= 0x04; // display row and column headings

    options |= 0x0; // panes should be not frozen

    if (settings.getDisplayZeroValues())
    {
      options |= 0x10;
    }

    options |= 0x20; // default header

    options |= 0x80; // display outline symbols

    // Handle the freeze panes
    if (settings.getHorizontalFreeze() != 0 ||
        settings.getVerticalFreeze() != 0)
    {
      options |= 0x08;
      selected |= 0x01;
    }

    // hard code the data in for now
    data = new byte[]
    {(byte) options,
     (byte) selected,
     (byte) 0,
     (byte) 0,
     (byte) 0,
     (byte) 0,
     (byte) 0x40,
     (byte) 0,
     (byte) 0,
     (byte) 0,
     (byte) 0,
     (byte) 0,
     (byte) 0,
     (byte) 0,
     (byte) 0,
     (byte) 0,
     (byte) 0,
     (byte) 0 };
  }

  /**
   * Gets the binary data for output to file
   * 
   * @return the binary data
   */
  public byte[] getData()
  {
    return data;
  }
}
