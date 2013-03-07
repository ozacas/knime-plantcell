package au.edu.unimelb.plantcell.annotations;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

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
	private String[] m_default_selection;
	private final TreeModel m_tree = new TreeModel();
	private DataTableSpec m_specs = null;
    private final SettingsModelString sms = new SettingsModelString(Sequence2StringsNodeModel.CFGKEY_SEQUENCE_COL, "");
    private JTree m_items;
    private final JTextPane m_help_label = new JTextPane();
 
	protected Sequence2StringsNodeDialog() {
        super();
      
        sms.addChangeListener(this);
        addDialogComponent(
        		new DialogComponentColumnNameSelection(sms,
        				"Sequence column", 0,  true, false, new ColumnFilter() {

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
        
        JPanel p = ((JPanel) getTab("Options"));
        
        m_items = new JTree(m_tree);
        m_items.setMinimumSize(new Dimension(150,120));
        m_items.setVisibleRowCount(SHOW_N_ROWS);
        m_items.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
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
        p2.setBorder(BorderFactory.createTitledBorder("What to extract? (several can be chosen)"));
        p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));
        p2.add(new JScrollPane(m_items));
        p.add(p2);
        p.add(Box.createRigidArea(new Dimension(6,6)));
        JPanel help_panel = new JPanel();
        help_panel.add(m_help_label);
        m_help_label.setContentType("text/html");
        final JScrollPane help_sp = new JScrollPane(help_panel);
        p.add(help_sp);
        
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
					}
					// fallthru...
				}
				m_help_label.setText("<html><h3>Please select a single task for help.");
				
				// ensure last user selection is always visible (eg. when loading the dialog for the first time)
				if (leaf_selected.size() >= 1) {
					m_items.scrollPathToVisible(leaf_selected.get(0));
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
		m_tree.clear();
		m_tree.add("Common", Sequence2StringsNodeModel.SEQUENCE_ID);
		m_tree.add("Common", Sequence2StringsNodeModel.DESCRIPTION_PRIMARY);
		m_tree.add("Common", Sequence2StringsNodeModel.SEQUENCE_SINGLE_LETTER);
		m_tree.add("Common", Sequence2StringsNodeModel.INPUT_SEQUENCE);
		m_tree.add("Common", Sequence2StringsNodeModel.SEQUENCE_LENGTH);
		
		for (BioJavaProcessorTask t : BioJavaProcessorNodeModel.getTasks()) {
			// this node only supports SequenceCell related tasks
			if (!( t.isCompatibleWith(SequenceCell.TYPE)))
				continue;
			
			// else
			String category = t.getCategory();
			for (String name : t.getNames()) {
				m_tree.add(category, name);
			}
		}
		stateChanged(null);		// get annotation tracks added
		update_selection(m_default_selection);
		m_tree.invalidate();
	}
	
	private void update_selection(final String[] sel) {
		List<TreePath> tp_list = new ArrayList<TreePath>();
		if (sel.length > 0) {
			m_items.clearSelection();
			for (String tmp : sel) {
				TreePath tp = m_tree.getPath(tmp);
				if (tp != null) {
					tp_list.add(tp);
				}
			}
			m_items.addSelectionPaths(tp_list.toArray(new TreePath[0]));
		}
	}
	
	@Override
	public void loadAdditionalSettingsFrom(NodeSettingsRO s, DataTableSpec[] specs) 
					throws NotConfigurableException {
		m_specs = specs[0];
		if (m_items != null && m_items.getSelectionCount() < 1) {
			if (s.containsKey(Sequence2StringsNodeModel.CFGKEY_WANTED)) {
				try {
					m_default_selection = s.getStringArray(Sequence2StringsNodeModel.CFGKEY_WANTED);
					update_selection(m_default_selection);
				} catch (InvalidSettingsException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void saveAdditionalSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
		List<TreePath> selitems = m_tree.paths2leaves(m_items.getSelectionPaths());
		if (selitems == null || selitems.size() < 1) {
			settings.addStringArray(Sequence2StringsNodeModel.CFGKEY_WANTED, "");
		} else {
			String[] sel = new String[selitems.size()];
			int i=0;
			for (TreePath o : selitems) {
				sel[i++] = m_tree.findName(o);
			}
			settings.addStringArray(Sequence2StringsNodeModel.CFGKEY_WANTED, sel);
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (m_specs == null) 
			return;
		
		int col_idx = m_specs.findColumnIndex(sms.getStringValue());
		if (col_idx < 0) {
			return;
		}
		
		DataColumnProperties props = m_specs.getColumnSpec(col_idx).getProperties();
		if (props == null || props.size() < 1) {
			return;
		}
		
		Enumeration<String> it = props.properties();
		
		while (it.hasMoreElements()) {
			String propName = it.nextElement();
			
			if (propName.startsWith(Track.PLANTCELL_TRACK_PREFIX)) {
				propName = propName.substring(Track.PLANTCELL_TRACK_PREFIX.length());
				// must prepend "Track - " as this is used to recognise a user-defined annotation track during execute()
				m_tree.add("Annotation Tracks", "Track - "+propName);
				m_tree.invalidate();		// signal listeners
			}
		}
		m_tree.nodeStructureChanged(new DefaultMutableTreeNode(m_tree.getRoot()));
	}
}

