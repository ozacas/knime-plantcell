package au.edu.unimelb.plantcell.networks;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import au.edu.unimelb.plantcell.networks.cells.MyEdge;
import au.edu.unimelb.plantcell.networks.cells.MyVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;

public class EdgeFilterDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8151950400965157818L;

	public EdgeFilterDialog(Collection<String> props, final MyFilterRuleModel rule_model) {
		super((Frame)null, "Add edge filter rule...", true);
		final JDialog dlg = this;

		JPanel button_panel  = new JPanel();
		button_panel.setLayout(new BoxLayout(button_panel, BoxLayout.LINE_AXIS));
		button_panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		JButton ok_button = new JButton("OK");
		
		button_panel.add(ok_button);
		button_panel.add(Box.createRigidArea(new Dimension(10, 0)));
		JButton cancel_button = new JButton("Cancel");
		cancel_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				dlg.dispose();
			}
			
		});
		button_panel.add(cancel_button);
		
		JPanel content_panel = new JPanel();
		content_panel.setLayout(new BoxLayout(content_panel, BoxLayout.Y_AXIS));
		content_panel.add(new JLabel("Show only edges where... "));
		JPanel sub_panel = new JPanel();
		sub_panel.setLayout(new BoxLayout(sub_panel, BoxLayout.X_AXIS));
		props.add("<Any>");
		props.add("<Is directly connected to>");
		String[] vec = props.toArray(new String[0]);
		Arrays.sort(vec);
		final JComboBox<String> cb_prop = new JComboBox<String>(vec);
		final JComboBox<String> cb_op   = new JComboBox<String>(new String[] { "<", ">", "!=", "=", ">=", "<=", " contains ", " does not contain", "has annotation", "is not annotated"});
		final JTextField t_val  = new JTextField("0");
		t_val.setColumns(25);

		sub_panel.add(cb_prop);
		sub_panel.add(Box.createRigidArea(new Dimension(5,5)));
		sub_panel.add(cb_op);
		sub_panel.add(Box.createRigidArea(new Dimension(5,5)));
		sub_panel.add(t_val);
		content_panel.add(sub_panel);
		
		Container c = getContentPane();
		c.add(content_panel, BorderLayout.CENTER);
		c.add(button_panel, BorderLayout.PAGE_END);
		
		cb_prop.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Object sel_obj = cb_prop.getSelectedItem();
				if (sel_obj == null)
					return;
				cb_op.setEnabled(!sel_obj.toString().startsWith("<Is directly connected"));
			}
			
		});
		ok_button.addActionListener(new ActionListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void actionPerformed(ActionEvent e) {
				// user wants the rule added so do that here...
				EdgeFilterPredicate<Context<Graph<MyVertex,MyEdge>, MyEdge>> efp = 
					new EdgeFilterPredicate<Context<Graph<MyVertex,MyEdge>, MyEdge>>(cb_prop.getSelectedItem().toString(), 
						cb_op.getSelectedItem().toString(), t_val.getText());
				
				rule_model.addElement(efp);
				
				// and we're done...
				dlg.dispose();
			}
			
		});
		this.pack();
	}
}
