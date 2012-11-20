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

package jxl.biff.formula;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.HashMap;

import common.Logger;

/**
 * A class which contains the function names for the current workbook. The
 * function names can potentially vary from workbook to workbook depending
 * on the locale
 */
public class FunctionNames
{
  /**
   * The logger class
   */
  private static Logger logger = Logger.getLogger(FunctionNames.class);

  /**
   * A hash mapping keyed on the function and returning its locale specific 
   * name
   */
  private HashMap names;

  /**
   * A hash mapping keyed on the locale specific name and returning the 
   * function
   */
  private HashMap functions;

  /**
   * Constructor
   * @ws the workbook settings
   */
  public FunctionNames(Locale l)
  {
    ResourceBundle rb = ResourceBundle.getBundle("functions", l);
    names = new HashMap(Function.functions.length);
    functions = new HashMap(Function.functions.length);

    // Iterate through all the functions, adding them to the hash maps
    Function f = null;
    String n = null;
    String propname = null;
    for (int i =  0; i < Function.functions.length ; i++)
    {
      f = Function.functions[i];
      propname = f.getPropertyName();

      n = propname.length() != 0 ? rb.getString(propname) : null;
      
      if (n != null)
      {
        names.put(f, n);
        functions.put(n, f);
      }
    }
  }

  /**
   * Gets the function for the specified name
   */
  Function getFunction(String s)
  {
    return (Function) functions.get(s);
  }

  /**
   * Gets the name for the function
   */
  String getName(Function f)
  {
    return (String) names.get(f);
  }
}
