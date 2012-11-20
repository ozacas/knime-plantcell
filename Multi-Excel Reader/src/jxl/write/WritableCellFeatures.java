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

package jxl.write;

import jxl.CellFeatures;

/**
 * Container for any additional cell features
 */
public class WritableCellFeatures extends CellFeatures
{
  /**
   * Constructor
   */
  public WritableCellFeatures()
  {
    super();
  }

  /**
   * Copy constructor
   *
   * @param the cell to copy
   */
  public WritableCellFeatures(CellFeatures cf)
  {
    super(cf);
  }

  /**
   * Sets the cell comment
   *
   * @param s the comment
   */
  public void setComment(String s)
  {
    super.setComment(s);
  }

  /**
   * Sets the cell comment and sets the size of the text box (in cells)
   * in which the comment is displayed
   *
   * @param s the comment
   * @param width the width of the comment box in cells
   * @param height the height of the comment box in cells
   */
  public void setComment(String s, double width, double height)
  {
    super.setComment(s, width, height);
  }

  /**
   * Removes the cell comment, if present
   */
  public void removeComment()
  {
    super.removeComment();
  }
}
