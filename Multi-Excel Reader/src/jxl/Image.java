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

package jxl;

import java.io.File;

import jxl.biff.drawing.Drawing;

/**
 * Accessor functions for an image
 */
public interface Image
{
  /**
   * Accessor for the image position
   *
   * @return the column number at which the image is positioned
   */
  public double getColumn();

  /**
   * Accessor for the image position
   *
   * @return the row number at which the image is positions
   */
  public double getRow();

  /**
   * Accessor for the image dimensions
   *
   * @return  the number of columns this image spans
   */
  public double getWidth();

  /**
   * Accessor for the image dimensions
   *
   * @return the number of rows which this image spans
   */
  public double getHeight();

  /**
   * Accessor for the image file
   *
   * @return the file which the image references
   */
  public File getImageFile();

  /**
   * Accessor for the image data
   *
   * @return the image data
   */
  public byte[] getImageData();
}
