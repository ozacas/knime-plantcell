package au.edu.unimelb.plantcell.annotations;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;

/**
 * <code>NodeDialog</code> for the "Delete Track" Node. * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ChangeTrackNodeDialog extends DefaultNodeSettingsPane implements ChangeListener {
	private final JList   m_tracks = new JList();
	private DataTableSpec m_inspec = null;
	private String m_default_track = null;
    private final SettingsModelString col = new SettingsModelString(ChangeTrackNodeModel.CFGKEY_ANNOTATIONS_FROM, "");
    private final SettingsModelString op  = new SettingsModelString(ChangeTrackNodeModel.CFGKEY_OPERATION, ChangeTrackNodeModel.OPERATIONS[0]);
    private final SettingsModelString newname = new SettingsModelString(ChangeTrackNodeModel.CFGKEY_NEWNAME, "enter new name here");
    
    protected ChangeTrackNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(col, 
        		"Sequences from... ", 0, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return (colSpec != null && 
								colSpec.getType().isCompatible(SequenceValue.class));
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable sequence columns to process!";
					}
        			
        		}));
        col.addChangeListener(this);
        
        addDialogComponent(new DialogComponentButtonGroup(
        		op, 
        		false, "Operation to perform?", ChangeTrackNodeModel.OPERATIONS
        		));
        op.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				String s = op.getStringValue().trim().toLowerCase();
				newname.setEnabled(s.startsWith("rename"));
			}
        	
        });
        
        addDialogComponent(new DialogComponentString(newname, "Change track name to..."));
        
        JPanel jp = new JPanel();
        jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
        jp.add(m_tracks);
        m_tracks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_tracks.setVisibleRowCount(8);
        
        addTab("Available Tracks", jp);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (m_inspec == null)	// cant display tracks if we cant get the columns...
			return;
		int col_idx = m_inspec.findColumnIndex(col.getStringValue());
		if (col_idx < 0)
			return;
		DataColumnProperties dcp = m_inspec.getColumnSpec(col_idx).getProperties();
		Enumeration<String> props = dcp.properties();
		ArrayList<String> vec = new ArrayList<String>();
		while (props.hasMoreElements()) {
			String s = props.nextElement();
			if (s.startsWith(TrackColumnPropertiesCreator.PLANTCELL_TRACKS)) {
				vec.add(s.substring(TrackColumnPropertiesCreator.PLANTCELL_TRACKS.length()+1));
			}
		}
		m_tracks.setListData(vec.toArray(new String[0]));
		if (vec.size() > 0) {
			if (m_default_track != null) 
				m_tracks.setSelectedValue(m_default_track, true);
			else {
				m_tracks.setSelectedIndex(0);
			}
		}
	}
	
	@Override 
	public void onOpen() {
		stateChanged(null);
		String s = op.getStringValue().trim().toLowerCase();
		newname.setEnabled(s.startsWith("rename"));
	}
	
	@Override 
	public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] inSpecs) {
		m_inspec = inSpecs[0];
		try {
			m_default_track = settings.getString(ChangeTrackNodeModel.CFGKEY_TRACKS);
		} catch (InvalidSettingsException e) {
			m_default_track = null;
			e.printStackTrace();
		}
	}
	
	@Override 
	public void saveAdditionalSettingsTo(NodeSettingsWO settings) {
		settings.addString(ChangeTrackNodeModel.CFGKEY_TRACKS, m_tracks.getSelectedValue().toString());
	}
}

