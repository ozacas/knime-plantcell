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

package jxl.biff;

import common.Logger;
import jxl.WorkbookSettings;
import jxl.biff.formula.ExternalSheet;
import jxl.biff.formula.FormulaParser;
import jxl.biff.formula.FormulaException;

/**
 * Class which parses the binary data associated with Data Validity (DV)
 * setting
 */
public class DVParser
{
  /**
   * The logger
   */
  private static Logger logger = Logger.getLogger(DVParser.class);

  // DV Type
  public static class DVType 
  {
    private int value;
    
    private static DVType[] types = new DVType[0];
   
    DVType(int v) 
    {
      value = v;
      DVType[] oldtypes = types;
      types = new DVType[oldtypes.length+1];
      System.arraycopy(oldtypes, 0, types, 0, oldtypes.length);
      types[oldtypes.length] = this;
    }

    static DVType getType(int v)
    {
      DVType found = null;
      for (int i = 0 ; i < types.length && found == null ; i++)
      {
        if (types[i].value == v)
        {
          found = types[i];
        }
      }
      return found;
    }

    public int getValue() 
    {
      return value;
    }
  }

  // Error Style
  public static class ErrorStyle
  {
    private int value;
    
    private static ErrorStyle[] types = new ErrorStyle[0];
   
    ErrorStyle(int v) 
    {
      value = v;
      ErrorStyle[] oldtypes = types;
      types = new ErrorStyle[oldtypes.length+1];
      System.arraycopy(oldtypes, 0, types, 0, oldtypes.length);
      types[oldtypes.length] = this;
    }

    static ErrorStyle getErrorStyle(int v)
    {
      ErrorStyle found = null;
      for (int i = 0 ; i < types.length && found == null ; i++)
      {
        if (types[i].value == v)
        {
          found = types[i];
        }
      }
      return found;
    }

    public int getValue() 
    {
      return value;
    }
  }

  // Conditions
  public static class Condition
  {
    private int value;
    
    private static Condition[] types = new Condition[0];
   
    Condition(int v) 
    {
      value = v;
      Condition[] oldtypes = types;
      types = new Condition[oldtypes.length+1];
      System.arraycopy(oldtypes, 0, types, 0, oldtypes.length);
      types[oldtypes.length] = this;
    }

    static Condition getCondition(int v)
    {
      Condition found = null;
      for (int i = 0 ; i < types.length && found == null ; i++)
      {
        if (types[i].value == v)
        {
          found = types[i];
        }
      }
      return found;
    }

    public int getValue() 
    {
      return value;
    }
  }


  // The values
  public static final DVType ANY = new DVType(0);
  public static final DVType INTEGER = new DVType(1);
  public static final DVType DECIMAL = new DVType(2);
  public static final DVType LIST = new DVType(3);
  public static final DVType DATE = new DVType(4);
  public static final DVType TIME = new DVType(5);
  public static final DVType TEXT_LENGTH = new DVType(6);
  public static final DVType FORMULA = new DVType(7);

  // The error styles
  public static final ErrorStyle STOP = new ErrorStyle(0);
  public static final ErrorStyle WARNING = new ErrorStyle(1);
  public static final ErrorStyle INFO = new ErrorStyle(2);

  // The conditions
  public static final Condition BETWEEN = new Condition(0);
  public static final Condition NOT_BETWEEN = new Condition(1);
  public static final Condition EQUAL = new Condition(2);
  public static final Condition NOT_EQUAL = new Condition(3);
  public static final Condition GREATER_THAN = new Condition(4);
  public static final Condition LESS_THAN = new Condition(5);
  public static final Condition GREATER_EQUAL = new Condition(6);
  public static final Condition LESS_EQUAL = new Condition(7);

  // The masks
  private static int STRING_LIST_GIVEN_MASK = 0x80;
  private static int EMPTY_CELLS_ALLOWED_MASK = 0x100;
  private static int SUPPRESS_ARROW_MASK = 0x200;
  private static int SHOW_PROMPT_MASK = 0x40000;
  private static int SHOW_ERROR_MASK = 0x80000;

  /**
   * The type
   */
  private DVType type;

  /**
   * The error style
   */
  private ErrorStyle errorStyle;

  /**
   * The condition
   */
  private Condition condition;

  /**
   * String list option
   */
  private boolean stringListGiven;

  /**
   * Empty cells allowed
   */
  private boolean emptyCellsAllowed;

  /**
   * Suppress arrow
   */
  private boolean suppressArrow;

  /**
   * Show prompt
   */
  private boolean showPrompt;

  /**
   * Show error
   */
  private boolean showError;

  /**
   * The title of the prompt box
   */
  private String promptTitle;

  /**
   * The title of the error box
   */
  private String errorTitle;

  /**
   * The text of the prompt box
   */
  private String promptText;

  /**
   * The text of the error box
   */
  private String errorText;

  /**
   * The first formula
   */
  private FormulaParser formula1;

  /**
   * The second formula
   */
  private FormulaParser formula2;

  /**
   * The column number of the cell at the top left of the range
   */
  private int column1;

  /**
   * The row number of the cell at the top left of the range
   */
  private int row1;

  /**
   * The column index of the cell at the bottom right
   */
  private int column2;

  /**
   * The row index of the cell at the bottom right
   */
  private int row2;

  /**
   * Constructor
   */
  public DVParser(byte[] data, 
                  ExternalSheet es, 
                  WorkbookMethods nt,
                  WorkbookSettings ws) throws FormulaException
  {
    int options = IntegerHelper.getInt(data[0], data[1], data[2], data[3]);
    
    int typeVal = options & 0xf;
    type = DVType.getType(typeVal);

    int errorStyleVal = (options & 0x70) >> 4;
    errorStyle = ErrorStyle.getErrorStyle(errorStyleVal);

    int conditionVal = (options & 0xf00000) >> 20;
    condition = Condition.getCondition(conditionVal);

    stringListGiven = (options & STRING_LIST_GIVEN_MASK) != 0;
    emptyCellsAllowed = (options & EMPTY_CELLS_ALLOWED_MASK) != 0;
    suppressArrow = (options & SUPPRESS_ARROW_MASK) != 0;
    showPrompt = (options & SHOW_PROMPT_MASK) != 0;
    showError = (options & SHOW_ERROR_MASK) != 0;

    int pos = 4;
    int length = IntegerHelper.getInt(data[pos], data[pos+1]);
    promptTitle = StringHelper.getUnicodeString(data, length, pos + 2);
    pos += length * 2 + 2;

    length = IntegerHelper.getInt(data[pos], data[pos+1]);
    errorTitle = StringHelper.getUnicodeString(data, length, pos + 2);
    pos += length * 2 + 2;

    length = IntegerHelper.getInt(data[pos], data[pos+1]);
    promptText = StringHelper.getUnicodeString(data, length, pos + 2);
    pos += length * 2 + 2;

    length = IntegerHelper.getInt(data[pos], data[pos+1]);
    errorText = StringHelper.getUnicodeString(data, length, pos + 2);
    pos += length * 2 + 2;

    int formulaLength = IntegerHelper.getInt(data[pos], data[pos+1]);
    pos += 4;
    if (formulaLength != 0)
    {
      byte[] tokens = new byte[formulaLength];
      System.arraycopy(data, pos, tokens, 0, formulaLength);
      formula1 = new FormulaParser(tokens, null, es, nt,ws);
      formula1.parse();
      pos += formulaLength;
    }

    formulaLength = IntegerHelper.getInt(data[pos], data[pos+1]);
    pos += 4;
    if (formulaLength != 0)
    {
      byte[] tokens = new byte[formulaLength];
      System.arraycopy(data, pos, tokens, 0, formulaLength);
      formula2 = new FormulaParser(tokens, null, es, nt, ws);
      formula2.parse();
      pos += formulaLength;
    }

    pos += 2;

    row1 = IntegerHelper.getInt(data[pos], data[pos+1]);
    pos += 2;

    row2 = IntegerHelper.getInt(data[pos], data[pos+1]);
    pos += 2;

    column1 = IntegerHelper.getInt(data[pos], data[pos+1]);
    pos += 2;

    column2 = IntegerHelper.getInt(data[pos], data[pos+1]);
    pos += 2;
  }

  /**
   * Gets the data
   */
  public byte[] getData()
  {
    // Compute the length of the data
    byte[] f1Bytes = formula1 != null ? formula1.getBytes() : new byte[0];
    byte[] f2Bytes = formula2 != null ? formula2.getBytes() : new byte[0];
    int dataLength = 
      4 + // the options
      promptTitle.length() * 2 + 2 + // the prompt title
      errorTitle.length() * 2 + 2 + // the error title
      promptText.length() * 2 + 2 + // the prompt text
      errorText.length() * 2 + 2 + // the error text
      f1Bytes.length + 2 + // first formula
      f2Bytes.length + 2 + // second formula
      + 4 + // unused bytes
      10; // cell range

    byte[] data = new byte[dataLength];

    // The position
    int pos = 0;

    // The options
    int options = 0;
    options |= type.getValue();
    options |= errorStyle.getValue() << 4;
    options |= condition.getValue() << 20;

    if (stringListGiven) 
    {
      options |= STRING_LIST_GIVEN_MASK;
    }

    if (emptyCellsAllowed) 
    {
      options |= EMPTY_CELLS_ALLOWED_MASK;
    }

    if (suppressArrow)
    {
      options |= SUPPRESS_ARROW_MASK;
    }

    if (showPrompt)
    {
      options |= SHOW_PROMPT_MASK;
    }

    if (showError)
    {
      options |= SHOW_ERROR_MASK;
    }

    // The text
    IntegerHelper.getFourBytes(options, data, pos);
    pos += 4;
    
    IntegerHelper.getTwoBytes(promptTitle.length(), data, pos);
    pos += 2;

    StringHelper.getUnicodeBytes(promptTitle, data, pos);
    pos += promptTitle.length() * 2;

    IntegerHelper.getTwoBytes(errorTitle.length(), data, pos);
    pos += 2;

    StringHelper.getUnicodeBytes(errorTitle, data, pos);
    pos += errorTitle.length() * 2;

    IntegerHelper.getTwoBytes(promptText.length(), data, pos);
    pos += 2;

    StringHelper.getUnicodeBytes(promptText, data, pos);
    pos += promptText.length() * 2;

    IntegerHelper.getTwoBytes(errorText.length(), data, pos);
    pos += 2;

    StringHelper.getUnicodeBytes(errorText, data, pos);
    pos += errorText.length() * 2;

    // Formula 1
    IntegerHelper.getTwoBytes(f1Bytes.length, data, pos);
    pos += 4;
    
    System.arraycopy(f1Bytes, 0, data, pos, f1Bytes.length);
    pos += f1Bytes.length;

    // Formula 2
    IntegerHelper.getTwoBytes(f2Bytes.length, data, pos);
    pos += 2;
    
    System.arraycopy(f2Bytes, 0, data, pos, f2Bytes.length);
    pos += f2Bytes.length;

    // Two bytes not used
    pos +=2 ;

    // The cell ranges
    IntegerHelper.getTwoBytes(1, data, pos);
    pos += 2;

    IntegerHelper.getTwoBytes(row1, data, pos);
    pos += 2;

    IntegerHelper.getTwoBytes(row2, data, pos);
    pos += 2;

    IntegerHelper.getTwoBytes(column1, data, pos);
    pos += 2;

    IntegerHelper.getTwoBytes(column2, data, pos);
    pos += 2;

    return data;
  }

  /**
   * Inserts a row
   *
   * @param row the row to insert
   */
  public void insertRow(int row)
  {
    if (formula1 != null)
    {
      formula1.rowInserted(0, row, true);
    }

    if (formula2 != null)
    {
      formula2.rowInserted(0, row, true);
    }

    if (row1 >= row)
    {
      row1++;
    }

    if (row2 >= row)
    {
      row2++;
    }
  }

  /**
   * Inserts a column
   *
   * @param col the column to insert
   */
  public void insertColumn(int col)
  {
    if (formula1 != null)
    {
      formula1.columnInserted(0, col, true);
    }

    if (formula2 != null)
    {
      formula2.columnInserted(0, col, true);
    }

    if (column1 >= col)
    {
      column1++;
    }

    if (column2 >= col)
    {
      column2++;
    }
  }

  /**
   * Removes a row
   *
   * @param row the row to insert
   */
  public void removeRow(int row)
  {
    if (formula1 != null)
    {
      formula1.rowRemoved(0, row, true);
    }

    if (formula2 != null)
    {
      formula2.rowRemoved(0, row, true);
    }

    if (row1 > row)
    {
      row1--;
    }

    if (row2 >= row)
    {
      row2--;
    }
  }

  /**
   * Removes a column
   *
   * @param col the row to remove
   */
  public void removeColumn(int col)
  {
    if (formula1 != null)
    {
      formula1.columnRemoved(0, col, true);
    }

    if (formula2 != null)
    {
      formula2.columnRemoved(0, col, true);
    }

    if (column1 > col)
    {
      column1--;
    }

    if (column2 >= col)
    {
      column2--;
    }
  }

  /**
   * Accessor for first column
   *
   * @return the first column
   */
  public int getFirstColumn()
  {
    return column1;
  }

  /**
   * Accessor for the last column
   *
   * @return the last column
   */
  public int getLastColumn()
  {
    return column2;
  }

  /**
   * Accessor for first row
   *
   * @return the first row
   */
  public int getFirstRow()
  {
    return row1;
  }

  /**
   * Accessor for the last row
   *
   * @return the last row
   */
  public int getLastRow()
  {
    return row2;
  }

}
