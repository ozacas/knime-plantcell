/*********************************************************************
*
*      Copyright (C) 2003 Andrew Khan
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

package jxl.biff.drawing;

/**
 * Enumeration for the BLIP type
 */
final class BlipType
{
  private int value;
  private String desc;

  private static BlipType[] types = new BlipType[0];

  private BlipType(int val, String d)
  {
    value = val;
    desc = d;

    BlipType[] newtypes = new BlipType[types.length+1];
    System.arraycopy(types, 0, newtypes, 0, types.length);
    newtypes[types.length] = this;
    types = newtypes;
  }

  public String getDescription()
  {
    return desc;
  }

  public int getValue()
  {
    return value;
  }

  public static BlipType getType(int val)
  {
    BlipType type = UNKNOWN;
    for (int i = 0 ; i < types.length ; i++)
    {
      if (types[i].value == val)
      {
        type = types[i];
        break;
      }
    }

    return type;
  }

  public static final BlipType ERROR = new BlipType(0, "Error"); // An error occured during loading
  public static final BlipType UNKNOWN = new BlipType(1, "Unknown");            // An unknown blip type
  public static final BlipType EMF = new BlipType(2, "EMF");    // Windows Enhanced Metafile
  public static final BlipType WMF = new BlipType(3, "WMF"); // Windows Metafile
  public static final BlipType PICT = new BlipType(4, "PICT"); // Macintosh PICT
  public static final BlipType JPEG = new BlipType(5, "JPEG");   // JFIF
  public static final BlipType PNG = new BlipType(6, "PNG");  // PNG
  public static final BlipType DIB = new BlipType(7, "DIB");  // Windows DIB
  public static final BlipType FIRST_CLIENT = new BlipType(32, "FIRST");   // First client defined blip type
  public static final BlipType LAST_CLIENT  = new BlipType(255, "LAST");   // Last client defined blip type
}
