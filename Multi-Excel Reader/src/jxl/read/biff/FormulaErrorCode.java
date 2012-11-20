/*********************************************************************
*
*      Copyright (C) 2005 Andrew Khan
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

/**
 * Enumeration for formula error codes
 */
class FormulaErrorCode
{
  int errorCode;
  String description;

  private static FormulaErrorCode[] codes = new FormulaErrorCode[0];
    
  FormulaErrorCode(int code, String desc)
  {
    errorCode = code;
    description = desc;
    FormulaErrorCode[] newcodes = new FormulaErrorCode[codes.length + 1];
    System.arraycopy(codes, 0, newcodes, 0, codes.length);
    newcodes[codes.length] = this;
    codes = newcodes;
  }
  
  static FormulaErrorCode getErrorCode(int code)
  {
    boolean found = false;
    FormulaErrorCode ec = UNKNOWN;
    for (int i = 0 ; i < codes.length && !found ; i++)
    {
      if (codes[i].errorCode == code)
      {
        found = true;
        ec = codes[i];
      }
    }
    return ec;
  }

  public static FormulaErrorCode UNKNOWN = new FormulaErrorCode(0xff, "?");
  public static FormulaErrorCode NULL = new FormulaErrorCode(0x0, "#NULL");
  public static FormulaErrorCode DIV0 = new FormulaErrorCode(0x7, "#DIV/0");
  public static FormulaErrorCode VALUE = new FormulaErrorCode(0xf, "#VALUE");
  public static FormulaErrorCode REF = new FormulaErrorCode(0x17, "#REF");
  public static FormulaErrorCode NAME = new FormulaErrorCode(0x1d, "#NAME");
  public static FormulaErrorCode NUM = new FormulaErrorCode(0x24, "#NUM");
  public static FormulaErrorCode NA = new FormulaErrorCode(0x2a, "#N/A");
}
