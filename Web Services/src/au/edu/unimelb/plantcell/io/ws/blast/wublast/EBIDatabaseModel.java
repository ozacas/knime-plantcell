package au.edu.unimelb.plantcell.io.ws.blast.wublast;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

public class EBIDatabaseModel implements ListModel {
	private String[]     m_array;
	private List<String> m_list;
	private final List<ListDataListener> m_l = new ArrayList<ListDataListener>();
	
	
	public EBIDatabaseModel(String[] vec) {
		m_array = vec;
		m_list  = null;
	}
	
	public EBIDatabaseModel(List<String> vec) {
		m_list  = vec;
		m_array = null;
	}
	
	@Override
	public void addListDataListener(ListDataListener l) {
		m_l.add(l);
	}

	@Override
	public Object getElementAt(int idx) {
		if (m_list != null) {
			return m_list.get(idx);
		}
		return m_array[idx];
	}

	@Override
	public int getSize() {
		if (m_list != null) {
			return m_list.size();
		}
		return m_array.length;
	}

	@Override
	public void removeListDataListener(ListDataListener l) {
		m_l.remove(l);
	}

}
