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

package jxl.write.biff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import common.Logger;
import common.Assert;
import jxl.WorkbookSettings;

/**
 * Class which encapsulates a data validation (typically in the form of a 
 * dropdown list box
 */
public class DataValidation
{
  /**
   * The logger
   */
  private final static Logger logger = Logger.getLogger(DataValidation.class);

  /** 
   * The data validity list
   */
  private DataValidityListRecord validityList;

  /**
   * The data validity record
   */
  private ArrayList validitySettings;

  /**
   * The current position in the validitySettins array
   */
  private int pos;

  /**
   * Handle to the workbook
   */
  private WritableWorkbookImpl workbook;

  /**
   * Handle to the workbook settings
   */
  private WorkbookSettings workbookSettings;

  /**
   * Constructor
   *
   * @param dv the data validations from the read only sheet
   */
  DataValidation(jxl.read.biff.DataValidation dv, 
                 WritableWorkbookImpl w, 
                 WorkbookSettings ws)
  {
    workbook = w;
    workbookSettings = ws;
    validityList = new DataValidityListRecord(dv.getDataValidityList());

    jxl.read.biff.DataValiditySettingsRecord[] settings = 
      dv.getDataValiditySettings();

    validitySettings = new ArrayList(settings.length);
    for (int i = 0; i < settings.length ; i++)
    {
      validitySettings.add(new DataValiditySettingsRecord(settings[i], 
                                                          workbook, 
                                                          workbookSettings));
    }
  }

  /**
   * Constructor
   *
   * @param dv the data validations from the writable sheet
   */
  DataValidation(DataValidation dv,
                 WritableWorkbookImpl w, 
                 WorkbookSettings ws)

  {
    workbook = w;
    workbookSettings = ws;
    validityList = new DataValidityListRecord(dv.validityList);

    validitySettings = new ArrayList(dv.validitySettings.size());

    for (Iterator i = dv.validitySettings.iterator(); i.hasNext(); )
    {
      DataValiditySettingsRecord dvsr = (DataValiditySettingsRecord) i.next();
      validitySettings.add
        (new DataValiditySettingsRecord(dvsr,
                                        workbook,
                                        workbookSettings));
    }
  }


  /**
   * Writes out the data validation
   * 
   * @exception IOException 
   * @param outputFile the output file
   */
  public void write(File outputFile) throws IOException
  {
    if (!validityList.hasDVRecords())
    {
      return;
    }

    outputFile.write(validityList);
    
    for (Iterator i = validitySettings.iterator(); i.hasNext() ; )
    {
      DataValiditySettingsRecord dv = (DataValiditySettingsRecord) i.next();
      outputFile.write(dv);
    }
  }

  /**
   * Inserts a row
   *
   * @param row the inserted row
   */
  public void insertRow(int row)
  {
    for (Iterator i = validitySettings.iterator(); i.hasNext() ; )
    {
      DataValiditySettingsRecord dv = (DataValiditySettingsRecord) i.next();
      dv.insertRow(row);
    }
  }

  /**
   * Inserts a row
   *
   * @param row the inserted row
   */
  public void removeRow(int row)
  {
    for (Iterator i = validitySettings.iterator(); i.hasNext() ; )
    {
      DataValiditySettingsRecord dv = (DataValiditySettingsRecord) i.next();

      if (dv.getFirstRow() == row && dv.getLastRow() == row)
      {
        i.remove();
        validityList.dvRemoved();
      }
      else
      {
        dv.removeRow(row);
      }
    }
  }

  /**
   * Inserts a column
   *
   * @param col the inserted column
   */
  public void insertColumn(int col)
  {
    for (Iterator i = validitySettings.iterator(); i.hasNext() ; )
    {
      DataValiditySettingsRecord dv = (DataValiditySettingsRecord) i.next();
      dv.insertColumn(col);
    }
  }

  /**
   * Removes a column
   *
   * @param col the inserted column
   */
  public void removeColumn(int col)
  {
    for (Iterator i = validitySettings.iterator(); i.hasNext() ; )
    {
      DataValiditySettingsRecord dv = (DataValiditySettingsRecord) i.next();
      
      if (dv.getFirstColumn() == col && dv.getLastColumn() == col)
      {
        i.remove();
        validityList.dvRemoved();
      }
      else
      {
        dv.removeColumn(col);
      }
    }
  }
}
