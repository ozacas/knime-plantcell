/*********************************************************************
*
*      Copyright (C) 2004 Andrew Khan
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

package jxl.read.biff;

import common.Assert;

/**
 * Class which encapsulates a data validation (typically in the form of a 7
 * dropdown list box
 */
public class DataValidation
{
  /** 
   * The data validity list
   */
  private DataValidityListRecord validityList;

  /**
   * The data validity record
   */
  private DataValiditySettingsRecord[] validitySettings;

  /**
   * The current position in the validitySettins array
   */
  private int pos;

  /**
   * Constructor
   */
  DataValidation(DataValidityListRecord dvlr)
  {
    validityList = dvlr;
    validitySettings = 
      new DataValiditySettingsRecord[validityList.getNumberOfSettings()];
    pos = 0;
  }

  /**
   * Adds a new settings object to this data validation
   */
  void add(DataValiditySettingsRecord dvsr)
  {
    Assert.verify(pos < validitySettings.length);

    validitySettings[pos] = dvsr;
    pos++;
  }

  /**
   * Accessor for the validity list.  Used when copying sheets
   */
  public DataValidityListRecord getDataValidityList()
  {
    return validityList;
  }

  /**
   * Accessor for the validity settings.  Used when copying sheets
   */
  public DataValiditySettingsRecord[] getDataValiditySettings()
  {
    return validitySettings;
  }

}
