package au.edu.unimelb.plantcell.views.surface.multi;

import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class MySliderRenderer implements TableCellRenderer {

	@Override
	public Component getTableCellRendererComponent(JTable arg0, Object arg1,
			boolean arg2, boolean arg3, int r, int c) {
		Integer i = (Integer) arg1;
	
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
	
		if (c == 5) {
			double z_off = (-50.0d + i.intValue());
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(1);
			return new JLabel(df.format(z_off)+"%");
		} else {
			return new JLabel(i.toString());
		}
	}

}
