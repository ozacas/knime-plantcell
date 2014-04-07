package au.edu.unimelb.plantcell.io.ws.mascot.datfile;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.xml.soap.SOAPException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
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

    /**
     * New pane for configuring the DatFileDownload node.
     */
    protected DatFileDownloadNodeDialog() {
    	final SettingsModelStringArray dat_files = new SettingsModelStringArray(DatFileDownloadNodeModel.CFGKEY_DAT_FILES, new String[0]);
    	final SettingsModelString method = new SettingsModelString(DatFileDownloadNodeModel.CFGKEY_DAT_FILES_SINCE, DatFileDownloadNodeModel.SINCE_METHODS[0]);
    	
    	final DialogComponentStringListSelection dat_file_list = new DialogComponentStringListSelection(
    			dat_files, ""
    			);
    	
    	// where to save the DAT files to...
    	createNewGroup("Save DAT files to...");
    	addDialogComponent(new DialogComponentFileChooser(new SettingsModelString(DatFileDownloadNodeModel.CFGKEY_SAVETO_FOLDER, ""),
    			"dat-file-folder", 0, true, new String[] {}));
    	
    	createNewGroup("Mascot Service");
    	final SettingsModelString url = new SettingsModelString(DatFileDownloadNodeModel.CFGKEY_MASCOT_SERVICE_URL, DatFileDownloadNodeModel.DEFAULT_MASCOT_SERVICE_URL);
    	
    	addDialogComponent(new DialogComponentString(url, "URL for MascotWS"));
    	DialogComponentButton refresh_button = new DialogComponentButton("Refresh");
    	refresh_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				refresh_dat_files(dat_file_list, method.getStringValue(), url.getStringValue());
			}
    		
    	});
    	addDialogComponent(refresh_button);
    	
    	// which server to run the DAT files from...
    	createNewGroup("Select the dat files to load...");
    	addDialogComponent(new DialogComponentStringSelection(
    			method, "What dat files to show below?", DatFileDownloadNodeModel.SINCE_METHODS));
    	
    	createNewTab("Mascot DAT Processing");
    }
    
    /**
     * reload from the current MascotWS server
     * @param m_dat_files
     */
    protected void refresh_dat_files(final DialogComponentStringListSelection list, final String method, final String mascotws_url) {
		assert(list != null);
		Calendar since = makeStartingDateOfInterest(method);
		assert(since != null);
		
		
		List<String> newItems;
		try {
			newItems = DatFileDownloadNodeModel.getDatFilesSince(since, mascotws_url);
			list.replaceListItems(newItems, new String[] {});
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	private Calendar makeStartingDateOfInterest(final String method) {
		Calendar ret = Calendar.getInstance();
		
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

