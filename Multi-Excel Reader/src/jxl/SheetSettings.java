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

import common.Assert;

import jxl.format.PageOrientation;
import jxl.format.PaperSize;

/**
 * This is a bean which client applications may use to get/set various
 * properties which are associated with a particular worksheet, such
 * as headers and footers, page orientation etc.
 */
public final class SheetSettings
{
  /**
   * The page orientation
   */
  private PageOrientation orientation;

  /**
   * The paper size for printing
   */
  private PaperSize paperSize;

  /**
   * Indicates whether or not this sheet is protected
   */
  private boolean sheetProtected;

  /**
   * Indicates whether or not this sheet is hidden
   */
  private boolean hidden;

  /**
   * Indicates whether or not this sheet is selected
   */
  private boolean selected;

  /**
   * The header
   */
  private HeaderFooter header;

  /**
   * The margin allocated for any page headers, in inches
   */
  private double headerMargin;

  /**
   * The footer
   */
  private HeaderFooter footer;

  /**
   * The margin allocated for any page footers, in inches
   */
  private double footerMargin;

  /**
   * The scale factor used when printing
   */
  private int scaleFactor;

  /**
   * The zoom factor used when viewing.  Note the difference between
   * this and the scaleFactor which is used when printing
   */
  private int zoomFactor;

  /**
   * The page number at which to commence printing
   */
  private int pageStart;

  /**
   * The number of pages into which this excel sheet is squeezed widthwise
   */
  private int fitWidth;

  /**
   * The number of pages into which this excel sheet is squeezed heightwise
   */
  private int fitHeight;

  /**
   * The horizontal print resolution
   */
  private int horizontalPrintResolution;

  /**
   * The vertical print resolution
   */
  private int verticalPrintResolution;

  /**
   * The margin from the left hand side of the paper in inches
   */
  private double leftMargin;

  /**
   * The margin from the right hand side of the paper in inches
   */
  private double rightMargin;

  /**
   * The margin from the top of the paper in inches
   */
  private double topMargin;

  /**
   * The margin from the bottom of the paper in inches
   */
  private double bottomMargin;

  /**
   * Indicates whether to fit the print to the pages or scale the output
   * This field is manipulated indirectly by virtue of the setFitWidth/Height
   * methods
   */
  private boolean fitToPages;

  /**
   * Indicates whether grid lines should be displayed
   */
  private boolean showGridLines;

  /**
   * Indicates whether grid lines should be printed
   */
  private boolean printGridLines;

  /**
   * Indicates whether sheet headings should be printed
   */
  private boolean printHeaders;

  /**
   * Indicates whether the sheet should display zero values
   */
  private boolean displayZeroValues;

  /**
   * The password for protected sheets
   */
  private String password;

  /**
   * The password hashcode - used when copying sheets
   */
  private int passwordHash;

  /**
   * The default column width, in characters
   */
  private int defaultColumnWidth;

  /**
   * The default row height, in 1/20th of a point
   */
  private int defaultRowHeight;

  /**
   * The horizontal freeze pane
   */
  private int horizontalFreeze;

  /**
   * The vertical freeze position
   */
  private int verticalFreeze;
  
  /**
   * Vertical centre flag
   */
  private boolean verticalCentre;

  /**
   * Horizontal centre flag
   */
  private boolean horizontalCentre;

  /**
   * The number of copies to print
   */
  private int copies;

  // ***
  // The defaults
  // **
  private static final PageOrientation defaultOrientation =
    PageOrientation.PORTRAIT;
  private static final PaperSize defaultPaperSize = PaperSize.A4;
  private static final double defaultHeaderMargin = 0.5;
  private static final double defaultFooterMargin = 0.5;
  private static final int    defaultPrintResolution = 0x12c;
  private static final double defaultWidthMargin     = 0.75;
  private static final double defaultHeightMargin    = 1;

  private static final int defaultDefaultColumnWidth = 8;
  private static final int defaultZoomFactor = 100;

  // The publicly accessible values
  /**
   * The default value for the default row height
   */
  public static final int DEFAULT_DEFAULT_ROW_HEIGHT = 0xff;

  /**
   * Default constructor
   */
  public SheetSettings()
  {
    orientation        = defaultOrientation;
    paperSize          = defaultPaperSize;
    sheetProtected     = false;
    hidden             = false;
    selected           = false;
    headerMargin       = defaultHeaderMargin;
    footerMargin       = defaultFooterMargin;
    horizontalPrintResolution = defaultPrintResolution;
    verticalPrintResolution   = defaultPrintResolution;
    leftMargin         = defaultWidthMargin;
    rightMargin        = defaultWidthMargin;
    topMargin          = defaultHeightMargin;
    bottomMargin       = defaultHeightMargin;
    fitToPages         = false;
    showGridLines      = true;
    printGridLines     = false;
    printHeaders       = false;
    displayZeroValues  = true;
    defaultColumnWidth = defaultDefaultColumnWidth;
    defaultRowHeight   = DEFAULT_DEFAULT_ROW_HEIGHT;
    zoomFactor         = defaultZoomFactor;
    horizontalFreeze   = 0;
    verticalFreeze     = 0;
    copies             = 1;
    header             = new HeaderFooter();
    footer             = new HeaderFooter();
  }

  /**
   * Copy constructor.  Called when copying sheets
   * @param copy the settings to copy
   */
  public SheetSettings(SheetSettings copy)
  {
    Assert.verify(copy != null);

    orientation    = copy.orientation;
    paperSize      = copy.paperSize;
    sheetProtected = copy.sheetProtected;
    hidden         = copy.hidden;
    selected       = false; // don't copy the selected flag
    headerMargin   = copy.headerMargin;
    footerMargin   = copy.footerMargin;
    scaleFactor    = copy.scaleFactor;
    pageStart      = copy.pageStart;
    fitWidth       = copy.fitWidth;
    fitHeight      = copy.fitHeight;
    horizontalPrintResolution = copy.horizontalPrintResolution;
    verticalPrintResolution   = copy.verticalPrintResolution;
    leftMargin         = copy.leftMargin;
    rightMargin        = copy.rightMargin;
    topMargin          = copy.topMargin;
    bottomMargin       = copy.bottomMargin;
    fitToPages         = copy.fitToPages;
    password           = copy.password;
    passwordHash       = copy.passwordHash;
    defaultColumnWidth = copy.defaultColumnWidth;
    defaultRowHeight   = copy.defaultRowHeight;
    zoomFactor         = copy.zoomFactor;
    showGridLines      = copy.showGridLines;
    displayZeroValues  = copy.displayZeroValues;
    horizontalFreeze   = copy.horizontalFreeze;
    verticalFreeze     = copy.verticalFreeze;
    horizontalCentre   = copy.horizontalCentre;
    verticalCentre     = copy.verticalCentre;
    copies             = copy.copies;
    header             = new HeaderFooter(copy.header);
    footer             = new HeaderFooter(copy.footer);
  }

  /**
   * Sets the paper orientation for printing this sheet
   *
   * @param po the orientation
   */
  public void setOrientation(PageOrientation po)
  {
    orientation = po;
  }

  /**
   * Accessor for the orientation
   *
   * @return the orientation
   */
  public PageOrientation getOrientation()
  {
    return orientation;
  }

  /**
   * Sets the paper size to be used when printing this sheet
   *
   * @param ps the paper size
   */
  public void setPaperSize(PaperSize ps)
  {
    paperSize = ps;
  }

  /**
   * Accessor for the paper size
   *
   * @return the paper size
   */
  public PaperSize getPaperSize()
  {
    return paperSize;
  }

  /**
   * Queries whether this sheet is protected (ie. read only)
   *
   * @return TRUE if this sheet is read only, FALSE otherwise
   */
  public boolean isProtected()
  {
    return sheetProtected;
  }

  /**
   * Sets the protected (ie. read only) status of this sheet
   *
   * @param p the protected status
   */
  public void setProtected(boolean p)
  {
    sheetProtected = p;
  }

  /**
   * Sets the margin for any page headers
   *
   * @param d the margin in inches
   */
  public void setHeaderMargin(double d)
  {
    headerMargin = d;
  }

  /**
   * Accessor for the header margin
   *
   * @return the header margin
   */
  public double getHeaderMargin()
  {
    return headerMargin;
  }

  /**
   * Sets the margin for any page footer
   *
   * @param d the footer margin in inches
   */
  public void setFooterMargin(double d)
  {
    footerMargin = d;
  }

  /**
   * Accessor for the footer margin
   *
   * @return the footer margin
   */
  public double getFooterMargin()
  {
    return footerMargin;
  }

  /**
   * Sets the hidden status of this worksheet
   *
   * @param h the hidden flag
   */
  public void setHidden(boolean h)
  {
    hidden = h;
  }

  /**
   * Accessor for the hidden nature of this sheet
   *
   * @return TRUE if this sheet is hidden, FALSE otherwise
   */
  public boolean isHidden()
  {
    return hidden;
  }

  /**
   * Sets this sheet to be when it is opened in excel
   *
   * @deprecated use overloaded version which takes a boolean
   */
  public void setSelected()
  {
    setSelected(true);
  }

  /**
   * Sets this sheet to be when it is opened in excel
   *
   * @param s sets whether this sheet is selected or not
   */
  public void setSelected(boolean s)
  {
    selected = s;
  }

  /**
   * Accessor for the selected nature of the sheet
   *
   * @return TRUE if this sheet is selected, FALSE otherwise
   */
  public boolean isSelected()
  {
    return selected;
  }

  /**
   * Sets the scale factor for this sheet to be used when printing.  The
   * parameter is a percentage, therefore setting a scale factor of 100 will
   * print at normal size, 50 half size, 200 double size etc
   *
   * @param sf the scale factor as a percentage
   */
  public void setScaleFactor(int sf)
  {
    scaleFactor = sf;
    fitToPages = false;
  }

  /**
   * Accessor for the scale factor
   *
   * @return the scale factor
   */
  public int getScaleFactor()
  {
    return scaleFactor;
  }

  /**
   * Sets the page number at which to commence printing
   *
   * @param ps the page start number
   */
  public void setPageStart(int ps)
  {
    pageStart = ps;
  }

  /**
   * Accessor for the page start
   *
   * @return the page start
   */
  public int getPageStart()
  {
    return pageStart;
  }

  /**
   * Sets the number of pages widthwise which this sheet should be
   * printed into
   *
   * @param fw the number of pages
   */
  public void setFitWidth(int fw)
  {
    fitWidth = fw;
    fitToPages = true;
  }

  /**
   * Accessor for the fit width
   *
   * @return the number of pages this sheet will be printed into widthwise
   */
  public int getFitWidth()
  {
    return fitWidth;
  }

  /**
   * Sets the number of pages vertically that this sheet will be printed into
   *
   * @param fh the number of pages this sheet will be printed into heightwise
   */
  public void setFitHeight(int fh)
  {
    fitHeight = fh;
    fitToPages = true;
  }

  /**
   * Accessor for the fit height
   *
   * @return the number of pages this sheet will be printed into heightwise
   */
  public int getFitHeight()
  {
    return fitHeight;
  }

  /**
   * Sets the horizontal print resolution
   *
   * @param hpw the print resolution
   */
  public void setHorizontalPrintResolution(int hpw)
  {
    horizontalPrintResolution = hpw;
  }

  /**
   * Accessor for the horizontal print resolution
   *
   * @return the horizontal print resolution
   */
  public int getHorizontalPrintResolution()
  {
    return horizontalPrintResolution;
  }

  /**
   * Sets the vertical print reslution
   *
   * @param vpw the vertical print resolution
   */
  public void setVerticalPrintResolution(int vpw)
  {
    verticalPrintResolution = vpw;
  }

  /**
   * Accessor for the vertical print resolution
   *
   * @return the vertical print resolution
   */
  public int getVerticalPrintResolution()
  {
    return verticalPrintResolution;
  }

  /**
   * Sets the right margin
   *
   * @param m the right margin in inches
   */
  public void setRightMargin(double m)
  {
    rightMargin = m;
  }

  /**
   * Accessor for the right margin
   *
   * @return the right margin in inches
   */
  public double getRightMargin()
  {
    return rightMargin;
  }

  /**
   * Sets the left margin
   *
   * @param m the left margin in inches
   */
  public void setLeftMargin(double m)
  {
    leftMargin = m;
  }

  /**
   * Accessor for the left margin
   *
   * @return the left margin in inches
   */
  public double getLeftMargin()
  {
    return leftMargin;
  }

  /**
   * Sets the top margin
   *
   * @param m the top margin in inches
   */
  public void setTopMargin(double m)
  {
    topMargin = m;
  }

  /**
   * Accessor for the top margin
   *
   * @return the top margin in inches
   */
  public double getTopMargin()
  {
    return topMargin;
  }

  /**
   * Sets the bottom margin
   *
   * @param m the bottom margin in inches
   */
  public void setBottomMargin(double m)
  {
    bottomMargin = m;
  }

  /**
   * Accessor for the bottom margin
   *
   * @return the bottom margin in inches
   */
  public double getBottomMargin()
  {
    return bottomMargin;
  }

  /**
   * Gets the default margin width
   *
   * @return the default margin width
   */
  public double getDefaultWidthMargin()
  {
    return defaultWidthMargin;
  }

  /**
   * Gets the default margin height
   *
   * @return the default margin height
   */
  public double getDefaultHeightMargin()
  {
    return defaultHeightMargin;
  }

  /**
   * Accessor for the fit width print flag
   * @return TRUE if the print is to fit to pages, false otherwise
   */
  public boolean getFitToPages()
  {
    return fitToPages;
  }

  /**
   * Accessor for the fit to pages flag
   * @param b TRUE to fit to pages, FALSE to use a scale factor
   */
  public void setFitToPages(boolean b)
  {
    fitToPages = b;
  }

  /**
   * Accessor for the password
   *
   * @return the password to unlock this sheet, or NULL if not protected
   */
  public String getPassword()
  {
    return password;
  }

  /**
   * Sets the password for this sheet
   *
   * @param s the password
   */
  public void setPassword(String s)
  {
    password = s;
  }

  /**
   * Accessor for the password hash - used only when copying sheets
   *
   * @return passwordHash
   */
  public int getPasswordHash()
  {
    return passwordHash;
  }

  /**
   * Accessor for the password hash - used only when copying sheets
   *
   * @param ph the password hash
   */
  public void setPasswordHash(int ph)
  {
    passwordHash = ph;
  }

  /**
   * Accessor for the default column width
   *
   * @return the default column width, in characters
   */
  public int getDefaultColumnWidth()
  {
    return defaultColumnWidth;
  }

  /**
   * Sets the default column width
   *
   * @param w the new default column width
   */
  public void setDefaultColumnWidth(int w)
  {
    defaultColumnWidth = w;
  }

  /**
   * Accessor for the default row height
   *
   * @return the default row height, in 1/20ths of a point
   */
  public int getDefaultRowHeight()
  {
    return defaultRowHeight;
  }

  /**
   * Sets the default row height
   *
   * @param h the default row height, in 1/20ths of a point
   */
  public void setDefaultRowHeight(int h)
  {
    defaultRowHeight = h;
  }

  /**
   * Accessor for the zoom factor.  Do not confuse zoom factor (which relates
   * to the on screen view) with scale factor (which refers to the scale factor
   * when printing)
   *
   * @return the zoom factor as a percentage
   */
  public int getZoomFactor()
  {
    return zoomFactor;
  }

  /**
   * Sets the zoom factor.  Do not confuse zoom factor (which relates
   * to the on screen view) with scale factor (which refers to the scale factor
   * when printing)
   *
   * @param zf the zoom factor as a percentage
   */
  public void setZoomFactor(int zf)
  {
    zoomFactor = zf;
  }

  /**
   * Accessor for the displayZeroValues property
   *
   * @return TRUE to display zero values, FALSE not to bother
   */
  public boolean getDisplayZeroValues()
  {
    return displayZeroValues;
  }

  /**
   * Sets the displayZeroValues property
   *
   * @param b TRUE to show zero values, FALSE not to bother
   */
  public void setDisplayZeroValues(boolean b)
  {
    displayZeroValues = b;
  }

  /**
   * Accessor for the showGridLines property
   *
   * @return TRUE if grid lines will be shown, FALSE otherwise
   */
  public boolean getShowGridLines()
  {
    return showGridLines;
  }

  /**
   * Sets the showGridLines property
   *
   * @param b TRUE to show grid lines on this sheet, FALSE otherwise
   */
  public void setShowGridLines(boolean b)
  {
    showGridLines = b;
  }

  /**
   * Accessor for the printGridLines property
   *
   * @return TRUE if grid lines will be printed, FALSE otherwise
   */
  public boolean getPrintGridLines()
  {
    return printGridLines;
  }

   /**
   * Sets the printGridLines property
   *
   * @param b TRUE to print grid lines on this sheet, FALSE otherwise
   */
  public void setPrintGridLines(boolean b)
  {
    printGridLines = b;
  }

  /**
   * Accessor for the printHeaders property
   *
   * @return TRUE if headers will be printed, FALSE otherwise
   */
  public boolean getPrintHeaders()
  {
    return printHeaders;
  }

   /**
   * Sets the printHeaders property
   *
   * @param b TRUE to print headers on this sheet, FALSE otherwise
   */
  public void setPrintHeaders(boolean b)
  {
    printHeaders = b;
  }

  /**
   * Gets the row at which the pane is frozen horizontally
   *
   * @return the row at which the pane is horizontally frozen, or 0 if there
   * is no freeze
   */
  public int getHorizontalFreeze()
  {
    return horizontalFreeze;
  }

  /**
   * Sets the row at which the pane is frozen horizontally
   *
   * @param row the row number to freeze at
   */
  public void setHorizontalFreeze(int row)
  {
    horizontalFreeze = Math.max(row, 0);
  }

  /**
   * Gets the column at which the pane is frozen vertically
   *
   * @return the column at which the pane is vertically frozen, or 0 if there
   * is no freeze
   */
  public int getVerticalFreeze()
  {
    return verticalFreeze;
  }

  /**
   * Sets the row at which the pane is frozen vertically
   *
   * @param col the column number to freeze at
   */
  public void setVerticalFreeze(int col)
  {
    verticalFreeze = Math.max(col, 0);
  }

  /**
   * Sets the number of copies
   *
   * @param c the number of copies
   */
  public void setCopies(int c)
  {
    copies = c;
  }

  /**
   * Accessor for the number of copies to print
   *
   * @return the number of copies
   */
  public int getCopies()
  {
    return copies;
  }

  /**
   * Accessor for the header
   *
   * @return the header
   */
  public HeaderFooter getHeader()
  {
    return header;
  }

  /**
   * Sets the header
   *
   * @param h the header
   */
  public void setHeader(HeaderFooter h)
  {
    header = h;
  }

  /**
   * Sets the footer
   *
   * @param f the footer
   */
  public void setFooter(HeaderFooter f)
  {
    footer = f;
  }

  /**
   * Accessor for the footer
   *
   * @return the footer
   */
  public HeaderFooter getFooter()
  {
    return footer;
  }

  /**
   * Accessor for the horizontal centre
   *
	 * @return Returns the horizontalCentre.
	 */
	public boolean isHorizontalCentre()
	{
		return horizontalCentre;
	}
	/**
   * Sets the horizontal centre
   *
	 * @param horizontalCentre The horizontalCentre to set.
	 */
	public void setHorizontalCentre(boolean horizontalCentre)
	{
		this.horizontalCentre = horizontalCentre;
	}

	/**
   * Accessor for the vertical centre
   *
	 * @return Returns the verticalCentre.
	 */
	public boolean isVerticalCentre()
	{
		return verticalCentre;
	}

	/**
   * Sets the vertical centre
	 * @param verticalCentre The verticalCentre to set.
	 */
	public void setVerticalCentre(boolean verticalCentre)
	{
		this.verticalCentre = verticalCentre;
	}
}
