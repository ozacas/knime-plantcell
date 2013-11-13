package au.edu.unimelb.plantcell.io.ws.biomart;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.io.ws.tmhmm.AbstractWebServiceNodeModel;


/**
 * Accessor for the biomart query engine
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class BiomartAccessorNodeModel extends AbstractWebServiceNodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Biomart Accessor");
    
    // dialog configuration & model settings
	public static final String CFGKEY_URL           = "biomart-home";

	private final SettingsModelString m_url   = new SettingsModelString(CFGKEY_URL, "");

	
	public BiomartAccessorNodeModel() {
		super(0, 1);
	}
	
	 
	public DataTableSpec make_output_spec(DataTableSpec inSpec) throws InvalidSettingsException {
		return null;
	}
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
	
    	
		return new BufferedDataTable[] { };
	}
	

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
	            throws InvalidSettingsException {
		return new DataTableSpec[] { null };
	}
	
	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_url.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_url.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_url.loadSettingsFrom(settings);
	}


	@Override
	public String getStatus(String jobID) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
