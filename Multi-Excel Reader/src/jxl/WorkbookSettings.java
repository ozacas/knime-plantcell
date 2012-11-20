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

package jxl;

import java.util.Locale;
import java.util.HashMap;

import common.Logger;

import jxl.biff.formula.FunctionNames;
import jxl.biff.CountryCode;

/**
 * This is a bean which client applications may use to set various advanced
 * workbook properties.  Use of this bean is not mandatory, and its absence
 * will merely result in workbooks being read/written using the default
 * settings
 */
public final class WorkbookSettings
{
  /**
   * The logger
   */
  private static Logger logger = Logger.getLogger(WorkbookSettings.class);

  /**
   * The amount of memory allocated to store the workbook data when
   * reading a worksheet.  For processeses reading many small workbooks inside
   * a WAS it might be necessary to reduce the default size
   */
  private int initialFileSize;

  /**
   * The amount of memory allocated to the array containing the workbook
   * data when its current amount is exhausted.
   */
  private int arrayGrowSize;

  /**
   * Flag to indicate whether the drawing feature is enabled or not
   * Drawings deactivated using -Djxl.nodrawings=true on the JVM command line
   * Activated by default or by using -Djxl.nodrawings=false on the JVM command
   * line
   */
  private boolean drawingsDisabled;

  /**
   * Flag to indicate whether the name feature is enabled or not
   * Names deactivated using -Djxl.nonames=true on the JVM command line
   * Activated by default or by using -Djxl.nonames=false on the JVM command
   * line
   */
  private boolean namesDisabled;

  /**
   * Flag to indicate whether formula cell references should be adjusted
   * following row/column insertion/deletion
   */
  private boolean formulaReferenceAdjustDisabled;

  /**
   * Flag to indicate whether the system hint garbage collection
   * is enabled or not.
   * As a rule of thumb, it is desirable to enable garbage collection
   * when reading large spreadsheets from  a batch process or from the
   * command line, but better to deactivate the feature when reading
   * large spreadsheets within a WAS, as the calls to System.gc() not
   * only garbage collect the junk in JExcelApi, but also in the
   * webservers JVM and can cause significant slowdown
   * GC deactivated using -Djxl.nogc=true on the JVM command line
   * Activated by default or by using -Djxl.nogc=false on the JVM command line
   */
  private boolean gcDisabled;

  /**
   * Flag to indicate whether the rationalization of cell formats is
   * disabled or not.
   * Rationalization is enabled by default, but may be disabled for
   * performance reasons.  It can be deactivated using -Djxl.norat=true on
   * the JVM command line
   */
  private boolean rationalizationDisabled;

  /**
   * Flag to indicate whether or not the merged cell checking has been
   * disabled
   */
  private boolean mergedCellCheckingDisabled;

  /**
   * Flag to indicate whether the copying of additional property sets
   * are disabled
   */
  private boolean propertySetsDisabled;

  /**
   * Flag to indicate whether or not to ignore blank cells when processing
   * sheets.  Cells which are identified as blank can still have associated
   * cell formats which the processing program may still need to read
   */
  private boolean ignoreBlankCells;

  /**
   * The locale.  Normally this is the same as the system locale, but there
   * may be cases (eg. where you are uploading many spreadsheets from foreign
   * sources) where you may want to specify the locale on an individual
   * worksheet basis
   * The locale may also be specified on the command line using the lang and
   * country System properties eg. -Djxl.lang=en -Djxl.country=UK for UK
   * English
   */
  private Locale locale;

  /**
   * The locale specific function names for this workbook
   */
  private FunctionNames functionNames;

  /**
   * The character encoding used for reading non-unicode strings.  This can
   * be different from the default platform encoding if processing spreadsheets
   * from abroad.  This may also be set using the system property jxl.encoding
   */
  private String encoding;

  /**
   * The character set used by the readable spreadsheeet
   */
  private int characterSet;

  /**
   * The display language used by Excel (ISO 3166 mnemonic)
   */
  private String excelDisplayLanguage;

  /**
   * The regional settings used by Excel (ISO 3166 mnemonic)
   */
  private String excelRegionalSettings;

  /**
   * A hash map of function names keyed on locale
   */
  private HashMap localeFunctionNames;

  // **
  // The default values
  // **
  private static final int defaultInitialFileSize = 5 * 1024 * 1024;
    // 5 megabytes
  private static final int defaultArrayGrowSize = 1024 * 1024; // 1 megabyte

  /**
   * Default constructor
   */
  public WorkbookSettings()
  {
    initialFileSize = defaultInitialFileSize;
    arrayGrowSize = defaultArrayGrowSize;
    localeFunctionNames = new HashMap();
    excelDisplayLanguage = CountryCode.USA.getCode();
    excelRegionalSettings = CountryCode.UK.getCode();

    // Initialize other properties from the system properties
    try
    {
      boolean suppressWarnings = Boolean.getBoolean("jxl.nowarnings");
      setSuppressWarnings(suppressWarnings);
      drawingsDisabled        = Boolean.getBoolean("jxl.nodrawings");
      namesDisabled           = Boolean.getBoolean("jxl.nonames");
      gcDisabled              = Boolean.getBoolean("jxl.nogc");
      rationalizationDisabled = Boolean.getBoolean("jxl.norat");
      mergedCellCheckingDisabled = 
        Boolean.getBoolean("jxl.nomergedcellchecks");
      formulaReferenceAdjustDisabled =
                                Boolean.getBoolean("jxl.noformulaadjust");
      propertySetsDisabled = Boolean.getBoolean("jxl.nopropertysets");
      ignoreBlankCells = Boolean.getBoolean("jxl.ignoreblanks");

      encoding = System.getProperty("file.encoding");
    }
    catch (SecurityException e)
    {
      logger.warn("Error accessing system properties.", e);
    }

    // Initialize the locale to the system locale
    try
    {
      if (System.getProperty("jxl.lang")    == null ||
          System.getProperty("jxl.country") == null)
      {
        locale = Locale.getDefault();
      }
      else
      {
        locale = new Locale(System.getProperty("jxl.lang"),
                            System.getProperty("jxl.country"));
      }

      if (System.getProperty("jxl.encoding") != null)
      {
        encoding = System.getProperty("jxl.encoding");
      }
    }
    catch (SecurityException e)
    {
      logger.warn("Error accessing system properties.", e);
      locale = Locale.getDefault();
    }
  }

  /**
   * Sets the amount of memory by which to increase the amount of
   * memory allocated to storing the workbook data.
   * For processeses reading many small workbooks
   * inside  a WAS it might be necessary to reduce the default size
   * Default value is 1 megabyte
   *
   * @param sz the file size in bytes
   */
  public void setArrayGrowSize(int sz)
  {
    arrayGrowSize = sz;
  }

  /**
   * Accessor for the array grow size property
   *
   * @return the array grow size
   */
  public int getArrayGrowSize()
  {
    return arrayGrowSize;
  }

  /**
   * Sets the initial amount of memory allocated to store the workbook data
   * when reading a worksheet.  For processeses reading many small workbooks
   * inside  a WAS it might be necessary to reduce the default size
   * Default value is 5 megabytes
   *
   * @param sz the file size in bytes
   */
  public void setInitialFileSize(int sz)
  {
    initialFileSize = sz;
  }

  /**
   * Accessor for the initial file size property
   *
   * @return the initial file size
   */
  public int getInitialFileSize()
  {
    return initialFileSize;
  }

  /**
   * Gets the drawings disabled flag
   *
   * @return TRUE if drawings are disabled, FALSE otherwise
   */
  public boolean getDrawingsDisabled()
  {
    return drawingsDisabled;
  }

  /**
   * Accessor for the disabling of garbage collection
   *
   * @return FALSE if JExcelApi hints for garbage collection, TRUE otherwise
   */
  public boolean getGCDisabled()
  {
    return gcDisabled;
  }

  /**
   * Accessor for the disabling of interpretation of named ranges
   *
   * @return FALSE if named cells are interpreted, TRUE otherwise
   */
  public boolean getNamesDisabled()
  {
    return namesDisabled;
  }

  /**
   * Disables the handling of names
   *
   * @param b TRUE to disable the names feature, FALSE otherwise
   */
  public void setNamesDisabled(boolean b)
  {
    namesDisabled = b;
  }

  /**
   * Disables the handling of drawings
   *
   * @param b TRUE to disable the names feature, FALSE otherwise
   */
  public void setDrawingsDisabled(boolean b)
  {
    drawingsDisabled = b;
  }

  /**
   * Sets whether or not to rationalize the cell formats before
   * writing out the sheet.  The default value is true
   *
   * @param r the rationalization flag
   */
  public void setRationalization(boolean r)
  {
    rationalizationDisabled = !r;
  }

  /**
   * Accessor to retrieve the rationalization flag
   *
   * @return TRUE if rationalization is off, FALSE if rationalization is on
   */
  public boolean getRationalizationDisabled()
  {
    return rationalizationDisabled;
  }

  /**
   * Accessor to retrieve the merged cell checking flag
   *
   * @return TRUE if merged cell checking is off, FALSE if it is on
   */
  public boolean getMergedCellCheckingDisabled()
  {
    return mergedCellCheckingDisabled;
  }

  /**
   * Accessor to set the merged cell checking 
   *
   * @param b - TRUE to enable merged cell checking, FALSE otherwise
   */
  public void setMergedCellChecking(boolean b)
  {
    mergedCellCheckingDisabled = !b;
  }

  /**
   * Sets whether or not to enable any property sets (such as macros)
   * to be copied along with the workbook
   * Leaving this feature enabled will result in the JXL process using
   * more memory
   *
   * @param r the property sets flag
   */
  public void setPropertySets(boolean r)
  {
    propertySetsDisabled = !r;
  }

  /**
   * Accessor to retrieve the property sets disabled flag
   *
   * @return TRUE if property sets are disabled, FALSE otherwise
   */
  public boolean getPropertySetsDisabled()
  {
    return propertySetsDisabled;
  }

  /**
   * Accessor to set the suppress warnings flag.  Due to the change
   * in logging in version 2.4, this will now set the warning
   * behaviour across the JVM (depending on the type of logger used)
   *
   * @param w the flag
   */
  public void setSuppressWarnings(boolean w)
  {
    logger.setSuppressWarnings(w);
  }

  /**
   * Accessor for the formula adjust disabled
   *
   * @return TRUE if formulas are adjusted following row/column inserts/deletes
   *         FALSE otherwise
   */
  public boolean getFormulaAdjust()
  {
    return !formulaReferenceAdjustDisabled;
  }

  /**
   * Setter for the formula adjust disabled property
   *
   * @param b TRUE to adjust formulas, FALSE otherwise
   */
  public void setFormulaAdjust(boolean b)
  {
    formulaReferenceAdjustDisabled = !b;
  }

  /**
   * Sets the locale used by JExcelApi to generate the spreadsheet.  
   * Setting this value has no effect on the language or region of
   * the generated excel file
   *
   * @param l the locale
   */
  public void setLocale(Locale l)
  {
    locale = l;
  }

  /**
   * Returns the locale used by JExcelAPI to read the spreadsheet
   *
   * @return the locale
   */
  public Locale getLocale()
  {
    return locale;
  }

  /**
   * Accessor for the character encoding
   *
   * @return the character encoding for this workbook
   */
  public String getEncoding()
  {
    return encoding;
  }

  /**
   * Sets the encoding for this workbook
   *
   * @param enc the encoding
   */
  public void setEncoding(String enc)
  {
    encoding = enc;
  }

  /**
   * Gets the function names.  This is used by the formula parsing package
   * in order to get the locale specific function names for this particular
   * workbook
   *
   * @return the list of function names
   */
  public FunctionNames getFunctionNames()
  {
    if (functionNames == null)
    {
      functionNames = (FunctionNames) localeFunctionNames.get(locale);

      // have not previously accessed function names for this locale,
      // so create a brand new one and add it to the list
      if (functionNames == null)
      {
        functionNames = new FunctionNames(locale);
        localeFunctionNames.put(locale, functionNames);
      }
    }

    return functionNames;
  }

  /**
   * Accessor for the character set.   This value is only used for reading
   * and has no effect when writing out the spreadsheet
   *
   * @return the character set used by this spreadsheet
   */
  public int getCharacterSet()
  {
    return characterSet;
  }

  /**
   * Sets the character set.  This is only used when the spreadsheet is
   * read, and has no effect when the spreadsheet is written
   *
   * @param cs the character set encoding value
   */
  public void setCharacterSet(int cs)
  {
    characterSet = cs;
  }

  /**
   * Sets the garbage collection disabled
   *
   * @param disabled 
   */
  public void setGCDisabled(boolean disabled)
  {
    gcDisabled = disabled;
  }

  /**
   * Sets the ignore blanks flag
   *
   * @param ignoreBlanks
   */
  public void setIgnoreBlanks(boolean ignoreBlanks)
  {
    ignoreBlankCells = ignoreBlanks;
  }

  /**
   * Accessor for the ignore blanks flag
   *
   * @return TRUE if blank cells are being ignored, FALSE otherwise
   */
  public boolean getIgnoreBlanks()
  {
    return ignoreBlankCells;
  }

  /**
   * Returns the two character ISO 3166 mnemonic used by excel for user 
   * language displayto display 
   */
  public String getExcelDisplayLanguage()
  {
    return excelDisplayLanguage;
  }

  /**
   * Returns the two character ISO 3166 mnemonic used by excel for 
   * its regional settings
   */
  public String getExcelRegionalSettings()
  {
    return excelRegionalSettings;
  }

  /**
   * Sets the language in which the generated file will display
   *
   * @param code the two character ISO 3166 country code
   */
  public void setExcelDisplayLanguage(String code)
  {
    excelDisplayLanguage = code;
  }

  /**
   * Sets the regional settings for the generated excel file
   * 
   * @param code the two character ISO 3166 country code
   */
  public void setExcelRegionalSettings(String code)
  {
    excelRegionalSettings = code;
  }
}
