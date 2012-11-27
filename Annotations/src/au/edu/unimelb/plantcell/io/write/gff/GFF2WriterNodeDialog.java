package au.edu.unimelb.plantcell.io.write.gff;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;


/**
 * <code>NodeDialog</code> for the "FastaWriter" Node.
 * Creates a .fasta file with the specified accession, description, and sequence columns
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class GFF2WriterNodeDialog extends DefaultNodeSettingsPane implements ChangeListener {
	private JList tracks;
	private DataTableSpec m_inspec;
	private SettingsModelString m_sms;

    /**
     * New pane for configuring FastaWriter node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    @SuppressWarnings("unchecked")
	protected GFF2WriterNodeDialog() {
        super();
        
        SettingsModelString filename = (SettingsModelString) GFF2WriterNodeModel.make(GFF2WriterNodeModel.CFGKEY_FILE);
        m_sms      = (SettingsModelString) GFF2WriterNodeModel.make(GFF2WriterNodeModel.CFGKEY_SEQ);
        SettingsModelBoolean overwrite= (SettingsModelBoolean) GFF2WriterNodeModel.make(GFF2WriterNodeModel.CFGKEY_OVERWRITE);
        
        addDialogComponent(new DialogComponentFileChooser(filename, "file-history", JFileChooser.SAVE_DIALOG, ".gff3|.gff"));
        addDialogComponent(new DialogComponentBoolean(overwrite, "Overwrite OK?"));
        addDialogComponent(new DialogComponentColumnNameSelection(m_sms, "Features from... ", 0, SequenceValue.class));
    
        // 2.  track to extract
        JPanel p = ((JPanel)getTab("Options"));
        tracks  = new JList(new String[] {"Track 1", "Track 2"});
        tracks.setMinimumSize(new Dimension(150,60));
        tracks.setVisibleRowCount(10);
        tracks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      
        JPanel p2 = new JPanel();
        p2.setBorder(BorderFactory.createTitledBorder("Which track to save?"));
        p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));
        p2.add(tracks);
        p.add(p2);  
    }

    @Override
    public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] inSpecs) {
    	m_inspec = inSpecs[0];
    	stateChanged(null);
    	if (tracks != null && tracks.getSelectedIndex() < 0) {
    		if (settings.containsKey(GFF2WriterNodeModel.CFGKEY_TRACK)) {
    			try {
					tracks.setSelectedValue(settings.getString(GFF2WriterNodeModel.CFGKEY_TRACK), true);
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
			settings.addString(GFF2WriterNodeModel.CFGKEY_TRACK, "");
		} else {
			settings.addString(GFF2WriterNodeModel.CFGKEY_TRACK, sel.toString());
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
				propName = propName.substring(Track.PLANTCELL_TRACK_PREFIX.length()+1);
				list.add(propName);
			}
		}
		tracks.setListData(list.toArray(new String[0]));
		if (list.size() == 1) {		 // only one track? pre-select it...
			tracks.setSelectedIndex(0);
		}
	}
	
}

