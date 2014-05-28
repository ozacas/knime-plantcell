package au.edu.unimelb.plantcell.io.ws.mascot.datfile;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.io.read.mascot.MascotReaderNodeModel;

/**
 * <code>NodeDialog</code> for the "DatFileDownload" Node.
 * Permits downloading of Mascot DAT files via a JAX-WS web service and will load each dat file into the output table as per the mascot reader. The node also saves the DAT files to the user-specified folder.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class DatFileDownloadNodeDialog extends DefaultNodeSettingsPane {
	public static void set_controls(final SettingsModelDoubleBounded ci, final String current_method) {
		assert(ci != null && current_method != null);
		ci.setEnabled(current_method.trim().toLowerCase().startsWith("confident"));
	}
	
	
    /**
     * New pane for configuring the DatFileDownload node.
     */
    protected DatFileDownloadNodeDialog() {
    	final SettingsModelStringArray dat_files = new SettingsModelStringArray(DatFileDownloadNodeModel.CFGKEY_DAT_FILES, new String[] {});
    	final SettingsModelString method = new SettingsModelString(DatFileDownloadNodeModel.CFGKEY_DAT_FILES_SINCE, DatFileDownloadNodeModel.SINCE_METHODS[0]);
    	final SettingsModelString result_type = new SettingsModelString(MascotReaderNodeModel.CFGKEY_RESULTTYPE, MascotReaderNodeModel.DEFAULT_RESULTTYPE);
    	final SettingsModelString user = new SettingsModelString(DatFileDownloadNodeModel.CFGKEY_USERNAME, "");
    	final SettingsModelString passwd = new SettingsModelString(DatFileDownloadNodeModel.CFGKEY_PASSWORD, "");
    	final SettingsModelDoubleBounded ci = new SettingsModelDoubleBounded(MascotReaderNodeModel.CFGKEY_CONFIDENCE, 
					MascotReaderNodeModel.DEFAULT_CONFIDENCE, 0.0, 1.0);
    	final DialogComponentStringListSelection dat_file_list = new DialogComponentStringListSelection(
    			dat_files, "Select the DAT Files to Load...", new ArrayList<String>(), ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, false, 5
    	);
    	
    	set_controls(ci, result_type.getStringValue());
    	
    	// where to save the DAT files to...
    	createNewGroup("Save DAT files to...");
    	addDialogComponent(new DialogComponentFileChooser(new SettingsModelString(DatFileDownloadNodeModel.CFGKEY_SAVETO_FOLDER, ""),
    			"dat-file-folder", 0, true, new String[] {}));
    	
    	createNewGroup("MascotEE settings");
    	final SettingsModelString url = new SettingsModelString(DatFileDownloadNodeModel.CFGKEY_MASCOTEE_URL, DatFileDownloadNodeModel.DEFAULT_MASCOTEE_URL);
    	
    	addDialogComponent(new DialogComponentString(url, "MascotEE url"));
    	DialogComponentButton refresh_button = new DialogComponentButton("Refresh");
    	refresh_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				refresh_dat_files(dat_file_list, method.getStringValue(), url.getStringValue(), user.getStringValue(), passwd.getStringValue());
			}
    		
    	});
    	
    	this.setHorizontalPlacement(true);
    	addDialogComponent(new DialogComponentString(user, "Username"));
    	addDialogComponent(new DialogComponentPasswordField(passwd, "Password"));
    	this.setHorizontalPlacement(false);
    	
    	// which server to run the DAT files from...
    	createNewGroup("Select the dat files to load...");
    	this.setHorizontalPlacement(true);
    	addDialogComponent(new DialogComponentStringSelection(
    			method, "What dat files to show below?", DatFileDownloadNodeModel.SINCE_METHODS));
    	addDialogComponent(refresh_button);
    	this.setHorizontalPlacement(false);
    	addDialogComponent(dat_file_list);
    	
    	createNewTab("Mascot DAT Processing");
    	DialogComponentButtonGroup bg = new DialogComponentButtonGroup(result_type, true, "Report which peptide hits per query?", 
    			MascotReaderNodeModel.RESULT_TYPES);
    	result_type.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				set_controls(ci, result_type.getStringValue());
			}
    		
    	});
    	
        bg.setToolTipText("Which peptide identifications per spectra do you want to see?");
        addDialogComponent(bg);
        
        addDialogComponent(new DialogComponentNumberEdit(ci,"Identity Threshold Confidence", 5));
        
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(MascotReaderNodeModel.CFGKEY_WANT_SPECTRA, true), "Want MS/MS spectra?"));
        
        method.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				refresh_dat_files(dat_file_list, method.getStringValue(), url.getStringValue(), user.getStringValue(), passwd.getStringValue());
			}
        	
        });
    }
    
    /**
     * reload from the current MascotWS server
     * @param m_dat_files
     */
    protected void refresh_dat_files(final DialogComponentStringListSelection list, 
    					final String method, final String mascotws_url, final String user, final String passwd) {
		assert(list != null && user != null && passwd != null);
		Authenticator auth = null;
		if (user.length() > 0) {
			auth = new Authenticator() {
		    		@Override
		    		protected PasswordAuthentication getPasswordAuthentication() {
		    			return new PasswordAuthentication(user, passwd.toCharArray());
		    		}
			};
		}
		Calendar since = makeStartingDateOfInterest(method);
		assert(since != null);
		SimpleDateFormat df = new SimpleDateFormat();
		NodeLogger   logger = NodeLogger.getLogger("Dat File Downloader");
		logger.info("Obtaining list of dat files since: "+df.format(since.getTime()));
		
		try {
			List<String> newItems = DatFileDownloadNodeModel.getDatFilesSince(since, mascotws_url, auth);
			if (newItems.size() == 0) {
				newItems.add("No DAT files available.");
			}
			Collections.sort(newItems);
			list.replaceListItems(newItems, new String[] {});
			logger.info("Sorted and updated "+newItems.size()+" dat files.");
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	private Calendar makeStartingDateOfInterest(final String method) {
		Calendar ret = Calendar.getInstance();
		
		if (method.startsWith("Last 7 days")) {
			ret.add(Calendar.DAY_OF_MONTH, -7);
		} else if (method.startsWith("Last 24 hours")) {
			ret.add(Calendar.HOUR_OF_DAY, -24);
		} else if (method.startsWith("Current month")) {
			ret.set(Calendar.DAY_OF_MONTH, 1);
		} else if (method.startsWith("Current year")) {
			ret.set(Calendar.MONTH, 0);
			ret.set(Calendar.DAY_OF_MONTH, 1);
		} else {
			ret.set(Calendar.YEAR, 1970);
		}
		
		ret.set(Calendar.HOUR_OF_DAY, 0);
		ret.set(Calendar.MINUTE,0);
		ret.set(Calendar.SECOND, 0);
		return ret;
	}

	@Override
    public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
    	// NO-OP reserved for future use
    }
    
    @Override
    public void saveAdditionalSettingsTo(NodeSettingsWO settings) {
    	// since this node doesnt support files but the superclass of the nodemodel implementation does, we
    	// must fake dialog values for those settings...
    	settings.addStringArray(MascotReaderNodeModel.CFGKEY_FILES, new String[0]);
    }
}

