package au.edu.unimelb.plantcell.views.surface.multi;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jzy3d.colors.Color;

public class MyColorRenderer implements TableCellRenderer {

	@Override
	public Component getTableCellRendererComponent(JTable arg0, Object arg1,
			boolean arg2, boolean arg3, int arg4, int arg5) {
		Color c = (Color) arg1;
		
		JLabel ret = new JLabel("   ");
		
		java.awt.Color awt = new java.awt.Color(c.a/255, c.g/255, c.b/255);
		ret.setBackground(awt);
		ret.setForeground(awt);
		return ret;
	}

}
