package au.edu.unimelb.plantcell.misc.biojava;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "BioJavaProcessor" Node.
 * Analyses the specified data (often using http://www.biojava.org) and produces the result at output
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class BioJavaProcessorNodeDialog extends DefaultNodeSettingsPane {
	
	private final HashMap<String, BioJavaProcessorTask> name2task = new HashMap<String, BioJavaProcessorTask>();

	private final HashSet<String> m_categories = new HashSet<String>();
	private MyColumnSelectionList csp = null;
	private String m_sel_task = null;
	private DataTableSpec m_spec = null;
	private BioJavaProcessorTask[] m_tasks = null;
	
	// widgets which are monitored for their current selection
	private JComboBox category_list = null;
	private JList task_list = null;
	private final JTextPane m_help_label = new JTextPane();
	private String m_sequence_col;
	

	protected BioJavaProcessorNodeDialog() {
        super();
        try {
        	
        List<String> c = getBioJavaTasks();
        csp = new MyColumnSelectionList(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				csp.setSelectedValue(m_sequence_col, false);
			}
        	
        });
        
        JPanel p = (JPanel) this.getTab("Options");
        JPanel child = new JPanel();
        child.setBorder(null);
        child.setLayout(new BorderLayout());
        p.add(child);
        JPanel settings_panel = new JPanel();
        JPanel help_panel     = new JPanel();
        child.setLayout(new BoxLayout(child, BoxLayout.X_AXIS));
        child.add(settings_panel);
        child.add(Box.createHorizontalStrut(5));
        child.add(help_panel);
        settings_panel.setLayout(new BoxLayout(settings_panel, BoxLayout.Y_AXIS));
        help_panel.setLayout(new BorderLayout());
        
        String[] cat = m_categories.toArray(new String[0]);
        Arrays.sort(cat);
        category_list = new JComboBox(cat);
        task_list     = new JList(c.toArray(new String[0]));
        task_list.setVisibleRowCount(6);
        final JLabel no_cols_label = new JLabel("");
        no_cols_label.setForeground(Color.RED);
        
        category_list.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				String cat = (String) category_list.getSelectedItem();
				if (cat == null)
					return;
				boolean set_sel_task = false;
				ArrayList<String> new_tasks = new ArrayList<String>();
				for (BioJavaProcessorTask t : m_tasks) {
					if (t.hasCategory(cat) || cat.equals("All")) {
						for (String s : t.getNames()) {
							new_tasks.add(s);
							if (m_sel_task != null && s.equals(m_sel_task)) {
								set_sel_task = true;
							}
						}
					}
				}
				
				task_list.setListData(new_tasks.toArray(new String[0]));
				if (set_sel_task) {
					task_list.setSelectedValue(m_sel_task, true);
				}
			}
        	
        });
        task_list.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				// columns change as the task changes due to the requirements of the task
				BioJavaProcessorTask found = null;
				String sel_col = (String) task_list.getSelectedValue();
				if (sel_col != null) {
					m_sel_task = sel_col;
				}
				
				found = findTask(sel_col);
				if (found == null)
					return;
				
				// when a given task is selected, we update the help...
				m_help_label.setText("<html><h3>"+found.getCategory()+": "+found.getNames()[0]+
						"</h3><br/><br/>"+found.getHTMLDescription(m_sel_task));
				
				// and update the list of available columns for processing...
				csp.setColumnFilter(found.getColumnFilter());
			}
        	
        });
        
        JScrollPane sp_tasks      = new JScrollPane(task_list);
        sp_tasks.setPreferredSize(new Dimension(300,140));
      
        add_widget(settings_panel, "Analysis category", category_list);
        add_widget(settings_panel, "Analysis to perform", sp_tasks);
        JPanel cols_panel = new JPanel();
        cols_panel.setLayout(new BoxLayout(cols_panel, BoxLayout.Y_AXIS));
        cols_panel.add(no_cols_label);
        cols_panel.add(new JScrollPane(csp));
        add_widget(settings_panel, "Column to process", cols_panel);

        m_help_label.setContentType("text/html");
        help_panel.add(new JScrollPane(m_help_label));
        help_panel.setMinimumSize(new Dimension(200,50));
        help_panel.setPreferredSize(new Dimension(400,120));
        
        } catch (NoClassDefFoundError e) {
        	e.printStackTrace();
			Logger.getAnonymousLogger().info(e.getMessage());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			pw.close();
			
			Logger.getAnonymousLogger().warning(sw.toString());
        }
        this.addTab("Advanced", new JScrollPane(new JPanel()));
    }
    
	private BioJavaProcessorTask findTask(String name) {
		for (BioJavaProcessorTask t : m_tasks) {
			if (t.hasName(name)) {
				return t;
			}
		}
		return null;
	}
	
    private void add_widget(JPanel p, String string, JComponent c) {
    	JPanel child = new JPanel();
    	child.setLayout(new BoxLayout(child, BoxLayout.X_AXIS));
    	child.add(new JLabel(string));
    	child.add(Box.createGlue());
    	child.add(c);
		p.add(child);
		p.add(Box.createRigidArea(new Dimension(3,3)));
	}

    List<String> getBioJavaTasks() {
    	List<String> ret = new ArrayList<String>();
        m_categories.add("All");
        m_tasks = BioJavaProcessorNodeModel.getTasks();
        for (BioJavaProcessorTask tmp : m_tasks) {
        	for (String s : tmp.getNames()) {
        		ret.add(s);
        		name2task.put(s, tmp);
        		m_categories.add(tmp.getCategory());
        	}
        }
        
        Collections.sort(ret);
        return ret;
    }
	@Override
    public void saveAdditionalSettingsTo(NodeSettingsWO settings) {
		Collection<String> cols = csp.getSelectedColumns();
		String selcol = "";
		if (cols.size() > 0) {
			selcol = cols.toArray(new String[0])[0];
		}
    	settings.addString(BioJavaProcessorNodeModel.CFGKEY_SEQUENCE_COL, selcol);
    	String task = (String) task_list.getSelectedValue();
    	if (task == null) 
    		task = "";
    	String category = (String) category_list.getSelectedItem();
    	if (category == null)
    		category = "All";
    	
    	settings.addString(BioJavaProcessorNodeModel.CFGKEY_TASK, task);
    	settings.addString(BioJavaProcessorNodeModel.CFGKEY_CATEGORY, category);
	}
    
    @Override 
    public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) {
    	try {
        	m_spec = specs[0];
        	
        	String default_task = "";
        	if (task_list != null) {
				if (settings.containsKey(BioJavaProcessorNodeModel.CFGKEY_TASK)) {
					default_task = settings.getString(BioJavaProcessorNodeModel.CFGKEY_TASK);
				}
				if (default_task.length() < 1 && task_list.getModel().getSize() > 0)
					default_task = task_list.getModel().getElementAt(0).toString();
				task_list.setSelectedValue(default_task, true);
			}
        	
			m_sequence_col = settings.getString(BioJavaProcessorNodeModel.CFGKEY_SEQUENCE_COL);
			if (csp != null) {
				BioJavaProcessorTask t = findTask(default_task);
				if (t != null)
					csp.setColumnFilter(t.getColumnFilter());
				csp.update(m_spec, m_sequence_col);
				if (csp.getModel().getSize() == 1) {
					csp.setSelectedValue(csp.getModel().getElementAt(0), true);
				} else {
					csp.setSelectedValue(m_sequence_col, true);
				}
			}
			if (category_list != null) {
				String default_value = "All";
				if (settings.containsKey(BioJavaProcessorNodeModel.CFGKEY_CATEGORY)) {
					default_value = settings.getString(BioJavaProcessorNodeModel.CFGKEY_CATEGORY);
				}
				category_list.setSelectedItem(default_value);
			}
			
    	} catch (Exception e) {
			e.printStackTrace();
		} 
    }
}


