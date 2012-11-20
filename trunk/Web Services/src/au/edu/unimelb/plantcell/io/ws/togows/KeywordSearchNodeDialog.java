package au.edu.unimelb.plantcell.io.ws.togows;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;

public class KeywordSearchNodeDialog extends DefaultNodeSettingsPane {
	private final ArrayList<String> m_databases = new ArrayList<String>();
	private final SettingsModelString m_url = new SettingsModelString(KeywordSearchNodeModel.CFGKEY_URL, "http://togows.dbcls.jp/search");
	
	
	private final DialogComponentStringSelection dcss;
	
	public KeywordSearchNodeDialog() {
		m_databases.add("uniprot");		// avoid empty-size array exception from KNIME
		
		createNewGroup("Which database do you want?");
		this.setHorizontalPlacement(true);
		dcss = new DialogComponentStringSelection(
				new SettingsModelString(KeywordSearchNodeModel.CFGKEY_DATABASE, "uniprot"), "Available databases", m_databases);
		addDialogComponent(dcss);
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
					dcss.replaceListItems(tmp, "uniprot");
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
			
		});
		addDialogComponent(refresh_button);
		
		createNewGroup("Search keywords");
		addDialogComponent(new DialogComponentString(new SettingsModelString(KeywordSearchNodeModel.CFGKEY_FIELD, "keywords to search"), "Keywords (space separated)"));
	}
	
	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
		try {
			m_url.setStringValue(settings.getString(KeywordSearchNodeModel.CFGKEY_URL));
			String[] dbs = settings.getStringArray(KeywordSearchNodeModel.CFGKEY_DB_LIST);
			
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
			dcss.replaceListItems(m_databases, m_databases.get(0));
		} catch (Exception e) {
			throw new NotConfigurableException(e.getMessage());
		}
	}
	
	@Override
	public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		// not in the UI for now, hardcoded
		settings.addString(KeywordSearchNodeModel.CFGKEY_URL, m_url.getStringValue());
		settings.addStringArray(KeywordSearchNodeModel.CFGKEY_DB_LIST, m_databases.toArray(new String[0]));
	}
}
