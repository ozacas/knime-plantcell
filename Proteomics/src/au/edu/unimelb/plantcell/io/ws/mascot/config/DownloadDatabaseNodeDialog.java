package au.edu.unimelb.plantcell.io.ws.mascot.config;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.xml.soap.SOAPException;
import javax.xml.ws.Service;

import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.servers.mascotee.endpoints.ConfigService;


/**
 * Node to download mascot sequence databases
 * @author acassin
 *
 */
public class DownloadDatabaseNodeDialog extends ShowConfigNodeDialog {
	private final SettingsModelString db = new SettingsModelString(DownloadDatabaseNodeModel.CFGKEY_MASCOT_DB, "");
	private final String[]     mascot_db = new String[] { "Press refresh to update available databases." };
	private final DialogComponentStringSelection db_list = new DialogComponentStringSelection(db, "Available mascot databases", mascot_db);

	public DownloadDatabaseNodeDialog() {
		super();
		
		createNewGroup("Save downloaded database to FASTA file...");
		SettingsModelString out_file = new SettingsModelString(DownloadDatabaseNodeModel.CFGKEY_OUT, "");
		addDialogComponent(new DialogComponentFileChooser(out_file, "fasta-file", JFileChooser.SAVE_DIALOG, ".fa|.fasta"));
		
		createNewGroup("Database to download...");
		addDialogComponent(db_list);
		DialogComponentButton refresh_button = new DialogComponentButton("Refresh");
		refresh_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				update_available_databases(db_list, getCurrentMascotEEUrl());
			}
			
		});
		addDialogComponent(refresh_button);
	}

	@Override
	public void updateInResponseToURLChange(final String new_url) {
		super.updateInResponseToURLChange(new_url);
		update_available_databases(db_list, new_url);
	}
	
	protected void update_available_databases(final DialogComponentStringSelection db_list, final String mascotee_url) {
		try {
				Service	                 srv = ShowConfigNodeModel.getConfigService(mascotee_url);
				ConfigService  configService = srv.getPort(ConfigService.class);
				String[] available_databases = configService.availableDatabases();
				if (available_databases == null || available_databases.length < 1) {
					available_databases = new String[] { "No available databases." };
				}
				ArrayList<String> db = new ArrayList<String>();
				for (String str : available_databases) {
					db.add(str);
				}
				db_list.replaceListItems(db, null);
		} catch (MalformedURLException|SOAPException e) {
				e.printStackTrace();
		}	
	}
}
