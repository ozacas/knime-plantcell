package au.edu.unimelb.plantcell.io.ws.mascot.config;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.servers.mascotee.endpoints.ConfigService;

/**
 * This is the model implementation of ShowConfigNodeModel
 * 
 * Dumps a tabular representation of the current mascot config (as recorded by the MascotEE config service) into the output table. Useful
 * mainly for debugging, although people might use it to get access to the definition of a particular modification or other parameter.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ShowConfigNodeModel extends NodeModel {
	private static final NodeLogger logger = NodeLogger.getLogger("Mascot Show Config");
	
    // configuration parameters which the dialog also uses (superclass also has state)
	public final static String CFGKEY_MASCOT_SERVICE_URL = "mascot-service-url";
	public static final String CFGKEY_USERNAME           = "username";
	public static final String CFGKEY_PASSWORD           = "password";

	
	// default values for the dialog
	public final static String DEFAULT_MASCOTEE_SERVICE_URL = "http://mascot.plantcell.unimelb.edu.au:8080/mascot/ConfigService?wsdl";
	private final static QName MASCOTEE_CONFIG_NAMESPACE = 
			new QName("http://www.plantcell.unimelb.edu.au/bioinformatics/wsdl", "ConfigService");
	
	
	
	// persisted state within this class (note that superclass state is also persisted!)
	private final SettingsModelString            m_url = new SettingsModelString(CFGKEY_MASCOT_SERVICE_URL, DEFAULT_MASCOTEE_SERVICE_URL);
	private final SettingsModelString       m_username = new SettingsModelString(CFGKEY_USERNAME, "");
	private final SettingsModelString       m_password = new SettingsModelString(CFGKEY_PASSWORD, "");
	
    /**
     * Constructor for the node model.
     */
    protected ShowConfigNodeModel() {
    	super(0,1);
        m_username.setEnabled(false);
        m_password.setEnabled(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	Service srv = Service.create(new URL(m_url.getStringValue()), MASCOTEE_CONFIG_NAMESPACE);

       	if (srv == null) {
       		throw new InvalidSettingsException("Unable to connect to "+m_url.getStringValue());
       	}
        ConfigService configService = srv.getPort(ConfigService.class);
     
        MyDataContainer c = new MyDataContainer(exec.createDataContainer(make_output_spec()), "Config");
        
        return new BufferedDataTable[] { c.close() };
    }

    private DataTableSpec make_output_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[5];
		cols[0] = new DataColumnSpecCreator("MascotEE URL", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Mascot Parameter Name", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Mascot Parameter Type", StringCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("Mascot Parameter Record", StringCell.TYPE).createSpec();
		cols[4] = new DataColumnSpecCreator("Notes (may be missing)", StringCell.TYPE).createSpec();
		
    	return new DataTableSpec(cols);
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_url.saveSettingsTo(settings);
    	m_username.saveSettingsTo(settings);
    	m_password.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_url.loadSettingsFrom(settings);
    	m_username.loadSettingsFrom(settings);
    	m_password.loadSettingsFrom(settings);
    }
   
    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
       m_url.validateSettings(settings);
       m_username.validateSettings(settings);
       m_password.validateSettings(settings);
    }

    public static Service getMascotService(final String url, final Authenticator auth) throws MalformedURLException {
    	if (auth != null) {
    		Authenticator.setDefault(auth);
    	}
    	return getMascotService(url);
    }
    
    public static Service getMascotService(final String url) throws MalformedURLException {
    	logger.info("Connecting to "+url);
    	return Service.create(new URL(url), MASCOTEE_CONFIG_NAMESPACE);
    }

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
	}

	@Override
	protected void reset() {
	}

}

