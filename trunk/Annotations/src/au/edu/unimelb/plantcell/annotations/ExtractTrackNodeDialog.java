package au.edu.unimelb.plantcell.annotations;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.BorderFactory;
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
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;

/**
 * <code>NodeDialog</code> for the "Track Extractor" Node.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ExtractTrackNodeDialog extends DefaultNodeSettingsPane implements ChangeListener {
	private final static int SHOW_N_ROWS = 5;		// how many rows to show in track lists?

	private JList tracks;
    private final SettingsModelString m_sms = new SettingsModelString(ExtractTrackNodeModel.CFGKEY_SEQUENCE, "");
    private DataTableSpec m_inspec;
	
  
    protected ExtractTrackNodeDialog() {
        super();
        
        m_sms.addChangeListener(this);
        
        // 1. sequence column (with tracks) to be extracted
        addDialogComponent(new DialogComponentColumnNameSelection(
        		m_sms, "Sequences from... ", 0, true, false, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return colSpec.getType().isCompatible(SequenceValue.class);
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable sequence columns (see Columns 2 Sequence node)!";
					}
        			
        		}));
        
        
        // 2.  track to extract
        JPanel p = ((JPanel)getTab("Options"));
        tracks  = new JList(new String[] {"Track 1", "Track 2"});
        tracks.setMinimumSize(new Dimension(150,60));
        tracks.setVisibleRowCount(SHOW_N_ROWS);
        tracks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      
        JPanel p2 = new JPanel();
        p2.setBorder(BorderFactory.createTitledBorder("Which track to extract?"));
        p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));
        p2.add(tracks);
        p.add(p2);        
    }
    
    @Override
    public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] inSpecs) {
    	m_inspec = inSpecs[0];
    	stateChanged(null);
    	if (tracks != null && tracks.getSelectedIndex() < 0) {
    		if (settings.containsKey(ExtractTrackNodeModel.CFGKEY_TRACK)) {
    			try {
					tracks.setSelectedValue(settings.getString(ExtractTrackNodeModel.CFGKEY_TRACK), true);
				} catch (InvalidSettingsException e) {
					e.printStackTrace();
				}
    		}
    	}
    }

	@Override
	public void saveAdditionalSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
		Object sel = tracks.getSelectedValue();
		if (sel == null) {
			settings.addString(ExtractTrackNodeModel.CFGKEY_TRACK, "");
		} else {
			settings.addString(ExtractTrackNodeModel.CFGKEY_TRACK, sel.toString());
		}
	}


	@Override
	public void stateChanged(ChangeEvent arg0) {
		if (m_inspec == null)
			return;
		int col_idx = m_inspec.findColumnIndex(m_sms.getStringValue());
		if (col_idx < 0) {
			tracks.setListData(new String[] { "" });
			return;
		}
		DataColumnProperties props = m_inspec.getColumnSpec(col_idx).getProperties();
		if (props == null || props.size() < 1) {
			tracks.setListData(new String[] { "" });
			return;
		}
		Enumeration<String> it = props.properties();
		
		ArrayList<String> list = new ArrayList<String>();
		while (it.hasMoreElements()) {
			String propName = it.nextElement();
			
			if (propName.startsWith(Track.PLANTCELL_TRACK_PREFIX)) {
				propName = propName.substring(Track.PLANTCELL_TRACK_PREFIX.length());
				list.add(propName);
			}
		}
		tracks.setListData(list.toArray(new String[0]));
		if (list.size() == 1) {		 // only one track? pre-select it...
			tracks.setSelectedIndex(0);
		}
	}
	
	
}

