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

package jxl.biff.formula;

import jxl.JXLException;

/**
 * Exception thrown when parsing a formula
 */
public class FormulaException extends JXLException
{
  private static class FormulaMessage
  {
    /**
     * The message
     */
    public String message;
    /**
     * Constructs this exception with the specified message
     * 
     * @param m the message
     */
    FormulaMessage(String m) {message = m;}
  }

  /**
   */
  static FormulaMessage unrecognizedToken = 
    new FormulaMessage("Unrecognized token");

  /**
   */
  static FormulaMessage unrecognizedFunction = 
    new FormulaMessage("Unrecognized function");

  /**
   */
  public static FormulaMessage biff8Supported = 
    new FormulaMessage("Only biff8 formulas are supported");

  static FormulaMessage lexicalError = 
    new FormulaMessage("Lexical error:  ");

  static FormulaMessage incorrectArguments = 
    new FormulaMessage("Incorrect arguments supplied to function");

  static FormulaMessage sheetRefNotFound = 
    new FormulaMessage("Could not find sheet");

  static FormulaMessage cellNameNotFound = 
    new FormulaMessage("Could not find named cell");


  /**
   * Constructs this exception with the specified message
   * 
   * @param m the message
   */
  public FormulaException(FormulaMessage m)
  {
    super(m.message);
  }

  /**
   * Constructs this exception with the specified message
   * 
   * @param m the message
   */
  public FormulaException(FormulaMessage m, int val)
  {
    super(m.message + " " + val);
  }

  /**
   * Constructs this exception with the specified message
   * 
   * @param m the message
   */
  public FormulaException(FormulaMessage m, String val)
  {
    super(m.message + " " + val);
  }
}





