package au.edu.unimelb.plantcell.annotations;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
	
	private final static int SHOW_N_ROWS = 5;
	
	private final static ArrayList<String> m_items = new ArrayList<String>();
	static {
		m_items.add(Sequence2StringsNodeModel.SEQUENCE_ID);
		m_items.add(Sequence2StringsNodeModel.DESCRIPTION_PRIMARY);
		m_items.add(Sequence2StringsNodeModel.SEQUENCE_SINGLE_LETTER);
		m_items.add(Sequence2StringsNodeModel.INPUT_SEQUENCE);
		m_items.add(Sequence2StringsNodeModel.SEQUENCE_LENGTH);
		
		for (String t : BioJavaProcessorNodeModel.getTaskNames()) {
			m_items.add(t);
		}
        Collections.sort(m_items);
	};
	private DataTableSpec m_specs = null;
    private final SettingsModelString sms = new SettingsModelString(Sequence2StringsNodeModel.CFGKEY_SEQUENCE_COL, "");
    private JList items;
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
        
        items = new JList(m_items.toArray());
        items.setMinimumSize(new Dimension(150,60));
        items.setVisibleRowCount(SHOW_N_ROWS);
        items.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JPanel p2 = new JPanel();
        p2.setBorder(BorderFactory.createTitledBorder("What to extract? (several can be chosen)"));
        p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));
        p2.add(new JScrollPane(items));
        p.add(p2);
        JPanel help_panel = new JPanel();
        help_panel.setBorder(BorderFactory.createTitledBorder("Help for current task"));
        help_panel.add(m_help_label);
        m_help_label.setContentType("text/html");
        final JScrollPane help_sp = new JScrollPane(help_panel);
        p.add(help_sp);
        
        // update help when a suitable list selection is made
        items.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				int[] sel = items.getSelectedIndices();
				if (sel.length == 1) {
					String name = items.getSelectedValue().toString();
					BioJavaProcessorTask found = findTask(name);
					if (found != null) {
						m_help_label.setText("<html><h3>"+name+
								"</h3><br/><br/>"+found.getHTMLDescription(name));
						m_help_label.setCaretPosition(0);
						return;
					}
					// fallthru...
				}
				m_help_label.setText("<html>No help available. Select a single task to see help.");
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
	public void loadAdditionalSettingsFrom(NodeSettingsRO s, DataTableSpec[] specs) 
					throws NotConfigurableException {
		m_specs = specs[0];
		stateChanged(null);
		if (items != null && items.getSelectedIndex() < 0) {
			if (s.containsKey(Sequence2StringsNodeModel.CFGKEY_WANTED)) {
				try {
					items.setSelectedIndices(getIndices(s.getStringArray(Sequence2StringsNodeModel.CFGKEY_WANTED)));
				} catch (InvalidSettingsException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private int[] getIndices(String[] sel_items) throws InvalidSettingsException {
		HashMap<String,Integer> item2idx = new HashMap<String,Integer>();
		ListModel lm = items.getModel();
		for (int i=0; i<lm.getSize(); i++) {
			item2idx.put(lm.getElementAt(i).toString(), new Integer(i));
		}
		
		ArrayList<Integer> ret = new ArrayList<Integer>();
		for (String s : sel_items) {
			if (item2idx.containsKey(s)) {
				ret.add(item2idx.get(s));
				// prevent duplicates
				item2idx.remove(s);
			}
		}
		
		if (ret.size() < 1)
			return null;
		
		int[] vec = new int[ret.size()];
		int idx = 0;
		for (Integer i : ret) {
			vec[idx++] = i.intValue();
		}
		return vec;
	}

	@Override
	public void saveAdditionalSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
		Object[] selitems = items.getSelectedValues();
		if (selitems == null || selitems.length < 1) {
			settings.addStringArray(Sequence2StringsNodeModel.CFGKEY_WANTED, "");
		} else {
			String[] sel = new String[selitems.length];
			int i=0;
			for (Object o : selitems) {
				sel[i++] = o.toString();
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
			items.setListData(m_items.toArray(new String[0]));
			return;
		}
		ArrayList<String> new_items = new ArrayList<String>();
		for (String s : m_items) {
			new_items.add(s);
		}
		DataColumnProperties props = m_specs.getColumnSpec(col_idx).getProperties();
		if (props == null || props.size() < 1) {
			items.setListData(new_items.toArray(new String[0]));
			return;
		}
		
		Enumeration<String> it = props.properties();
		while (it.hasMoreElements()) {
			String propName = it.nextElement();
			
			if (propName.startsWith(Track.PLANTCELL_TRACK_PREFIX)) {
				propName = propName.substring(Track.PLANTCELL_TRACK_PREFIX.length());
				new_items.add("Track - "+propName);
			}
		}
		Collections.sort(new_items);
		items.setListData(new_items.toArray(new String[0]));
		if (new_items.size() == 1) {
			items.setSelectedIndex(0);
		}
	}
}

