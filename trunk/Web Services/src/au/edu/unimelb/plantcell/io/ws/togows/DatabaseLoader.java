package au.edu.unimelb.plantcell.io.ws.togows;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

public class DatabaseLoader {
	public DatabaseLoader() {
	}
	
	/**
	 * Attempts to contain the TogoWS server at the specified URL and update the list of 
	 * databases. The current list, if available, is set into <code>ret</code>. The return list
	 * of databases is not modified if any error occurs (eg. network problems)
	 * 
	 * @param u
	 * @param l may be <code>null</code> if you dont want to log anything
	 * @param ret
	 */
	public void reload(URL u, NodeLogger l, SettingsModelStringArray ret) {
		BufferedReader brdr = null;
		try {
			if (l != null) {
				l.info("Opening connection to "+u);
			}
			URLConnection     conn = u.openConnection();
			BufferedInputStream is = new BufferedInputStream(conn.getInputStream());
			brdr = new BufferedReader(new InputStreamReader(is));
			String line;
			ArrayList<String> list = new ArrayList<String>();
			int count = 0;
			while ((line = brdr.readLine()) != null) {
				String[] names = line.trim().split("\\s+");
				list.add(names[0]);
				count++;
			}
			Collections.sort(list);
			ret.setStringArrayValue(list.toArray(new String[0]));
	
			if (l != null) {
				l.info("TogoWS server has "+count+" databases.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (brdr != null) {
					brdr.close();
				}
			} catch (Exception e) {
			}
		}
	}
}
