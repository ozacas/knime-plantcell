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

package jxl.write.biff;

import java.io.OutputStream;
import java.io.IOException;

import common.Logger;
import jxl.WorkbookSettings;
import jxl.biff.ByteData;

/**
 * A file of excel data to be written out.  All the excel data is held
 * in memory, and when the close method is called a CompoundFile object
 * is used to write the Biff oriented excel data in the CompoundFile
 * format
 */
public final class File
{
  /**
   * The logger
   */
  private static Logger logger = Logger.getLogger(File.class);

  /**
   * The data from the excel 97 file
   */
  private byte[] data;
  /**
   * The current position within the file
   */
  private int pos;
  /**
   * The output stream
   */
  private OutputStream outputStream;
  /**
   * The initial file size
   */
  private int initialFileSize;
  /**
   * The amount to increase the growable array by
   */
  private int arrayGrowSize;
  /**
   * The workbook settings
   */
  private WorkbookSettings workbookSettings;
  /**
   * The read compound file.  This will only be non-null if there are macros
   * or other property sets of that ilk which that we should be copying
   */
  jxl.read.biff.CompoundFile readCompoundFile;

  /**
   * Constructor
   * 
   * @param os the output stream
   * @param ws the configuration settings for this workbook
   * @param rcf the rea compound file
   */
  File(OutputStream os, WorkbookSettings ws, jxl.read.biff.CompoundFile rcf)
  {
    initialFileSize = ws.getInitialFileSize();
    arrayGrowSize = ws.getArrayGrowSize();
    data = new byte[initialFileSize];
    pos = 0;
    outputStream = os;
    workbookSettings = ws;
    readCompoundFile = rcf;
  }

  /**
   * Closes the file.  In fact, this writes out all the excel data
   * to disk using a CompoundFile object, and then frees up all the memory
   * allocated to the workbook
   * 
   * @exception IOException 
   * @param cs TRUE if this should close the stream, FALSE if the application
   * closes it
   */
  void close(boolean cs) throws IOException, JxlWriteException
  {
    CompoundFile cf = new CompoundFile(data, pos, outputStream, 
                                       readCompoundFile);
    cf.write();
    
    outputStream.flush();

    if (cs)
    {
      outputStream.close();
    }

    // Cleanup a bit
    data = null;

    if (!workbookSettings.getGCDisabled())
    {
      System.gc();
    }
  }

  /**
   * Adds the biff record data to the memory allocated for this File
   * 
   * @exception IOException 
   * @param record the record to add to the excel data
   */
  public void write(ByteData record) throws IOException
  {
    try
    {
    byte[] bytes = record.getBytes();
    
    while (pos + bytes.length > data.length)
    {
      // Grow the array
      byte[] newdata = new byte[data.length + arrayGrowSize];
      System.arraycopy(data, 0, newdata, 0, pos);
      data = newdata;
    }

    System.arraycopy(bytes, 0, data, pos, bytes.length);
    pos += bytes.length;
    }
    catch (Throwable t)
    {
      t.printStackTrace();
      throw new RuntimeException(t);
    }

  }

  /**
   * Gets the current position within the file
   * 
   * @return the current position
   */
  int getPos()
  {
    return pos;
  }

  /**
   * Used to manually alter the contents of the written out data.  This
   * is used when cross-referencing cell records
   * 
   * @param pos the position to alter
   * @param newdata the data to modify
   */
  void setData(byte[] newdata, int pos)
  {
    System.arraycopy(newdata, 0, data, pos, newdata.length);
  }

  /**
   * Sets a new output file.  This allows the smae workbook to be
   * written to various different output files without having to
   * read in any templates again
   *
   * @param os the output stream
   */
  public void setOutputFile(OutputStream os)
  {
    if (data != null)
    {
      logger.warn("Rewriting a workbook with non-empty data");
    }

    outputStream = os;
    data = new byte[initialFileSize]; 
    pos = 0;
  }
}
