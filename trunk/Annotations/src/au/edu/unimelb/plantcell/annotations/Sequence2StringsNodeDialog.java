package au.edu.unimelb.plantcell.annotations;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.biojava.tasks.BioJavaProcessorTask;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.misc.biojava.BioJavaProcessorNodeModel;

/**
 * Convert a sequence to tabular format based on user-chosen items of interest
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class Sequence2StringsNodeDialog extends DefaultNodeSettingsPane implements ChangeListener {
	/**
	 * How many rows to show in the tree view
	 */
	private final static int SHOW_N_ROWS = 10;
	private final TreeModel m_tree = new TreeModel();
	private DataTableSpec m_specs = null;
    private final SettingsModelString sms = new SettingsModelString(Sequence2StringsNodeModel.CFGKEY_SEQUENCE_COL, "");
    private JTree m_items;
    private JList<String> m_sel_items;
    private final JTextPane m_help_label = new JTextPane();
 
	protected Sequence2StringsNodeDialog() {
        super();
      
        sms.addChangeListener(this);
        addDialogComponent(
        		new DialogComponentColumnNameSelection(sms,
        				"Column to analyze", 0,  true, false, new ColumnFilter() {

							@Override
							public boolean includeColumn(DataColumnSpec colSpec) {
								if (colSpec.getType().isCompatible(SequenceValue.class))
									return true;
								return false;
							}

							@Override
							public String allFilteredMsg() {
								return "No suitable Biological sequence column to use!";
							}
        			
        		}));
        
        JPanel options_panel =  ((JPanel) getTab("Options"));
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        options_panel.add(p);
        
        m_items = new JTree(m_tree);
        m_items.setMinimumSize(new Dimension(150,120));
        m_items.setVisibleRowCount(SHOW_N_ROWS);
        m_items.setRootVisible(false);
        m_items.setCellRenderer(new DefaultTreeCellRenderer() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 5858855517017002635L;
        	
			public Icon getLeafIcon() {
				// no icons for the leaves
				return null;
			}
        });
        JPanel p2 = new JPanel();
        p2.setBorder(BorderFactory.createTitledBorder("Available methods"));
        p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));
        p2.add(new JScrollPane(m_items));
        JPanel button_panel = new JPanel();
        button_panel.setLayout(new BoxLayout(button_panel, BoxLayout.Y_AXIS));
        m_sel_items = new JList<String>(new DefaultListModel<String>());

        JButton button_add = new JButton("Add >>");
        button_add.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				String name = m_tree.findName(m_items.getSelectionPath());
				if (name != null) {
					DefaultListModel<String> lm = (DefaultListModel<String>) m_sel_items.getModel();
					if (lm.indexOf(name) < 0)
						lm.addElement(name);
				}
			}
        	
        });
        JButton button_rm  = new JButton("<< Remove");
        button_rm.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int idx = m_sel_items.getSelectedIndex();
				if (idx >= 0) {
					DefaultListModel<String> lm = (DefaultListModel<String>) m_sel_items.getModel();
					lm.removeElementAt(idx);
				}
			}
        	
        });
        
        button_panel.add(button_add);
        button_panel.add(Box.createRigidArea(new Dimension(0, 10)));
        button_panel.add(button_rm);
  
        JPanel selected_items_panel = new JPanel();
        selected_items_panel.setBorder(BorderFactory.createTitledBorder("Methods to execute (ie. add columns)"));
        selected_items_panel.setLayout(new BoxLayout(selected_items_panel, BoxLayout.X_AXIS));
        selected_items_panel.add(new JScrollPane(m_sel_items));
       
        p.add(p2);
        p.add(button_panel);
        p.add(selected_items_panel);
        
        JPanel help_panel = new JPanel();
        help_panel.add(m_help_label);
        m_help_label.setContentType("text/html");
        m_help_label.setEditable(false);
               
        final JScrollPane help_sp = new JScrollPane(help_panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        options_panel.add(help_sp);
        
        // update help when a suitable list selection is made
        m_items.addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent arg0) {
				TreePath[] sel = m_items.getSelectionPaths();
				List<TreePath> leaf_selected = m_tree.paths2leaves(sel);
				if (leaf_selected.size() == 1) {
					String name = m_tree.findName(leaf_selected.get(0));
					BioJavaProcessorTask t = findTask(name);
					if (t != null) {
						m_help_label.setText("<html><h3>"+name+
								"</h3><br/><br/>"+t.getHTMLDescription(name));
						m_help_label.setCaretPosition(0);
						return;
					} else {
						m_help_label.setText("<html><h3>No help available for "+name+".");
					}
					// fallthru...
				} else {
					m_help_label.setText("<html><h3>Please select a single task on the left for help.");
				}
			}
        	
        });
    }
	 
	private BioJavaProcessorTask findTask(String name) {
		for (BioJavaProcessorTask t : BioJavaProcessorNodeModel.getTasks()) {
			if (t.hasName(name)) {
				return t;
			}
		}
		return null;
	}
	
	@Override
	public void onOpen() {
		stateChanged(null);
	}

	
	@Override
	public void loadAdditionalSettingsFrom(NodeSettingsRO s, DataTableSpec[] specs) 
					throws NotConfigurableException {
		m_specs = specs[0];
		
		if (s.containsKey(Sequence2StringsNodeModel.CFGKEY_WANTED)) {
			try {
				String[] sel = s.getStringArray(Sequence2StringsNodeModel.CFGKEY_WANTED);
				DefaultListModel<String> mdl = ((DefaultListModel<String>)m_sel_items.getModel());
				mdl.removeAllElements();
				for (String item : sel) {
					mdl.addElement(item);
				}
			} catch (InvalidSettingsException e) {
				e.printStackTrace();
			}
		} else {
			DefaultListModel<String> mdl = ((DefaultListModel<String>)m_sel_items.getModel());
			mdl.addElement(Sequence2StringsNodeModel.SEQUENCE_ID);
			mdl.addElement(Sequence2StringsNodeModel.SEQUENCE_LENGTH);
		}
	}

	@Override
	public void saveAdditionalSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
		ArrayList<String> items = new ArrayList<String>();
		for (int i=0; i<m_sel_items.getModel().getSize(); i++) {
			items.add(m_sel_items.getModel().getElementAt(i));
		}
		
		settings.addStringArray(Sequence2StringsNodeModel.CFGKEY_WANTED, items.toArray(new String[0]));
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (m_specs == null) 
			return;
		
		m_tree.clear();
		m_tree.add("Common", Sequence2StringsNodeModel.SEQUENCE_ID);
		m_tree.add("Common", Sequence2StringsNodeModel.DESCRIPTION_PRIMARY);
		m_tree.add("Common", Sequence2StringsNodeModel.SEQUENCE_SINGLE_LETTER);
		m_tree.add("Common", Sequence2StringsNodeModel.INPUT_SEQUENCE);
		m_tree.add("Common", Sequence2StringsNodeModel.SEQUENCE_LENGTH);
		
		for (BioJavaProcessorTask t : BioJavaProcessorNodeModel.getTasks()) {
			// this node only supports SequenceCell related tasks
			if (!(t.isCompatibleWith(SequenceCell.TYPE)))
				continue;
			
			// else
			String category = t.getCategory();
			for (String name : t.getNames()) {
				m_tree.add(category, name);
			}
		}
		
		int col_idx = m_specs.findColumnIndex(sms.getStringValue());
		if (col_idx >= 0) {
			DataColumnProperties props = m_specs.getColumnSpec(col_idx).getProperties();
			if (props != null && props.size() >= 1) {
				Enumeration<String> it = props.properties();
				while (it.hasMoreElements()) {
					String propName = it.nextElement();
					
					if (propName.startsWith(Track.PLANTCELL_TRACK_PREFIX)) {
						propName = propName.substring(Track.PLANTCELL_TRACK_PREFIX.length());
						// must prepend "Track - " as this is used to recognise a user-defined annotation track during execute()
						m_tree.add("Annotation Tracks", "Track - "+propName);
					}
				}
			}
		}
		m_tree.invalidate();		// signal listeners
	}
}

