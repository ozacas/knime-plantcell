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
import jxl.write.biff.CellValue;
import jxl.biff.drawing.Comment;

/**
 * Container for any additional cell features
 */
public class BaseCellFeatures
{
  /**
   * The logger
   */
  public static Logger logger = Logger.getLogger(BaseCellFeatures.class);

  /**
   * The comment
   */
  private String comment;

  /**
   * The comment width in cells
   */
  private double commentWidth;

  /**
   * The comment height in cells
   */
  private double commentHeight;
  
  /**
   * A handle to the drawing object
   */
  private Comment commentDrawing;

  /**
   * The cell to which this is attached, and which may need to be notified
   */
  private CellValue writableCell;

  // Constants
  private final static double defaultCommentWidth = 3;
  private final static double defaultCommentHeight = 4;

  /**
   * Constructor
   */
  protected BaseCellFeatures()
  {
  }

  /**
   * Copy constructor
   *
   * @param the cell to copy
   */
  public BaseCellFeatures(BaseCellFeatures cf)
  {
    comment = cf.comment;
    commentWidth = cf.commentWidth;
    commentHeight = cf.commentHeight;
  }

  /**
   * Accessor for the cell comment
   */
  protected String getComment()
  {
    return comment;
  }

  /**
   * Accessor for the comment width
   */
  public double getCommentWidth()
  {
    return commentWidth;
  }

  /**
   * Accessor for the comment height
   */
  public double getCommentHeight()
  {
    return commentHeight;
  }

  /** 
   * Called by the cell when the features are added
   *
   * @param wc the writable cell
   */
  public final void setWritableCell(CellValue wc)
  {
    writableCell = wc;
  } 

  /**
   * Internal method to set the cell comment.  Used when reading
   */
  public void setReadComment(String s, double w, double h)
  {
    comment = s;
    commentWidth = w;
    commentHeight = h;
  }

  /**
   * Sets the cell comment
   *
   * @param s the comment
   */
  public void setComment(String s)
  {
    setComment(s, defaultCommentWidth, defaultCommentHeight);
  }

  /**
   * Sets the cell comment
   *
   * @param s the comment
   * @param height the height of the comment box in cells
   * @param width the width of the comment box in cells
   */
  public void setComment(String s, double width, double height)
  {
    comment = s;
    commentWidth = width;
    commentHeight = height;

    if (commentDrawing != null)
    {
      commentDrawing.setCommentText(s);
      commentDrawing.setWidth(width);
      commentDrawing.setWidth(height);
      // commentDrawing is set up when trying to modify a copied cell
    }
  }

  /**
   * Removes the cell comment, if present
   */
  public void removeComment()
  {
    // Set the comment string to be empty
    comment = null;

    // Remove the drawing from the drawing group
    if (commentDrawing != null)
    {
      // do not call DrawingGroup.remove() because comments are not present
      // on the Workbook DrawingGroup record
      writableCell.removeComment(commentDrawing);
      commentDrawing = null;
    }
  }

  /**
   * Sets the comment drawing object
   */
  public final void setCommentDrawing(Comment c)
  {
    commentDrawing = c;
  }

  /**
   * Accessor for the comment drawing
   */
  public final Comment getCommentDrawing()
  {
    return commentDrawing;
  }
}
