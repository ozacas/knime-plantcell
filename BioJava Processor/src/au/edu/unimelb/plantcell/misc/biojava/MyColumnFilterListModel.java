package au.edu.unimelb.plantcell.misc.biojava;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.util.ColumnFilter;

/**
 * 
 * @author andrew.cassin
 *
 */
public class MyColumnFilterListModel extends DefaultListModel implements ListModel, ListDataListener {
	private final List<Object> m_unfiltered_items = new ArrayList<Object>();
	private final List<Object> m_filtered_items   = new ArrayList<Object>();
	private ColumnFilter    m_cf;
	private DataTableSpec   m_dts;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5509473621704114990L;

	public MyColumnFilterListModel() {
		setTableSpec(null);
		setColumnFilter(null);
	}
	
	public MyColumnFilterListModel(DataTableSpec inSpec) {
		setTableSpec(inSpec);
		addListDataListener(this);
	}
	
	public void setTableSpec(DataTableSpec inSpec) {
		m_dts = inSpec;
		m_unfiltered_items.clear();

		if (m_dts != null) {
			Iterator<DataColumnSpec> it = m_dts.iterator();
			while (it.hasNext()) {
				m_unfiltered_items.add(it.next());
			}
		}
		rebuild();
	}
	
	public void setColumnFilter(ColumnFilter cf) {
		m_cf = cf;
		rebuild();
	}
	
	private void rebuild() {
		m_filtered_items.clear();
		
		// no column filter? ok, assume everything is in...
		// else let only those that pass column filtering in
		if (m_dts != null) {
			for (int i=0; i<m_unfiltered_items.size(); i++) {
				DataColumnSpec o = (DataColumnSpec) m_unfiltered_items.get(i);
				if (o != null) {
					if (m_cf == null || m_cf.includeColumn(o)) {
						m_filtered_items.add(o);
					}
				}
			}
		}
		
		fireContentsChanged(this, 0, getSize()-1);
	}

	@Override
	public Object get(int arg0) {
		return getElementAt(arg0);
	}
	
	@Override
	public Object getElementAt(int arg0) {
		return m_filtered_items.get(arg0);
	}

	@Override
	public int getSize() {
		return m_filtered_items.size();
	}

	@Override
	public int indexOf(Object o) {
		return m_filtered_items.indexOf(o);
	}
	
	@Override 
	public int indexOf(Object o, int idx) {
		for (int i=0; i<m_filtered_items.size(); i++) {
			if (o.equals(m_filtered_items.get(i))) {
				return i;
			}
		}
		return -1;
	}
	
	@Override 
	public void insertElementAt(Object o, int idx) {
		throw new RuntimeException("Not implemented!");
	}
	
	@Override
	public boolean isEmpty() {
		return (m_filtered_items.size() == 0);
	}
	
	@Override
	public Object lastElement() {
		return m_filtered_items.get(m_filtered_items.size()-1);
	}
	
	@Override 
	public int lastIndexOf(Object o) {
		throw new RuntimeException("Not implemented!");
	}
	
	@Override 
	public int lastIndexOf(Object o, int idx) {
		throw new RuntimeException("Not implemented!");
	}
	
	@Override
	public Object remove(int idx) {
		return m_filtered_items.remove(idx);
	}
	
	@Override 
	public void removeAllElements() {
		m_filtered_items.clear();
	}
	
	@Override
	public boolean removeElement(Object o) {
		return m_filtered_items.remove(o);
	}
	
	@Override
	public void removeElementAt(int idx) {
		m_filtered_items.remove(idx);
	}
	
	@Override
	public Object set(int idx, Object o) {
		return m_filtered_items.set(idx, o);
	}
	
	@Override
	public void setElementAt(Object o, int idx) {
		set(idx, o);
	}
	
	@Override
	public void setSize(int new_size) {
		for (int i=m_filtered_items.size()-1; i>new_size; i--) {
			m_filtered_items.remove(i);
		}
	}
	
	@Override
	public int size() {
		return getSize();
	}
	
	/*** EVENTS FROM THE unfiltered list items model ***/
	
	@Override
	public void contentsChanged(ListDataEvent e) {
		rebuild();
	}

	@Override
	public void intervalAdded(ListDataEvent e) {
		rebuild();
	}

	@Override
	public void intervalRemoved(ListDataEvent e) {
		rebuild();
	}

}
