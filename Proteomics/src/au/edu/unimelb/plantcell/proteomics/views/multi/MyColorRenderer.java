package au.edu.unimelb.plantcell.proteomics.views.multi;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import org.jzy3d.colors.Color;

public class MyColorRenderer extends DefaultTableCellRenderer {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5991903288984178307L;

	@Override
	public Component getTableCellRendererComponent(JTable arg0, Object arg1, boolean arg2, boolean arg3, int arg4, int arg5) {
		Color c = (Color) arg1;
		java.awt.Color awt = new java.awt.Color(c.r/255, c.g/255, c.b/255);
		Component ret = super.getTableCellRendererComponent(arg0, arg1, arg2, arg3, arg4, arg5);
		ret.setBackground(awt);
		ret.setForeground(awt);
		return ret;
	}

}
