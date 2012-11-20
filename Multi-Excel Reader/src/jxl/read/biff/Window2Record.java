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

package jxl.read.biff;

import common.Logger;

import jxl.biff.IntegerHelper;
import jxl.biff.RecordData;

/**
 * Contains the cell dimensions of this worksheet
 */
class Window2Record extends RecordData
{
  /**
   * The logger
   */
  private static Logger logger = Logger.getLogger(Window2Record.class);

  /**
   * Selected flag
   */
  private boolean selected;
  /**
   * Show grid lines flag
   */
  private boolean showGridLines;
  /**
   * Display zero values flag
   */
  private boolean displayZeroValues;
  /**
   * The window contains frozen panes
   */
  private boolean frozenPanes;
  /**
   * The window contains panes that are frozen but not split
   */
  private boolean frozenNotSplit;

  /**
   * Constructs the dimensions from the raw data
   *
   * @param t the raw data
   */
  public Window2Record(Record t)
  {
    super(t);
    byte[] data = t.getData();

    int options = IntegerHelper.getInt(data[0], data[1]);

    selected = ((options & 0x200) != 0);
    showGridLines = ((options & 0x02) != 0);
    frozenPanes = ((options & 0x08) != 0);
    displayZeroValues = ((options & 0x10) != 0);
    frozenNotSplit = ((options & 0x100) != 0);
  }

  /**
   * Accessor for the selected flag
   *
   * @return TRUE if this sheet is selected, FALSE otherwise
   */
  public boolean isSelected()
  {
    return selected;
  }

  /**
   * Accessor for the show grid lines flag
   *
   * @return TRUE to show grid lines, FALSE otherwise
   */
  public boolean getShowGridLines()
  {
    return showGridLines;
  }

  /**
   * Accessor for the zero values flag
   *
   * @return TRUE if this sheet displays zero values, FALSE otherwise
   */
  public boolean getDisplayZeroValues()
  {
    return displayZeroValues;
  }

  /**
   * Accessor for the frozen panes flag
   *
   * @return TRUE if this contains frozen panes, FALSE otherwise
   */
  public boolean getFrozen()
  {
    return frozenPanes;
  }

  /**
   * Accessor for the frozen not split flag
   *
   * @return TRUE if this contains frozen, FALSE otherwise
   */
  public boolean getFrozenNotSplit()
  {
    return frozenNotSplit;
  }
}







