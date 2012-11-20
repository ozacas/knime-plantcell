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

package jxl.biff;

import jxl.read.biff.Record;

/**
 * A record detailing whether the sheet is protected
 */
public class WorkspaceInformationRecord extends WritableRecordData
{
  /**
   * The options byte
   */
  private int wsoptions;

  // the masks
  private static final int fitToPages = 0x100;
  private static final int defaultOptions = 0x4c1;

  /**
   * Constructs this object from the raw data
   *
   * @param t the raw data
   */
  public WorkspaceInformationRecord(Record t)
  {
    super(t);
    byte[] data = getRecord().getData();

    wsoptions = IntegerHelper.getInt(data[0], data[1]);
  }

  /**
   * Constructs this object from the raw data
   */
  public WorkspaceInformationRecord()
  {
    super(Type.WSBOOL);
    wsoptions = defaultOptions;
  }

  /**
   * Gets the fit to pages flag
   *
   * @return TRUE if fit to pages is set
   */
  public boolean getFitToPages()
  {
    return ((wsoptions & fitToPages) != 0);
  }

  /**
   * Sets the fit to page flag
   *
   * @param b fit to page indicator
   */
  public void setFitToPages(boolean b)
  {
    wsoptions = b ? wsoptions | fitToPages :
                    wsoptions & ~fitToPages;
  }

  /**
   * Gets the binary data for output to file
   *
   * @return the binary data
   */
  public byte[] getData()
  {
    byte[] data = new byte[2];

    // Hard code in the information for now
    IntegerHelper.getTwoBytes(wsoptions, data, 0);

    return data;
  }
}









