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

package jxl.read.biff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

import common.Assert;
import common.Logger;

import jxl.Cell;
import jxl.DateCell;
import jxl.CellType;
import jxl.Range;
import jxl.SheetSettings;
import jxl.WorkbookSettings;
import jxl.CellReferenceHelper;
import jxl.HeaderFooter;
import jxl.CellFeatures;
import jxl.biff.Type;
import jxl.biff.FormattingRecords;
import jxl.biff.ContinueRecord;
import jxl.biff.WorkspaceInformationRecord;
import jxl.biff.drawing.Chart;
import jxl.format.PaperSize;
import jxl.format.PageOrientation;
import jxl.biff.formula.FormulaException;
import jxl.biff.drawing.ObjRecord;
import jxl.biff.drawing.MsoDrawingRecord;
import jxl.biff.drawing.Drawing;
import jxl.biff.drawing.Comment;
import jxl.biff.drawing.Button;
import jxl.biff.drawing.DrawingData;
import jxl.biff.drawing.TextObjectRecord;
import jxl.biff.drawing.NoteRecord;

/**
 * Reads the sheet.  This functionality was originally part of the
 * SheetImpl class, but was separated out in order to simplify the former
 * class
 */
final class SheetReader
{
  /**
   * The logger
   */
  private static Logger logger = Logger.getLogger(SheetReader.class);

  /**
   * The excel file
   */
  private File excelFile;

  /**
   * A handle to the shared string table
   */
  private SSTRecord sharedStrings;

  /**
   * A handle to the sheet BOF record, which indicates the stream type
   */
  private BOFRecord sheetBof;

  /**
   * A handle to the workbook BOF record, which indicates the stream type
   */
  private BOFRecord workbookBof;

  /**
   * A handle to the formatting records
   */
  private FormattingRecords formattingRecords;

  /**
   * The  number of rows
   */
  private int numRows;

  /**
   * The number of columns
   */
  private int numCols;

  /**
   * The cells
   */
  private Cell[][] cells;

  /**
   * The start position in the stream of this sheet
   */
  private int startPosition;

  /**
   * The list of non-default row properties
   */
  private ArrayList rowProperties;

  /**
   * An array of column info records.  They are held this way before
   * they are transferred to the more convenient array
   */
  private ArrayList columnInfosArray;

  /**
   * A list of shared formula groups
   */
  private ArrayList sharedFormulas;

  /**
   * A list of hyperlinks on this page
   */
  private ArrayList hyperlinks;

  /**
   * A list of merged cells on this page
   */
  private Range[] mergedCells;

  /**
   * The list of data validations on this page
   */
  private DataValidation dataValidation;

  /**
   * The list of charts on this page
   */
  private ArrayList charts;

  /**
   * The list of drawings on this page
   */
  private ArrayList drawings;

  /**
   * The drawing data for the drawings
   */
  private DrawingData drawingData;

  /**
   * Indicates whether or not the dates are based around the 1904 date system
   */
  private boolean nineteenFour;

  /**
   * The PLS print record
   */
  private PLSRecord plsRecord;

  /**
   * The property set record associated with this workbook
   */
  private ButtonPropertySetRecord buttonPropertySet;

  /**
   * The workspace options
   */
  private WorkspaceInformationRecord workspaceOptions;

  /**
   * The horizontal page breaks contained on this sheet
   */
  private int[] rowBreaks;

  /**
   * The sheet settings
   */
  private SheetSettings settings;

  /**
   * The workbook settings
   */
  private WorkbookSettings workbookSettings;

  /**
   * A handle to the workbook which contains this sheet.  Some of the records
   * need this in order to reference external sheets
   */
  private WorkbookParser workbook;

  /**
   * A handle to the sheet
   */
  private SheetImpl sheet;

  /**
   * Constructor
   *
   * @param fr the formatting records
   * @param sst the shared string table
   * @param f the excel file
   * @param sb the bof record which indicates the start of the sheet
   * @param wb the bof record which indicates the start of the sheet
   * @param wp the workbook which this sheet belongs to
   * @param sp the start position of the sheet bof in the excel file
   * @param sh the sheet
   * @param nf 1904 date record flag
   * @exception BiffException
   */
  SheetReader(File f,
              SSTRecord sst,
              FormattingRecords fr,
              BOFRecord sb,
              BOFRecord wb,
              boolean nf,
              WorkbookParser wp,
              int sp,
              SheetImpl sh)
  {
    excelFile = f;
    sharedStrings = sst;
    formattingRecords = fr;
    sheetBof = sb;
    workbookBof = wb;
    columnInfosArray = new ArrayList();
    sharedFormulas = new ArrayList();
    hyperlinks = new ArrayList();
    rowProperties = new ArrayList(10);
    charts = new ArrayList();
    drawings = new ArrayList();
    nineteenFour = nf;
    workbook = wp;
    startPosition = sp;
    sheet = sh;
    settings = new SheetSettings();
    workbookSettings = workbook.getSettings();
  }

  /**
   * Adds the cell to the array
   *
   * @param cell the cell to add
   */
  private void addCell(Cell cell)
  {
    // Sometimes multiple cells (eg. MULBLANK) can exceed the
    // column/row boundaries.  Ignore these

    if (cell.getRow() < numRows && cell.getColumn() < numCols)
    {
      if (cells[cell.getRow()][cell.getColumn()] != null)
      {
        StringBuffer sb = new StringBuffer();
        CellReferenceHelper.getCellReference
          (cell.getColumn(), cell.getRow(), sb);
        logger.warn("Cell " + sb.toString() +
                      " already contains data");
      }
      cells[cell.getRow()][cell.getColumn()] = cell;
    }
    else
    {
      logger.warn("Cell " +
                  CellReferenceHelper.getCellReference
                                   (cell.getColumn(), cell.getRow()) +
                  " exceeds defined cell boundaries in Dimension record " +
                  "(" + numCols + "x" + numRows + ")");
    }
  }

  /**
   * Reads in the contents of this sheet
   */
  final void read()
  {
    Record r = null;
    BaseSharedFormulaRecord sharedFormula = null;
    boolean sharedFormulaAdded = false;

    boolean cont = true;

    // Set the position within the file
    excelFile.setPos(startPosition);

    // Handles to the last drawing and obj records
    MsoDrawingRecord msoRecord = null;
    ObjRecord objRecord = null;
    boolean firstMsoRecord = true;

    // A handle to window2 record
    Window2Record window2Record = null;

    // A handle to printgridlines record
    PrintGridLinesRecord printGridLinesRecord = null;

    // A handle to printheaders record
    PrintHeadersRecord printHeadersRecord = null;

    // Hash map of comments, indexed on objectId.  As each corresponding
    // note record is encountered, these are removed from the array
    HashMap comments = new HashMap();

    while (cont)
    {
      r = excelFile.next();

      if (r.getType() == Type.UNKNOWN && r.getCode() == 0)
      {
        logger.warn("Biff code zero found");

        // Try a dimension record
        if (r.getLength() == 0xa)
        {
          logger.warn("Biff code zero found - trying a dimension record.");
          r.setType(Type.DIMENSION);
        }
        else
        {
          logger.warn("Biff code zero found - Ignoring.");
        }
      }

      if (r.getType() == Type.DIMENSION)
      {
        DimensionRecord dr = null;

        if (workbookBof.isBiff8())
        {
          dr = new DimensionRecord(r);
        }
        else
        {
          dr = new DimensionRecord(r, DimensionRecord.biff7);
        }
        numRows = dr.getNumberOfRows();
        numCols = dr.getNumberOfColumns();
        cells = new Cell[numRows][numCols];
      }
      else if (r.getType() == Type.LABELSST)
      {
        LabelSSTRecord label = new LabelSSTRecord(r,
                                                  sharedStrings,
                                                  formattingRecords,
                                                  sheet);
        addCell(label);
      }
      else if (r.getType() == Type.RK || r.getType() == Type.RK2)
      {
        RKRecord rkr = new RKRecord(r, formattingRecords, sheet);

        if (formattingRecords.isDate(rkr.getXFIndex()))
        {
          DateCell dc = new DateRecord
            (rkr, rkr.getXFIndex(), formattingRecords, nineteenFour, sheet);
          addCell(dc);
        }
        else
        {
          addCell(rkr);
        }
      }
      else if (r.getType() == Type.HLINK)
      {
        HyperlinkRecord hr = new HyperlinkRecord(r, sheet, workbookSettings);
        hyperlinks.add(hr);
      }
      else if (r.getType() == Type.MERGEDCELLS)
      {
        MergedCellsRecord  mc = new MergedCellsRecord(r, sheet);
        if (mergedCells == null)
        {
          mergedCells = mc.getRanges();
        }
        else
        {
          Range[] newMergedCells =
            new Range[mergedCells.length + mc.getRanges().length];
          System.arraycopy(mergedCells, 0, newMergedCells, 0,
                           mergedCells.length);
          System.arraycopy(mc.getRanges(),
                           0,
                           newMergedCells, mergedCells.length,
                           mc.getRanges().length);
          mergedCells = newMergedCells;
        }
      }
      else if (r.getType() == Type.MULRK)
      {
        MulRKRecord mulrk = new MulRKRecord(r);

        // Get the individual cell records from the multiple record
        int num = mulrk.getNumberOfColumns();
        int ixf = 0;
        for (int i = 0; i < num; i++)
        {
          ixf = mulrk.getXFIndex(i);

          NumberValue nv = new NumberValue
            (mulrk.getRow(),
             mulrk.getFirstColumn() + i,
             RKHelper.getDouble(mulrk.getRKNumber(i)),
             ixf,
             formattingRecords,
             sheet);


          if (formattingRecords.isDate(ixf))
          {
            DateCell dc = new DateRecord(nv, 
                                         ixf, 
                                         formattingRecords,
                                         nineteenFour, 
                                         sheet);
            addCell(dc);
          }
          else
          {
            nv.setNumberFormat(formattingRecords.getNumberFormat(ixf));
            addCell(nv);
          }
        }
      }
      else if (r.getType() == Type.NUMBER)
      {
        NumberRecord nr = new NumberRecord(r, formattingRecords, sheet);

        if (formattingRecords.isDate(nr.getXFIndex()))
        {
          DateCell dc = new DateRecord(nr,
                                       nr.getXFIndex(),
                                       formattingRecords,
                                       nineteenFour, sheet);
          addCell(dc);
        }
        else
        {
          addCell(nr);
        }
      }
      else if (r.getType() == Type.BOOLERR)
      {
        BooleanRecord br = new BooleanRecord(r, formattingRecords, sheet);

        if (br.isError())
        {
          ErrorRecord er = new ErrorRecord(br.getRecord(), formattingRecords,
                                           sheet);
          addCell(er);
        }
        else
        {
          addCell(br);
        }
      }
      else if (r.getType() == Type.PRINTGRIDLINES)
      {
        printGridLinesRecord = new PrintGridLinesRecord(r);
        settings.setPrintGridLines(printGridLinesRecord.getPrintGridLines());
      }
      else if (r.getType() == Type.PRINTHEADERS)
      {
        printHeadersRecord = new PrintHeadersRecord(r);
        settings.setPrintHeaders(printHeadersRecord.getPrintHeaders());
      }
      else if (r.getType() == Type.WINDOW2)
      {
        window2Record = new Window2Record(r);

        settings.setShowGridLines(window2Record.getShowGridLines());
        settings.setDisplayZeroValues(window2Record.getDisplayZeroValues());
        settings.setSelected(true);
      }
      else if (r.getType() == Type.PANE)
      {
        PaneRecord pr = new PaneRecord(r);

        if (window2Record != null &&
            window2Record.getFrozen())
        {
          settings.setVerticalFreeze(pr.getRowsVisible());
          settings.setHorizontalFreeze(pr.getColumnsVisible());
        }
      }
      else if (r.getType() == Type.CONTINUE)
      {
        ;
      }
      else if (r.getType() == Type.NOTE)
      {
        if (!workbookSettings.getDrawingsDisabled())
        {
          NoteRecord nr = new NoteRecord(r);

          // Get the comment for the object id
          Comment comment = (Comment) comments.remove
            (new Integer(nr.getObjectId()));

          if (comment == null)
          {
            logger.warn(" cannot find comment for note id " +
                        nr.getObjectId() + "...ignoring");
          }
          else
          {
            comment.setNote(nr);

            drawings.add(comment);

            addCellComment(comment.getColumn(),
                           comment.getRow(),
                           comment.getText(),
                           comment.getWidth(),
                           comment.getHeight());
          }
        }
      }
      else if (r.getType() == Type.ARRAY)
      {
        ;
      }
      else if (r.getType() == Type.PROTECT)
      {
        ProtectRecord pr = new ProtectRecord(r);
        settings.setProtected(pr.isProtected());
      }
      else if (r.getType() == Type.SHAREDFORMULA)
      {
        if (sharedFormula == null)
        {
          logger.warn("Shared template formula is null - " +
                      "trying most recent formula template");
          SharedFormulaRecord lastSharedFormula =
            (SharedFormulaRecord) sharedFormulas.get(sharedFormulas.size() - 1);

          if (lastSharedFormula != null)
          {
            sharedFormula = lastSharedFormula.getTemplateFormula();
          }
        }

        SharedFormulaRecord sfr = new SharedFormulaRecord
          (r, sharedFormula, workbook, workbook, sheet);
        sharedFormulas.add(sfr);
        sharedFormula = null;
      }
      else if (r.getType() == Type.FORMULA || r.getType() == Type.FORMULA2)
      {
        FormulaRecord fr = new FormulaRecord(r,
                                             excelFile,
                                             formattingRecords,
                                             workbook,
                                             workbook,
                                             sheet,
                                             workbookSettings);

        if (fr.isShared())
        {
          BaseSharedFormulaRecord prevSharedFormula = sharedFormula;
          sharedFormula = (BaseSharedFormulaRecord) fr.getFormula();

          // See if it fits in any of the shared formulas
          sharedFormulaAdded = addToSharedFormulas(sharedFormula);

          if (sharedFormulaAdded)
          {
            sharedFormula = prevSharedFormula;
          }

          // If we still haven't added the previous base shared formula,
          // revert it to an ordinary formula and add it to the cell
          if (!sharedFormulaAdded && prevSharedFormula != null)
          {
            // Do nothing.  It's possible for the biff file to contain the
            // record sequence
            // FORMULA-SHRFMLA-FORMULA-SHRFMLA-FORMULA-FORMULA-FORMULA
            // ie. it first lists all the formula templates, then it
            // lists all the individual formulas
            addCell(revertSharedFormula(prevSharedFormula));
          }
        }
        else
        {
          Cell cell = fr.getFormula();
          try
          {
            // See if the formula evaluates to date
            if (fr.getFormula().getType() == CellType.NUMBER_FORMULA)
            {
              NumberFormulaRecord nfr = (NumberFormulaRecord) fr.getFormula();
              if (formattingRecords.isDate(nfr.getXFIndex()))
              {
                cell = new DateFormulaRecord(nfr,
                                             formattingRecords,
                                             workbook,
                                             workbook,
                                             nineteenFour,
                                             sheet);
              }
            }
            
            addCell(cell);
          }
          catch (FormulaException e)
          {
            // Something has gone wrong trying to read the formula data eg. it
            // might be unsupported biff7 data
            logger.warn
              (CellReferenceHelper.getCellReference
               (cell.getColumn(), cell.getRow()) + " " + e.getMessage());
          }
        }
      }
      else if (r.getType() == Type.LABEL)
      {
        LabelRecord lr = null;

        if (workbookBof.isBiff8())
        {
          lr = new LabelRecord(r, formattingRecords, sheet, workbookSettings);
        }
        else
        {
          lr = new LabelRecord(r, formattingRecords, sheet, workbookSettings,
                               LabelRecord.biff7);
        }
        addCell(lr);
      }
      else if (r.getType() == Type.RSTRING)
      {
        RStringRecord lr = null;

        // RString records are obsolete in biff 8
        Assert.verify(!workbookBof.isBiff8());
        lr = new RStringRecord(r, formattingRecords,
                               sheet, workbookSettings,
                               RStringRecord.biff7);
        addCell(lr);
      }
      else if (r.getType() == Type.NAME)
      {
        ;
      }
      else if (r.getType() == Type.PASSWORD)
      {
        PasswordRecord pr = new PasswordRecord(r);
        settings.setPasswordHash(pr.getPasswordHash());
      }
      else if (r.getType() == Type.ROW)
      {
        RowRecord rr = new RowRecord(r);

        // See if the row has anything funny about it
        if (!rr.isDefaultHeight() ||
            !rr.matchesDefaultFontHeight() ||
            rr.isCollapsed() ||
            rr.hasDefaultFormat())
        {
          rowProperties.add(rr);
        }
      }
      else if (r.getType() == Type.BLANK)
      {
        if (!workbookSettings.getIgnoreBlanks())
        {
          BlankCell bc = new BlankCell(r, formattingRecords, sheet);
          addCell(bc);
        }
      }
      else if (r.getType() == Type.MULBLANK)
      {
        if (!workbookSettings.getIgnoreBlanks())
        {
          MulBlankRecord mulblank = new MulBlankRecord(r);

          // Get the individual cell records from the multiple record
          int num = mulblank.getNumberOfColumns();

          for (int i = 0; i < num; i++)
          {
            int ixf = mulblank.getXFIndex(i);

            MulBlankCell mbc = new MulBlankCell
              (mulblank.getRow(),
               mulblank.getFirstColumn() + i,
               ixf,
               formattingRecords,
               sheet);
            
            addCell(mbc);
          }
        }
      }
      else if (r.getType() == Type.SCL)
      {
        SCLRecord scl = new SCLRecord(r);
        settings.setZoomFactor(scl.getZoomFactor());
      }
      else if (r.getType() == Type.COLINFO)
      {
        ColumnInfoRecord cir = new ColumnInfoRecord(r);
        columnInfosArray.add(cir);
      }
      else if (r.getType() == Type.HEADER)
      {
        HeaderRecord hr = null;
        if (workbookBof.isBiff8())
        {
          hr = new HeaderRecord(r, workbookSettings);
        }
        else
        {
          hr = new HeaderRecord(r, workbookSettings, HeaderRecord.biff7);
        }

        HeaderFooter header = new HeaderFooter(hr.getHeader());
        settings.setHeader(header);
      }
      else if (r.getType() == Type.FOOTER)
      {
        FooterRecord fr = null;
        if (workbookBof.isBiff8())
        {
          fr = new FooterRecord(r, workbookSettings);
        }
        else
        {
          fr = new FooterRecord(r, workbookSettings, FooterRecord.biff7);
        }

        HeaderFooter footer = new HeaderFooter(fr.getFooter());
        settings.setFooter(footer);
      }
      else if (r.getType() == Type.SETUP)
      {
        SetupRecord sr = new SetupRecord(r);
        if (sr.isPortrait())
        {
          settings.setOrientation(PageOrientation.PORTRAIT);
        }
        else
        {
          settings.setOrientation(PageOrientation.LANDSCAPE);
        }
        settings.setPaperSize(PaperSize.getPaperSize(sr.getPaperSize()));
        settings.setHeaderMargin(sr.getHeaderMargin());
        settings.setFooterMargin(sr.getFooterMargin());
        settings.setScaleFactor(sr.getScaleFactor());
        settings.setPageStart(sr.getPageStart());
        settings.setFitWidth(sr.getFitWidth());
        settings.setFitHeight(sr.getFitHeight());
        settings.setHorizontalPrintResolution
          (sr.getHorizontalPrintResolution());
        settings.setVerticalPrintResolution(sr.getVerticalPrintResolution());
        settings.setCopies(sr.getCopies());

        if (workspaceOptions != null)
        {
          settings.setFitToPages(workspaceOptions.getFitToPages());
        }
      }
      else if (r.getType() == Type.WSBOOL)
      {
        workspaceOptions = new WorkspaceInformationRecord(r);
      }
      else if (r.getType() == Type.DEFCOLWIDTH)
      {
        DefaultColumnWidthRecord dcwr = new DefaultColumnWidthRecord(r);
        settings.setDefaultColumnWidth(dcwr.getWidth());
      }
      else if (r.getType() == Type.DEFAULTROWHEIGHT)
      {
        DefaultRowHeightRecord drhr = new DefaultRowHeightRecord(r);
        if (drhr.getHeight() != 0)
        {
          settings.setDefaultRowHeight(drhr.getHeight());
        }
      }
      else if (r.getType() == Type.LEFTMARGIN)
      {
        MarginRecord m = new LeftMarginRecord(r);
        settings.setLeftMargin(m.getMargin());
      }
      else if (r.getType() == Type.RIGHTMARGIN)
      {
        MarginRecord m = new RightMarginRecord(r);
        settings.setRightMargin(m.getMargin());
      }
      else if (r.getType() == Type.TOPMARGIN)
      {
        MarginRecord m = new TopMarginRecord(r);
        settings.setTopMargin(m.getMargin());
      }
      else if (r.getType() == Type.BOTTOMMARGIN)
      {
        MarginRecord m = new BottomMarginRecord(r);
        settings.setBottomMargin(m.getMargin());
      }
      else if (r.getType() == Type.HORIZONTALPAGEBREAKS)
      {
        HorizontalPageBreaksRecord dr = null;

        if (workbookBof.isBiff8())
        {
          dr = new HorizontalPageBreaksRecord(r);
        }
        else
        {
          dr = new HorizontalPageBreaksRecord
            (r, HorizontalPageBreaksRecord.biff7);
        }
        rowBreaks = dr.getRowBreaks();
      }
      else if (r.getType() == Type.PLS)
      {
        plsRecord = new PLSRecord(r);
      }
      else if (r.getType() == Type.DVAL)
      {
        // Leave these until another day.  They need to be wrapped inside
        // an implementation of DrawingGroupObject
        //        DataValidityListRecord dvlr = new DataValidityListRecord(r);
        //        dataValidation = new DataValidation(dvlr);
      } 
      else if (r.getType() == Type.HCENTER)
      {
      	CentreRecord hr = new CentreRecord(r);
      	settings.setHorizontalCentre(hr.isCentre());
      } 
      else if (r.getType() == Type.VCENTER)
      {
      	CentreRecord vc = new CentreRecord(r);
      	settings.setVerticalCentre(vc.isCentre());
      }
      else if (r.getType() == Type.DV)
      {
        // Leave these until another day.  They need to be wrapped inside
        // an implementation of DrawingGroupObject
        //        DataValiditySettingsRecord dvsr = new DataValiditySettingsRecord(r);
        //        dataValidation.add(dvsr);
      }
      else if (r.getType() == Type.OBJ)
      {
        objRecord = new ObjRecord(r);

        if (!workbookSettings.getDrawingsDisabled())
        {
          handleObjectRecord(objRecord, msoRecord, comments);
        }

        // Save chart handling until the chart BOF record appears
        if (objRecord.getType() != ObjRecord.CHART)
        {
          objRecord = null;
          msoRecord = null;
        }
      }
      else if (r.getType() == Type.MSODRAWING)
      {
        if (!workbookSettings.getDrawingsDisabled())
        {
          if (msoRecord != null)
          {
            // For form controls, a rogue MSODRAWING record can crop up
            // after the main one.  Add these into the drawing data
            drawingData.addRawData(msoRecord.getData());
          }
          msoRecord = new MsoDrawingRecord(r);

          if (firstMsoRecord)
          {
            msoRecord.setFirst();
            firstMsoRecord = false;
          }
        }
      }
      else if (r.getType() == Type.BUTTONPROPERTYSET)
      {
        buttonPropertySet = new ButtonPropertySetRecord(r);
      }
      else if (r.getType() == Type.BOF)
      {
        BOFRecord br = new BOFRecord(r);
        Assert.verify(!br.isWorksheet());

        int startpos = excelFile.getPos() - r.getLength() - 4;

        // Skip to the end of the nested bof
        // Thanks to Rohit for spotting this
        Record r2 = excelFile.next();
        while (r2.getCode() != Type.EOF.value)
        {
          r2 = excelFile.next();
        }

        if (br.isChart())
        {
          if (!workbook.getWorkbookBof().isBiff8())
          {
            logger.warn("only biff8 charts are supported");
          }
          else
          {

            if (drawingData == null)
            {
              drawingData = new DrawingData();
            }
          
            if (!workbookSettings.getDrawingsDisabled())
            {
              Chart chart = new Chart(msoRecord, objRecord, drawingData,
                                      startpos, excelFile.getPos(),
                                      excelFile, workbookSettings);
              charts.add(chart);

              if (workbook.getDrawingGroup() != null)
              {
                workbook.getDrawingGroup().add(chart);
              }
            }
          }

          // Reset the drawing records
          msoRecord = null;
          objRecord = null;
        }

        // If this worksheet is just a chart, then the EOF reached
        // represents the end of the sheet as well as the end of the chart
        if (sheetBof.isChart())
        {
          cont = false;
        }
      }
      else if (r.getType() == Type.EOF)
      {
        cont = false;
      }
    }

    // Restore the file to its accurate position
    excelFile.restorePos();

    // Add all the shared formulas to the sheet as individual formulas
    Iterator i = sharedFormulas.iterator();

    while (i.hasNext())
    {
      SharedFormulaRecord sfr = (SharedFormulaRecord) i.next();

      Cell[] sfnr = sfr.getFormulas(formattingRecords, nineteenFour);

      for (int sf = 0; sf < sfnr.length; sf++)
      {
        addCell(sfnr[sf]);
      }
    }

    // If the last base shared formula wasn't added to the sheet, then
    // revert it to an ordinary formula and add it
    if (!sharedFormulaAdded && sharedFormula != null)
    {
      addCell(revertSharedFormula(sharedFormula));
    }

    // If there is a stray msoDrawing record, then flag to the drawing group
    // that one has been omitted
    if (msoRecord != null && workbook.getDrawingGroup() != null)
    {
      workbook.getDrawingGroup().setDrawingsOmitted(msoRecord, objRecord);
    }
      
    // Check that the comments hash is empty
    if (!comments.isEmpty())
    {
      logger.warn("Not all comments have a corresponding Note record");
    }
  }

  /**
   * Sees if the shared formula belongs to any of the shared formula
   * groups
   *
   * @param fr the candidate shared formula
   * @return TRUE if the formula was added, FALSE otherwise
   */
  private boolean addToSharedFormulas(BaseSharedFormulaRecord fr)
  {
    Iterator i = sharedFormulas.iterator();
    boolean added = false;
    SharedFormulaRecord sfr = null;

    while (i.hasNext() && !added)
    {
      sfr = (SharedFormulaRecord) i.next();
      added = sfr.add(fr);
    }

    return added;
  }

  /**
   * Reverts the shared formula passed in to an ordinary formula and adds
   * it to the list
   *
   * @param f the formula
   * @return the new formula
   * @exception FormulaException
   */
  private Cell revertSharedFormula(BaseSharedFormulaRecord f) 
  {
    // String formulas look for a STRING record soon after the formula
    // occurred.  Temporarily the position in the excel file back
    // to the point immediately after the formula record
    int pos = excelFile.getPos();
    excelFile.setPos(f.getFilePos());

    FormulaRecord fr = new FormulaRecord(f.getRecord(),
                                         excelFile,
                                         formattingRecords,
                                         workbook,
                                         workbook,
                                         FormulaRecord.ignoreSharedFormula,
                                         sheet,
                                         workbookSettings);

    try
    {
    Cell cell = fr.getFormula();

    // See if the formula evaluates to date
    if (fr.getFormula().getType() == CellType.NUMBER_FORMULA)
    {
      NumberFormulaRecord nfr = (NumberFormulaRecord) fr.getFormula();
      if (formattingRecords.isDate(fr.getXFIndex()))
      {
        cell = new DateFormulaRecord(nfr,
                                     formattingRecords,
                                     workbook,
                                     workbook,
                                     nineteenFour,
                                     sheet);
      }
    }

    excelFile.setPos(pos);
    return cell;
    }
    catch (FormulaException e)
    {
      // Something has gone wrong trying to read the formula data eg. it
      // might be unsupported biff7 data
      logger.warn
        (CellReferenceHelper.getCellReference(fr.getColumn(), fr.getRow()) + 
         " " + e.getMessage());

      return null;
    }
  }


  /**
   * Accessor
   *
   * @return the number of rows
   */
  final int getNumRows()
  {
    return numRows;
  }

  /**
   * Accessor
   *
   * @return the number of columns
   */
  final int getNumCols()
  {
    return numCols;
  }

  /**
   * Accessor
   *
   * @return the cells
   */
  final Cell[][] getCells()
  {
    return cells;
  }

  /**
   * Accessor
   *
   * @return the row properties
   */
  final ArrayList    getRowProperties()
  {
    return rowProperties;
  }

  /**
   * Accessor
   *
   * @return the column information
   */
  final ArrayList getColumnInfosArray()
  {
    return columnInfosArray;
  }

  /**
   * Accessor
   *
   * @return the hyperlinks
   */
  final ArrayList getHyperlinks()
  {
    return hyperlinks;
  }

  /**
   * Accessor
   *
   * @return the charts
   */
  final ArrayList getCharts()
  {
    return charts;
  }

  /**
   * Accessor
   *
   * @return the drawings
   */
  final ArrayList getDrawings()
  {
    return drawings;
  }

  /**
   * Accessor
   *
   * @return the data validations
   */
  final DataValidation getDataValidation()
  {
    return dataValidation;
  }

  /**
   * Accessor
   *
   * @return the ranges
   */
  final Range[] getMergedCells()
  {
    return mergedCells;
  }

  /**
   * Accessor
   *
   * @return the sheet settings
   */
  final SheetSettings getSettings()
  {
    return settings;
  }

  /**
   * Accessor
   *
   * @return the row breaks
   */
  final int[] getRowBreaks()
  {
    return rowBreaks;
  }

  /**
   * Accessor
   *
   * @return the workspace options
   */
  final WorkspaceInformationRecord getWorkspaceOptions()
  {
    return workspaceOptions;
  }

  /**
   * Accessor
   *
   * @return the environment specific print record
   */
  final PLSRecord getPLS()
  {
    return plsRecord;
  }

  /**
   * Accessor for the button property set, used during copying
   *
   * @return the button property set
   */
  final ButtonPropertySetRecord getButtonPropertySet()
  {
    return buttonPropertySet;
  }

  /**
   * Adds a cell comment to a cell just read in
   *
   * @param col the column for the comment
   * @param row the row for the comment
   * @param text the comment text
   * @param width the width of the comment text box
   * @param height the height of the comment text box
   */
  private void addCellComment(int col, int row, String text, 
                              double width,
                              double height)
  {
    Cell c = cells[row][col];
    if (c == null)
    {
      logger.warn("Cell at " + CellReferenceHelper.getCellReference(col, row) +
                  " not present - adding a blank");
      MulBlankCell mbc = new MulBlankCell(row,
                                          col,
                                          0,
                                          formattingRecords,
                                          sheet);
      CellFeatures cf = new CellFeatures();
      cf.setReadComment(text, width, height);
      mbc.setCellFeatures(cf);
      addCell(mbc);

      return;
    }

    if (c instanceof CellFeaturesAccessor)
    {
      CellFeaturesAccessor cv = (CellFeaturesAccessor) c;
      CellFeatures cf = cv.getCellFeatures();

      if (cf == null)
      {
        cf = new CellFeatures();
        cv.setCellFeatures(cf);
      }

      cf.setReadComment(text, width ,height);
    }
    else
    {
      logger.warn("Not able to add comment to cell type " +
                  c.getClass().getName() +
                  " at " + CellReferenceHelper.getCellReference(col, row));
    }
  }

  /**
   * Reads in the object record
   *
   * @param objRecord the obj record
   * @param msoRecord the mso drawing record read in earlier
   * @param comments the hash map of comments
   */
  private void handleObjectRecord(ObjRecord objRecord,
                                  MsoDrawingRecord msoRecord,
                                  HashMap comments)
  {
    if (msoRecord == null)
    {
      logger.warn("Object record is not associated with a drawing " +
                  " record - ignoring");
      return;
    }

    // Handle images
    if (objRecord.getType() == ObjRecord.PICTURE)
    {
      if (drawingData == null)
      {
        drawingData = new DrawingData();
      }

      Drawing drawing = new Drawing(msoRecord,
                                    objRecord,
                                    drawingData,
                                    workbook.getDrawingGroup());
      drawings.add(drawing);
      return;
    }

    // Handle comments
    if (objRecord.getType() == ObjRecord.EXCELNOTE)
    {
      if (drawingData == null)
      {
        drawingData = new DrawingData();
      }

      Comment comment = new Comment(msoRecord,
                                    objRecord,
                                    drawingData,
                                    workbook.getDrawingGroup(),
                                    workbookSettings);

      Record r2 = excelFile.next();
      if (r2.getType() == Type.MSODRAWING)
      {
        MsoDrawingRecord mso = new MsoDrawingRecord(r2);
        comment.addMso(mso);
        r2 = excelFile.next();
      }
      Assert.verify(r2.getType() == Type.TXO);
      TextObjectRecord txo = new TextObjectRecord(r2);
      comment.setTextObject(txo);

      r2 = excelFile.next();
      Assert.verify(r2.getType() == Type.CONTINUE);
      ContinueRecord text = new ContinueRecord(r2);
      comment.setText(text);

      r2 = excelFile.next();
      if (r2.getType() == Type.CONTINUE)
      {
        ContinueRecord formatting = new ContinueRecord(r2);
        comment.setFormatting(formatting);
      }

      comments.put(new Integer(comment.getObjectId()), comment);
      return;
    }

    // Handle form buttons
    if (objRecord.getType() == ObjRecord.BUTTON)
    {
      if (drawingData == null)
      {
        drawingData = new DrawingData();
      }

      Button button = new Button(msoRecord,
                                 objRecord,
                                 drawingData,
                                 workbook.getDrawingGroup(),
                                 workbookSettings);

      Record r2 = excelFile.next();
      if (r2.getType() == Type.MSODRAWING)
      {
        MsoDrawingRecord mso = new MsoDrawingRecord(r2);
        button.addMso(mso);
        r2 = excelFile.next();
      }
      Assert.verify(r2.getType() == Type.TXO);
      TextObjectRecord txo = new TextObjectRecord(r2);
      button.setTextObject(txo);

      r2 = excelFile.next();
      Assert.verify(r2.getType() == Type.CONTINUE);
      ContinueRecord text = new ContinueRecord(r2);
      button.setText(text);

      r2 = excelFile.next();
      if (r2.getType() == Type.CONTINUE)
      {
        ContinueRecord formatting = new ContinueRecord(r2);
        button.setFormatting(formatting);
      }

      drawings.add(button);

      return;
    }

    // Handle charts
    if (objRecord.getType() != ObjRecord.CHART)
    {
      logger.warn(objRecord.getType() + " on sheet \"" +
                  sheet.getName() +
                  "\" not supported - omitting");
      if (drawingData == null)
      {
        drawingData = new DrawingData();
      }

      drawingData.addData(msoRecord.getData());

      if (workbook.getDrawingGroup() != null) // can be null for Excel 95
      {
        workbook.getDrawingGroup().setDrawingsOmitted(msoRecord,
                                                      objRecord);
      }

      return;
    }
  }
}
