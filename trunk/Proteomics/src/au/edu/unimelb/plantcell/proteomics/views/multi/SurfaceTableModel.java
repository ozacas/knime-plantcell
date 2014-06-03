package au.edu.unimelb.plantcell.proteomics.views.multi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.jzy3d.colors.Color;

/**
 * 
 * @author andrew.cassin
 *
 */
public class SurfaceTableModel implements TableModel {
	private final List<TableModelListener> m_listeners = new ArrayList<TableModelListener>();
	private final static String[] cols = new String[] { "Surface", "Display as ... ?", "Opacity (%)", "Colour", "Size of points (relative)", "Z Offset (%)" };
	private MultiSurfaceNodeModel m_mdl= null;
	private final HashMap<String,Color> m_colours = new HashMap<String,Color>();
	private final HashMap<String,String> m_showas = new HashMap<String,String>();
	private final HashMap<String,Integer> m_alpha = new HashMap<String,Integer>();
	private final HashMap<String,Integer> m_size  = new HashMap<String,Integer>();
	private final HashMap<String,Integer> m_zoffset=new HashMap<String,Integer>();
	
	public SurfaceTableModel(final MultiSurfaceNodeModel mdl) {
		m_mdl = mdl;
	}
	
	@Override
	public void addTableModelListener(TableModelListener arg0) {
		m_listeners.add(arg0);
	}
	
	@Override
	public void removeTableModelListener(TableModelListener arg0) {
		m_listeners.remove(arg0);
	}

	
	@Override
	public Class<?> getColumnClass(int c) {
		if (c == 2 || c == 4 || c == 5)
			return Integer.class;
		if (c == 3)
			return Color.class;
		return String.class;
	}

	@Override
	public int getColumnCount() {
		return cols.length;
	}

	@Override
	public String getColumnName(int c) {
		return cols[c];
	}

	@Override
	public int getRowCount() {
		if (m_mdl == null)
			return 0;
		return m_mdl.getSurfaceCount();
	}

	@Override
	public Object getValueAt(int r, int c) {
		if (c == 0) {
			int i=0;
			for (String s : m_mdl.getZNames()) {
				if (r == i++) {
					return s;
				}
			}
		} 
		String surface_name = (String) getValueAt(r, 0);
		if (surface_name == null)
			return null;
		if (c == 3) {
			return getColour(surface_name);
		} else if (c == 1) {
			return getShowAs(surface_name);
		} else if (c == 2) {
			return getTransparency(surface_name);
		} else if (c == 4) {
			return getSize(surface_name);
		} else if (c == 5) {
			return getZOffset(surface_name);
		}
		return null;
	}

	public Object getZOffset(String surface_name) {
		Integer z = m_zoffset.get(surface_name);
		if (z == null)
			return new Integer(0);		// ie. not offset on Z-axis
		return z;
	}

	private Integer getTransparency(String surface_name) {
		Integer alpha = m_alpha.get(surface_name);
		if (alpha == null)
			return new Integer(100);
		return alpha;
	}
	
	public Integer getSize(String surface_name) {
		Integer size = m_size.get(surface_name);
		if (size == null)
			return new Integer(1);
		return size;
	}

	@Override
	public boolean isCellEditable(int r, int c) {
		return c > 0;		// name of each surface is not editable
	}

	
	@Override
	public void setValueAt(Object o, int r, int c) {
		String surface_name = (String) getValueAt(r, 0);
		if (surface_name == null)
			return;
		
		if (c == 3) {
			Color col = (Color) o;
			//System.err.println("Changing colour for "+surface_name+" to RGB: "+col.r+" "+col.g+" "+col.b);
			m_colours.put(surface_name, col);
		} else if (c == 1) {
			m_showas.put(surface_name, (String) o);
		} else if (c == 2) {
			m_alpha.put(surface_name, (Integer) o);
		} else if (c == 4) {
			m_size.put(surface_name, (Integer) o);
		} else if (c == 5) {
			m_zoffset.put(surface_name, (Integer) o);
		}
		
		fireListeners();
	}
	
	private void fireListeners() {
		TableModelEvent ev = new TableModelEvent(this);
		for (TableModelListener l : m_listeners) {
			l.tableChanged(ev);
		}
	}

	public Color getColour(String surface_name) {
		Color ret = m_colours.get(surface_name);
		if (ret == null) {
			return Color.BLACK;
		}
		return ret;
	}

	public String getShowAs(String surface_name) {
		String s = m_showas.get(surface_name);
		if (s == null) 
			return "Scatter";
		return s;
	}

	public double getAlpha(String surface_name) {
		int i = getTransparency(surface_name);
		return ((double)i) / 100.0d; 
	}

	/**
	 * Called when the view is notified of a change to the model, this code must adjust the table model accordingly
	 * @param nodeModel
	 */
	public void modelChanged(MultiSurfaceNodeModel nodeModel) {
		m_mdl = nodeModel;
	}

}
