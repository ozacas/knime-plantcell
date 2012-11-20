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

package jxl.biff.drawing;

/**
 * Class used to display a complete hierarchically organized Escher stream
 * The whole thing is dumped to System.out
 * 
 * This class is only used as a debugging tool
 */
public class EscherDisplay
{
  private EscherStream stream;

  public EscherDisplay(EscherStream s)
  {
    stream = s;
  }

  public void display()
  {
    EscherRecordData er = new EscherRecordData(stream,0);
    EscherContainer ec = new EscherContainer(er);
    displayContainer(ec, 0);
  }

  private void displayContainer(EscherContainer ec, int level)
  {
    displayRecord(ec, level);

    // Display the contents of the container
    level++;

    EscherRecord[] children = ec.getChildren();

    for (int i = 0; i < children.length ; i++)
    {
      EscherRecord er = children[i];
      if (er.data.isContainer())
      {
        displayContainer( (EscherContainer) er, level);
      }
      else
      {
        displayRecord(er, level);
      }
    }
  }

  private void displayRecord(EscherRecord er, int level)
  {
    indent(level);
    
    EscherRecordType type = er.getType();

    // Display the code
    System.out.print(Integer.toString(type.getValue(), 16));
    System.out.print(" - ");

    // Display the name
    if (type == EscherRecordType.DGG_CONTAINER)
    {
      System.out.println("Dgg Container");
    }
    else if (type == EscherRecordType.BSTORE_CONTAINER)
    {
      System.out.println("BStore Container");
    }
    else if (type == EscherRecordType.DG_CONTAINER)
    {
      System.out.println("Dg Container");
    }
    else if (type == EscherRecordType.SPGR_CONTAINER)
    {
      System.out.println("Spgr Container");
    }
    else if (type == EscherRecordType.SP_CONTAINER)
    {
      System.out.println("Sp Container");
    }
    else if (type == EscherRecordType.DGG)
    {
      System.out.println("Dgg");
    }
    else if (type == EscherRecordType.BSE)
    {
      System.out.println("Bse");
    }
    else if (type == EscherRecordType.DG)
    {
      System.out.println("Dg");
    }
    else if (type == EscherRecordType.SPGR)
    {
      System.out.println("Spgr");
    }
    else if (type == EscherRecordType.SP)
    {
      System.out.println("Sp");
    }
    else if (type == EscherRecordType.OPT)
    {
      System.out.println("Opt");
    }
    else if (type == EscherRecordType.CLIENT_ANCHOR)
    {
      System.out.println("Client Anchor");
    }
    else if (type == EscherRecordType.CLIENT_DATA)
    {
      System.out.println("Client Data");
    }
    else if (type == EscherRecordType.CLIENT_TEXT_BOX)
    {
      System.out.println("Client Text Box");
    }
    else if (type == EscherRecordType.SPLIT_MENU_COLORS)
    {
      System.out.println("Split Menu Colors");
    }
    else
    {
      System.out.println("???");
    }
  }

  private void indent(int level)
  {
    for (int i = 0; i < level * 2; i++)
    {
      System.out.print(' ');
    }
  }
}
