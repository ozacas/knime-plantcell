package au.edu.unimelb.plantcell.proteomics.proteowizard.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
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
import au.edu.unimelb.plantcell.servers.msconvertee.endpoints.MSConvert;
import au.edu.unimelb.plantcell.servers.msconvertee.endpoints.MSConvertFeature;

/**
 * This is the model implementation of Proteowizard show configuration node.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ShowConfigNodeModel extends NodeModel {
	private static final QName MSCONVERTEE_QNAME = new QName("http://impl.msconvertee.servers.plantcell.unimelb.edu.au/", "MSConvertImplService");
	 // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Proteowizard Show Config");
  
	static final String CFGKEY_ENDPOINT      = "service-endpoint";
	static final String CFGKEY_USERNAME      = "username";
	static final String CFGKEY_PASSWD        = "password";
	
	private final SettingsModelString  m_endpoint  = new SettingsModelString(CFGKEY_ENDPOINT, "http://localhost:8080/msconvertee/webservices/MSConvertImpl?wsdl");
	private final SettingsModelString  m_username  = new SettingsModelString(CFGKEY_USERNAME, "");
	private final SettingsModelString  m_passwd    = new SettingsModelString(CFGKEY_PASSWD, "");
	
    /**
     * Constructor for the node model.
     */
    protected ShowConfigNodeModel() {
        super(0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	logger.info("Connecting to "+m_endpoint.getStringValue());
    	MyDataContainer c = new MyDataContainer(exec.createDataContainer(make_output_spec()[0]), "Feature");
    	Service         s = Service.create(new URL(m_endpoint.getStringValue()), MSCONVERTEE_QNAME);
    	MSConvert port = s.getPort(MSConvert.class);
    	List<MSConvertFeature> supported_features = port.supportedFeatures();
    	for (MSConvertFeature feat : supported_features) {
    		DataCell[] cells = new DataCell[4];
    		cells[0] = new StringCell(m_endpoint.getStringValue());
    		cells[1] = new StringCell(feat.name());
    		cells[2] = BooleanCell.TRUE;
    		cells[3] = DataType.getMissingCell();
    		c.addRow(cells);
    	}
    	for (MSConvertFeature feat : port.allFeatures()) {
    		// ignore features which have already been added to the output table or should not be seen by the user
    		if (feat == MSConvertFeature.UNSUPPORTED_FEATURE || supported_features.contains(feat)) {
    			continue;
    		}
    		DataCell[] cells = new DataCell[4];
    		cells[0] = new StringCell(m_endpoint.getStringValue());
    		cells[1] = new StringCell(feat.name());
    		cells[2] = BooleanCell.FALSE;
    		cells[3] = DataType.getMissingCell();
    		c.addRow(cells);
    	}
    	logger.info("Obtained MSConvertEE configuration successfully.");
    	return new BufferedDataTable[] {c.close()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return make_output_spec();
    }

    private DataTableSpec[] make_output_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[4];
		cols[0] = new DataColumnSpecCreator("MSConvertEE URL", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Feature", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Available from this server?", BooleanCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("Comments (optional)", StringCell.TYPE).createSpec();
		return new DataTableSpec[] { new DataTableSpec(cols) };
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         m_endpoint.saveSettingsTo(settings);
         m_username.saveSettingsTo(settings);
         m_passwd.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)  throws InvalidSettingsException {
         m_endpoint.loadSettingsFrom(settings);
         m_username.loadSettingsFrom(settings);
         m_passwd.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
         m_endpoint.validateSettings(settings);
         m_username.validateSettings(settings);
         m_passwd.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO: generated method stub
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO: generated method stub
    }

}

