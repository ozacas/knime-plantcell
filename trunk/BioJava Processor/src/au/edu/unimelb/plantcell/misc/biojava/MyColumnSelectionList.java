package au.edu.unimelb.plantcell.misc.biojava;

import java.util.Collection;

import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.ColumnSelectionList;

public class MyColumnSelectionList extends ColumnSelectionList {
	private MyColumnFilterListModel m_model;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4928270577923122805L;

	public MyColumnSelectionList(ListSelectionListener listener) {
		super();
		setUserSelectionAllowed(true);
	    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    addListSelectionListener(listener);
	}
	
	@Override
	public void update(DataTableSpec dts) {
		if (m_model != null) {
			m_model.setTableSpec(dts);
		}
		super.update(dts);
	}
	
	@Override
	public void update(DataTableSpec dts, String... strings) {
		if (m_model != null) {
			m_model.setTableSpec(dts);
		}
		super.update(dts, strings);
	}
	
	@Override 
	public void update(DataTableSpec dts, Collection<String> strings) {
		if (m_model != null) {
			m_model.setTableSpec(dts);
		}
		super.update(dts, strings);
	}
	
	@Override
	public ListModel getModel() {
		if (m_model == null) {
			m_model = new MyColumnFilterListModel();
		}
		return m_model;
	}
	
	public boolean hasSuitableColumns() {
		return (m_model.getSize() > 0);
	}
	
	/**
	 * Override to ensure we wrap the underlying <code>ListModel</code> into a suitable
	 * form for being able to filter unsuitable columns for the current task
	 */
	@Override
	public void setModel(ListModel new_model) {
		// NO-OP
	}
	
	public void setColumnFilter(ColumnFilter cf) {
		m_model.setColumnFilter(cf);
	}
}
