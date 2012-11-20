package au.edu.unimelb.plantcell.io.ws.togows;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;


public class EntryReaderNodeDialog extends DefaultNodeSettingsPane {
	private final ArrayList<String> m_databases = new ArrayList<String>();
	private final ArrayList<String> m_fields = new ArrayList<String>();
	private final DialogComponentStringSelection m_dcss;
	private final SettingsModelString m_db = new SettingsModelString(EntryReaderNodeModel.CFGKEY_DATABASE, "uniprot");
	private final SettingsModelString m_url= new SettingsModelString(EntryReaderNodeModel.CFGKEY_URL, "");
	private final DialogComponentStringSelection m_field_list; 
	
	@SuppressWarnings("unchecked")
	public EntryReaderNodeDialog() {
		m_databases.add("uniprot");		// avoid empty-size array exception from KNIME
		m_fields.add("All");
		
		createNewGroup("Which database do you want?");
		this.setHorizontalPlacement(true);
		m_dcss = new DialogComponentStringSelection(m_db, "Available databases", m_databases);

		addDialogComponent(m_dcss);
		final DialogComponentButton refresh_button = new DialogComponentButton("Refresh");
		refresh_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				SettingsModelStringArray dbs = new SettingsModelStringArray(KeywordSearchNodeModel.CFGKEY_DB_LIST, new String[] {""});
				
				try {
					new DatabaseLoader().reload(new URL(m_url.getStringValue()), null, dbs);
					ArrayList<String> tmp = new ArrayList<String>();
					for (String s : dbs.getStringArrayValue()) {
						tmp.add(s);
					}
					m_dcss.replaceListItems(tmp, "uniprot");
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
			
		});
		addDialogComponent(refresh_button);
		
		
		m_field_list = new DialogComponentStringSelection(new SettingsModelString(EntryReaderNodeModel.CFGKEY_FIELD, "All"), "Fields", m_fields);
		m_db.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				String db = m_db.getStringValue();
				m_fields.clear();
				m_fields.add("All");
				try {
					URLConnection conn = new URL(m_url.getStringValue() + db + "?fields").openConnection();
					BufferedReader rdr = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					String line;
					while ((line = rdr.readLine()) != null) {
						if (line.trim().length() > 0) {
							m_fields.add(line.trim());
						}
					}
					rdr.close();		// will invoke close on the input stream ie. URL connection
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					
					m_field_list.replaceListItems(m_fields, "All");
				}
			}
			
		});
		
		addDialogComponent(m_dcss);
		
		this.setHorizontalPlacement(false);
		addDialogComponent(m_field_list);
		addDialogComponent(new DialogComponentColumnNameSelection(
				new SettingsModelString(EntryReaderNodeModel.CFGKEY_ID_COLUMN, "All"), 
				"Get entry IDs from... ", 0, StringValue.class));
	}
	
	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
		try {
			m_url.setStringValue(settings.getString(EntryReaderNodeModel.CFGKEY_URL));

			String[] dbs = settings.getStringArray(EntryReaderNodeModel.CFGKEY_DB_LIST);
			if (dbs.length < 1 || dbs[0].length() == 0) {		// nothing from user config (new node maybe)?
				SettingsModelStringArray tmp = new SettingsModelStringArray(EntryReaderNodeModel.CFGKEY_DB_LIST, dbs);
				new DatabaseLoader().reload(new URL(m_url.getStringValue()), null, tmp);
				dbs = tmp.getStringArrayValue();
			}
			
			m_databases.clear();
			for (String s : dbs) {
				m_databases.add(s);
			}
			
			Collections.sort(m_databases);
			m_dcss.replaceListItems(m_databases, m_databases.get(0));
			m_db.setStringValue(settings.getString(EntryReaderNodeModel.CFGKEY_DATABASE));
		} catch (Exception e) {
			throw new NotConfigurableException(e.getMessage());
		}
	}
	
	@Override
	public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		// not in the UI for now, hardcoded
		settings.addString(EntryReaderNodeModel.CFGKEY_URL, "http://togows.dbcls.jp/entry");
		settings.addStringArray(EntryReaderNodeModel.CFGKEY_DB_LIST, m_databases.toArray(new String[0]));
		settings.addStringArray(EntryReaderNodeModel.CFGKEY_FIELD_LIST, m_fields.toArray(new String[0]));
		settings.addString(EntryReaderNodeModel.CFGKEY_DATABASE, m_db.getStringValue());
	}
}
