package au.edu.unimelb.plantcell.networks;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

public class FilterRuleList extends JList {

	/**
	 * not serialised
	 */
	private static final long serialVersionUID = -4293752495095743489L;

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
		public Component getListCellRendererComponent(JList l, Object val,
				int index, boolean isSelected, boolean hasFocus) {
			return super.getListCellRendererComponent(l, val, index, isSelected, hasFocus);
		}

	}

}
