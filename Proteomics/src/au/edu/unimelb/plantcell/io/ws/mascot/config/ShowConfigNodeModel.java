package au.edu.unimelb.plantcell.io.ws.mascot.config;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.ws.Service;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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
 * mainly for debugging, although people might use it to get access to the definition of a particular modification or other parameter. Be
 * careful to ensure that changes to this class are compatible with subclasses.
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
	public final static String DEFAULT_MASCOTEE_SERVICE_URL = "http://mascot.plantcell.unimelb.edu.au:8080/mascotee/ConfigService?wsdl";
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
    	String u = getURL();
    	Service srv = getConfigService(u);
       	if (srv == null) {
       		throw new InvalidSettingsException("Unable to connect to "+m_url.getStringValue());
       	}
        ConfigService configService = srv.getPort(ConfigService.class);
        if (configService == null)
        	throw new Exception("Cannot connect to server!");
        
        MyDataContainer c = new MyDataContainer(exec.createDataContainer(make_output_spec()), "Config");
        exec.checkCanceled();
        
        // 1. dump available databases
        for (String db : configService.availableDatabases()) {
        	DataCell[] cells = getCellsAsMissing(c);
        	cells[0] = new StringCell(u);
        	cells[1] = new StringCell(db);
        	cells[2] = new StringCell("database");
        	cells[3] = getDatabaseRecord(configService, db);
        	c.addRow(cells);
        }
        exec.checkCanceled();
        // 2. dump available instruments
        for (String ins : configService.availableInstruments()) {
        	DataCell[] cells = getCellsAsMissing(c);
        	cells[0] = new StringCell(u);
        	cells[1] = new StringCell(ins);
        	cells[2] = new StringCell("instrument");
        	cells[3] = getInstrumentRecord(configService, ins);
        	c.addRow(cells);
        }
        exec.checkCanceled();
        // 3. dump available enzymes
        for (String cle : configService.availableEnzymes()) {
        	DataCell[] cells = getCellsAsMissing(c);
        	cells[0] = new StringCell(u);
        	cells[1] = new StringCell(cle);
        	cells[2] = new StringCell("enzyme");
        	cells[3] = getEnzymeRecord(configService, cle);
        	c.addRow(cells);
        }
        exec.checkCanceled();
        // 4. modifications
        for (String mod : configService.availableModifications()) {
        	DataCell[] cells = getCellsAsMissing(c);
        	cells[0] = new StringCell(u);
        	cells[1] = new StringCell(mod);
        	cells[2] = new StringCell("modification");
        	cells[3] = getModificationRecord(configService, mod);
        	c.addRow(cells);
        }
        exec.checkCanceled();
        // 5. mascot configuration parameters
        for (String s : configService.availableConfigParameters()) {
        	DataCell[] cells = getCellsAsMissing(c);
        	cells[0] = new StringCell(u);
        	cells[1] = new StringCell(s);
        	cells[2] = new StringCell("Parameter");
        	cells[3] = new StringCell(configService.getParamValue(s));
        	c.addRow(cells);
        }
        
        return new BufferedDataTable[] { c.close() };
    }

	protected String getURL() {
		return m_url.getStringValue();
	}

	private DataCell getModificationRecord(final ConfigService configService, final String mod) throws SOAPException {
		return new StringCell(configService.getDetailedModificationRecord(mod));
	}

	@Override
    public DataTableSpec[] configure(final DataTableSpec[] inSpecs) {
    	return new DataTableSpec[] { make_output_spec() };
    }
    
    private DataCell getEnzymeRecord(final ConfigService configService, final String cle) throws SOAPException {
		return new StringCell(configService.getDetailedEnzymeRecord(cle));
	}

	private DataCell getInstrumentRecord(final ConfigService configService, final String ins) throws SOAPException {
		String[] fragRules = configService.getFragmentationRulesForInstrument(ins);
		StringBuilder sb = new StringBuilder();
    	for (String s : fragRules) {
    		sb.append(s);
    		sb.append('\n');
    	}
    	return new StringCell(sb.toString());
	}

	private DataCell getDatabaseRecord(final ConfigService configService, final String db) throws SOAPException {
		return new StringCell(configService.getDetailedDatabaseRecord(db));
	}

	private DataCell[] getCellsAsMissing(final MyDataContainer c) {
    	 DataCell[] cells = new DataCell[c.getTableSpec().getNumColumns()];
         for (int i=0; i<cells.length; i++) {
         	cells[i] = DataType.getMissingCell();
         }
         return cells;
    }
    
    protected DataTableSpec make_output_spec() {
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

    public static Service getConfigService(final String url) throws MalformedURLException {
    	if (url == null || url.length() < 1) {
    		throw new MalformedURLException("Missing MascotEE URL!");
    	}
    	String u = url;
    	if (u.endsWith("/") || u.endsWith("mascotee")) {
    		u += "ConfigService?wsdl";
    	}
    	URL u2 = new URL(u);
    	logger.info("Connecting to "+u2.toExternalForm());
    	return Service.create(u2, MASCOTEE_CONFIG_NAMESPACE);
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

