package au.edu.unimelb.plantcell.io.jemboss.simple;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

public class StandardEmbossDialog extends DefaultNodeSettingsPane {
		private final JEditorPane           m_html_help = new JEditorPane();
		private ACDApplication              m_cur_prog  = null;		// currently selected program
		final   UserSettingsPanel   user_settings_panel = new UserSettingsPanel();
		final   JList                    list_of_fields = new JList(new String[] { "field1" });

		public StandardEmbossDialog() {
			super();
			m_html_help.setPreferredSize(new Dimension(300,150));
			m_html_help.setMinimumSize(m_html_help.getPreferredSize());
		    m_html_help.setEditable(false);   
		    m_html_help.setMaximumSize(new Dimension(640, 400));
			list_of_fields.setPreferredSize(new Dimension(80, 140));
			list_of_fields.setMinimumSize(list_of_fields.getPreferredSize());
			list_of_fields.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		}
		
		public void changeAdvancedTab(ACDApplication prog, String[] current_fields) {
			if (prog == m_cur_prog)
				return;
			
			list_of_fields.setListData(current_fields);
			user_settings_panel.removeAll();
			user_settings_panel.revalidate();
			user_settings_panel.repaint();
			m_cur_prog = prog;
			
			try {
				ACDApplication acdtable = ACDApplication.find("acdtable");
				Thread t = new HTMLUpdateThread(acdtable, prog, m_html_help);
				t.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public void addAdvancedTab() {	
			final JPanel adv_params_tab = new JPanel();
	        adv_params_tab.setLayout(new BoxLayout(adv_params_tab, BoxLayout.Y_AXIS));
	        final JPanel top_panel = new JPanel();
	        top_panel.setLayout(new BoxLayout(top_panel, BoxLayout.X_AXIS));
	        JPanel fields_panel = new JPanel();
	        fields_panel.setLayout(new BoxLayout(fields_panel, BoxLayout.Y_AXIS));
	        user_settings_panel.setLayout(new BoxLayout(user_settings_panel, BoxLayout.Y_AXIS));
	        top_panel.add(fields_panel);
	        top_panel.add(Box.createRigidArea(new Dimension(5,0)));
	        JPanel p = new JPanel();
	        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
	        p.add(new JScrollPane(user_settings_panel));
	        top_panel.add(p);
	        fields_panel.add(new JScrollPane(list_of_fields));
	        JButton b_add = new JButton("Add");
	        JButton b_remove = new JButton("Remove");
	        b_add.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (m_cur_prog == null)
						return;
					Object[] selected = list_of_fields.getSelectedValues();
					if (selected == null || selected.length < 1)
						return;
					for (Object sel : selected) {
						ACDField f = m_cur_prog.getField(sel.toString());
						if (f == null)
							return;
						
						if (user_settings_panel.hasCurrentField(f.getName()))
							return;
						
						UserField uf = new UserField(f);
						user_settings_panel.add(uf);
					}
					user_settings_panel.revalidate();
					user_settings_panel.repaint();
				}
	        	
	        });
	        b_remove.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					Object[] o = list_of_fields.getSelectedValues();
					if (o == null || o.length < 1)
						return;
					for (Object sel : o) {
						String cur_field = sel.toString();
						if (user_settings_panel.hasCurrentField(cur_field)) {
							user_settings_panel.remove(cur_field);
						}
					}
					user_settings_panel.revalidate();
					user_settings_panel.repaint();
				}
	        	
	        });
	        fields_panel.add(b_add);
	        fields_panel.add(b_remove);
	        JScrollPane sp = new JScrollPane(m_html_help);
	        sp.setMinimumSize(new Dimension(300,120));
	        sp.setPreferredSize(new Dimension(400,130));
	        sp.setMaximumSize(new Dimension(600,400));
	        JPanel bottom_panel = new JPanel();
	        bottom_panel.setLayout(new BoxLayout(bottom_panel, BoxLayout.X_AXIS));
	        bottom_panel.add(sp);
	        adv_params_tab.add(top_panel);
	        adv_params_tab.add(bottom_panel);
	        
	        addTab("Advanced", adv_params_tab);
		}
		
		@Override
		public void saveAdditionalSettingsTo(final NodeSettingsWO settings) {
			user_settings_panel.saveAdditionalSettingsTo(settings);
		}
		
		@Override 
		public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] inSpecs) {
			user_settings_panel.loadAdditionalSettingsFrom(settings);
		}
}
