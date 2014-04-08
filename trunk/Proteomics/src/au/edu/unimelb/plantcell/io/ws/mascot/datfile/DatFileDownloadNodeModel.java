package au.edu.unimelb.plantcell.io.ws.mascot.datfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.ws.Service;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.io.read.mascot.MascotReaderNodeModel;
import au.edu.unimelb.plantcell.servers.mascotws.DatFile.DatFileService;

/**
 * This is the model implementation of DatFileDownload.
 * Permits downloading of Mascot DAT files via a JAX-WS web service and will load each dat file into the output table as per the mascot reader. The node also saves the DAT files to the user-specified folder.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class DatFileDownloadNodeModel extends MascotReaderNodeModel {
	private static final NodeLogger logger = NodeLogger.getLogger("Mascot Dat Downloader");
	
    // configuration parameters which the dialog also uses (superclass also has state)
	public final static String CFGKEY_MASCOT_SERVICE_URL = "mascot-service-url";
	public final static String CFGKEY_DAT_FILES_SINCE    = "since-when";
	public final static String CFGKEY_DAT_FILES          = "available-dat-files";
	public final static String CFGKEY_SAVETO_FOLDER      = "save-dat-files-to";
	
	// default values for the dialog
	public final static String DEFAULT_MASCOT_SERVICE_URL = "http://mascot.plantcell.unimelb.edu.au:8080/mascot/DatFileService?wsdl";
	private final static QName MASCOT_SERVICE_NAMESPACE = 
			new QName("http://www.plantcell.unimelb.edu.au/bioinformatics/wsdl", "DatFileService");
	public final static String[] SINCE_METHODS = {
		"Last 24 hours",
		"Last 7 days",
		"Current month",
		"Current year",
		"Since the dawn of time (will take a long time)"
	};
	
	// persisted state within this class (note that superclass state is also persisted!)
	private final SettingsModelString            m_url = new SettingsModelString(CFGKEY_MASCOT_SERVICE_URL, DEFAULT_MASCOT_SERVICE_URL);
	private final SettingsModelString       m_strategy = new SettingsModelString(CFGKEY_DAT_FILES_SINCE, SINCE_METHODS[0]);
	private final SettingsModelStringArray m_dat_files = new SettingsModelStringArray(CFGKEY_DAT_FILES, new String[0]);
	private final SettingsModelString         m_saveto = new SettingsModelString(CFGKEY_SAVETO_FOLDER, "");
	
    /**
     * Constructor for the node model.
     */
    protected DatFileDownloadNodeModel() {
    	// this node has the same outputs and inputs as the superclass, so...
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	Service srv = Service.create(new URL(m_url.getStringValue()), MASCOT_SERVICE_NAMESPACE);

       	if (srv == null)
       		throw new InvalidSettingsException("Unable to connect to "+m_url.getStringValue());
        DatFileService datFileService = srv.getPort(DatFileService.class);
        
        // first retrieve all the desired dat files into the chosen folder
        String[] wanted_dat_files = m_dat_files.getStringArrayValue();
        if (wanted_dat_files.length < 1) {
        	throw new InvalidSettingsException("No dat files chosen for download! Reconfigure...");
        }
        File out = new File(m_saveto.getStringValue());
        if (!out.exists() && out.isDirectory()) {
        	throw new InvalidSettingsException("Output folder, "+out.getAbsolutePath()+" is not an accessible directory!");
        }
        ArrayList<File> downloaded_files = new ArrayList<File>();
        for (String s : wanted_dat_files) {
        	logger.info("Saving mascot dat file: "+s);
        	File   dat_out = new File(out, s.replaceAll("[^A-Z0-9a-z\\.]", "_"));
        	DataHandler dh = datFileService.getDatFile(s);
        	FileOutputStream fos = new FileOutputStream(dat_out);
        	InputStream is = dh.getInputStream();
        	byte[] buf = new byte[128 * 1024];
        	int got;
        	while ((got = is.read(buf, 0, buf.length)) >= 0) {
        		if (got > 0)
        			fos.write(buf, 0, got);
        	}
        	fos.close();
        	is.close();
        	downloaded_files.add(dat_out);
        }
        
        // now that the files are downloaded we need to initialise the superclass with the chosen files...
        super.setFiles(downloaded_files);
        
        // now process the downloaded dat files as per the mascot reader node
        return super.execute(inData, exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	super.saveSettingsTo(settings);
    	m_url.saveSettingsTo(settings);
    	m_strategy.saveSettingsTo(settings);
    	m_dat_files.saveSettingsTo(settings);
    	m_saveto.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	super.loadValidatedSettingsFrom(settings);
    	m_url.loadSettingsFrom(settings);
    	m_strategy.loadSettingsFrom(settings);
    	m_dat_files.loadSettingsFrom(settings);
    	m_saveto.loadSettingsFrom(settings);
    }
   
    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
       super.validateSettings(settings);
       m_url.validateSettings(settings);
       m_strategy.validateSettings(settings);
       m_dat_files.validateSettings(settings);
       m_saveto.validateSettings(settings);
    }

	public static List<String> getDatFilesSince(Calendar since, String url) throws MalformedURLException, InvalidSettingsException, SOAPException {
		assert(url != null && since != null);
		Service srv = Service.create(new URL(url), MASCOT_SERVICE_NAMESPACE);

       	if (srv == null)
       		throw new InvalidSettingsException("Unable to connect to "+url);
        DatFileService datFileService = srv.getPort(DatFileService.class);
         
        StringBuilder str = new StringBuilder();
        str.append(since.get(Calendar.YEAR));
        int month = since.get(Calendar.MONTH)+1;
        if (month < 10)
        	str.append("0"+month);
        else
        	str.append(month);
        int day_of_month = since.get(Calendar.DAY_OF_MONTH);
        if (day_of_month < 10)
        	str.append("0"+day_of_month);
        else
        	str.append(day_of_month);
        String[] s = datFileService.getDatFilesSince(str.toString());
        ArrayList<String> ret = new ArrayList<String>(s.length);
        for (String dat_file : s) {
        	ret.add(dat_file);
        }
        return ret;
	}

}

