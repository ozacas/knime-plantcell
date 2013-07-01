package au.edu.unimelb.plantcell.networks;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

public class FilterRuleList<T> extends JList<MyPredicate<T>> {

	/**
	 * not serialised
	 */
	private static final long serialVersionUID = -4293752495095743489L;

	@SuppressWarnings("unchecked")
	public FilterRuleList(MyFilterRuleModel rule_model) {
		super(rule_model);
		this.setCellRenderer(new MyPredicateRenderer());
		this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}
	
	public class MyPredicateRenderer extends DefaultListCellRenderer {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6046869914350145809L;

		@Override
		public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") JList l, Object val,
				int index, boolean isSelected, boolean hasFocus) {
			return super.getListCellRendererComponent(l, val, index, isSelected, hasFocus);
		}

	}

}
