package au.edu.unimelb.plantcell.annotations;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.ColumnSelectionPanel;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;

/**
 * <code>NodeDialog</code> for the "Track Filter" Node.
 * Various nodes for analysis of sequence regions * 
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class TrackFilterNodeDialog extends DefaultNodeSettingsPane implements ActionListener {
	private final static int SHOW_N_ROWS = 5;		// how many rows to show in track lists?
	
	private final static String[] operations = new String[] { "A overlaps B", "A completely within B", "B completely within A", "A not overlapping B", "A empty, B not empty" };
	
	private final Map<String,Set<String>> colname2tracknames = new HashMap<String,Set<String>>();
	
	private final ColumnSelectionPanel m_csp = new ColumnSelectionPanel(BorderFactory.createEmptyBorder(), new ColumnFilter() {
	
		@Override
		public boolean includeColumn(DataColumnSpec colSpec) {
			if (!colSpec.getType().isCompatible(SequenceValue.class))
				return false;
			
			DataColumnProperties props = colSpec.getProperties();
			Enumeration<String> it = props.properties();
			
			int n_suitable = 0;
			while (it.hasMoreElements()) {
				String propName = it.nextElement();
				Set<String> set = colname2tracknames.get(colSpec.getName());
				if (set == null) {
					set = new HashSet<String>();
					colname2tracknames.put(colSpec.getName(), set);
				}
				if (propName.startsWith(TrackColumnPropertiesCreator.PLANTCELL_TRACKS)) {
					propName = propName.substring(TrackColumnPropertiesCreator.PLANTCELL_TRACKS.length()+1);
					set.add(propName);
					n_suitable++;
				}
			}
			return (n_suitable >= 2);
		}

		@Override
		public String allFilteredMsg() {
			return "No suitable annotated sequence columns (must have at least two tracks)!";
		}
		
	});
	private DataTableSpec m_spec = null;
	private JList a_tracks, b_tracks;
	
    /**
     * New pane for configuring RegionAnalyzer node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected TrackFilterNodeDialog() {
        super();
        
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        
        // 0. two tracks to compare
        a_tracks  = new JList();
        a_tracks.setVisibleRowCount(SHOW_N_ROWS);
        b_tracks  = new JList();
        b_tracks.setVisibleRowCount(SHOW_N_ROWS);

        JPanel p2 = new JPanel();
        p2.setBorder(BorderFactory.createTitledBorder("Which tracks are to be compared?"));
        p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));
        Font large_label = java.awt.Font.decode(Font.SANS_SERIF+"-14-bold");
        JLabel a_label = new JLabel("A");
        a_label.setFont(large_label);
        JLabel b_label = new JLabel("B");
        b_label.setFont(large_label);
        p2.add(a_label);
        p2.add(new JScrollPane(a_tracks));
        p2.add(Box.createHorizontalStrut(10));
        p2.add(b_label);
        p2.add(new JScrollPane(b_tracks));
        p.add(p2);
        
        // 1. column selection to process
        JPanel columns_panel = new JPanel();
        columns_panel.setBorder(BorderFactory.createTitledBorder("Which annotated sequence column to filter?"));
        
        m_csp.addActionListener(this);
        columns_panel.add(m_csp);
        p.add(columns_panel);
        
       
        ((JPanel)this.getTab("Options")).add(p);
        
        // 3. operations available
        addDialogComponent(new DialogComponentButtonGroup(new SettingsModelString(TrackFilterNodeModel.CFGKEY_OPERATION, operations[0]), true, 
				"Operation to perform?", operations));
    }
    
    @Override
    public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] inSpecs) {
    	m_spec = inSpecs[0];
    	try {
    		init(settings);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

	@Override
	public void saveAdditionalSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
		settings.addString(TrackFilterNodeModel.CFGKEY_TRACK1, a_tracks.getSelectedValue().toString());
		settings.addString(TrackFilterNodeModel.CFGKEY_TRACK2, b_tracks.getSelectedValue().toString());
		settings.addString(TrackFilterNodeModel.CFGKEY_ANNOTATIONS_FROM, m_csp.getSelectedColumn());
	}

	private void init(NodeSettingsRO settings) throws Exception {
		m_csp.update(m_spec, "");
		if (settings != null) {
			m_csp.setSelectedColumn(settings.getString(TrackFilterNodeModel.CFGKEY_ANNOTATIONS_FROM));
			a_tracks.setSelectedValue(settings.getString(TrackFilterNodeModel.CFGKEY_TRACK1), true);
			b_tracks.setSelectedValue(settings.getString(TrackFilterNodeModel.CFGKEY_TRACK2), true);
		}
	}
	
	@Override
	public void onOpen() {
		super.onOpen();
		
		try {
			init(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Responds to a change in the column to process as performed by the user (typically)
	 * @param arg0
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// save currently selected track (will select again if we can)
		Object a_save = a_tracks.getSelectedValue();
		Object b_save = b_tracks.getSelectedValue();
		
		// update the list of tracks available
		String colName = m_csp.getSelectedColumn();
		if (colName != null) {
			Set<String> tracks = colname2tracknames.get(colName);
			String[] vec = tracks.toArray(new String[0]);
			Arrays.sort(vec, 0, vec.length);
			a_tracks.setListData(vec);
			b_tracks.setListData(vec);
			if (a_save != null)
				a_tracks.setSelectedValue(a_save, true);
			if (b_save != null)
				b_tracks.setSelectedValue(b_save, true);
		}
	}
}

