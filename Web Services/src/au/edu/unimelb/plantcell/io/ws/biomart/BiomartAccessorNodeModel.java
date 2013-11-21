package au.edu.unimelb.plantcell.io.ws.biomart;
import java.util.HashSet;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.io.ws.biomart.soap.BioMartSoapService;
import au.edu.unimelb.plantcell.io.ws.biomart.soap.Dataset;
import au.edu.unimelb.plantcell.io.ws.biomart.soap.Mart;
import au.edu.unimelb.plantcell.io.ws.biomart.soap.PortalServiceImpl;
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
	public static final String CFGKEY_FILTER        = "biomart-filter";
	public static final String CFGKEY_DB 			= "biomart-database";
	public static final String CFGKEY_DATASET       = "biomart-dataset";		// within chosen database
	public static final String CFGKEY_WHAT          = "biomart-what";
	public static final String CFGKEY_ROWLIMIT      = "row-limit-on-data-fetched";

	private final SettingsModelString m_url   = new SettingsModelString(CFGKEY_URL, "");
	//private final SettingsModelString m_query = new SettingsModelString(CFGKEY_QUERY, "");
	private final SettingsModelString m_db    = new SettingsModelString(CFGKEY_DB, "");
	private final SettingsModelString m_dataset=new SettingsModelString(CFGKEY_DATASET, "");
	private final SettingsModelStringArray m_filter = new SettingsModelStringArray(CFGKEY_FILTER, new String[] {});
	private final SettingsModelStringArray m_what   = new SettingsModelStringArray(CFGKEY_WHAT, new String[] {});
	private final SettingsModelIntegerBounded m_rowlimit = new SettingsModelIntegerBounded(CFGKEY_ROWLIMIT, 1000, 0, Integer.MAX_VALUE);
	
	public BiomartAccessorNodeModel() {
		super(0, 1);
	}
	
	 
	public DataTableSpec make_output_spec(DataTableSpec inSpec) throws InvalidSettingsException {
		String[] items = m_what.getStringArrayValue();
		if (items.length < 1)
			throw new InvalidSettingsException("Must select at least one attribute!");
		
		DataColumnSpec[] cols = new DataColumnSpec[items.length];
		HashSet<String> done = new HashSet<String>();
		for (int i=0; i<cols.length; i++) {
			String item = items[i];
			if (done.contains(item)) {
				int val = 1;
				do {
					item = items[i] + "#"+val;
					val++;
				} while (done.contains(item));
				// fall thru
			}
			cols[i] = new DataColumnSpecCreator(item, StringCell.TYPE).createSpec();
		}
		
		return new DataTableSpec(cols);
	}
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
	
    	BioMartSoapService mart = new BioMartSoapService();
    	PortalServiceImpl  port = mart.getPortalServiceImplPort();
    	DataTableSpec outSpec = make_output_spec(null);
    	
    	String limit = "";
    	if (m_rowlimit.getIntValue() > 0) {
    		limit = "limit=\""+String.valueOf(m_rowlimit.getIntValue())+"\"";
    	}
    	
    	StringBuilder attributes = new StringBuilder();
    	for (String attr : m_what.getStringArrayValue()) {
    		attributes.append("<Attribute name=\""+attr+"\" />");
    	}
    	Mart m = getMart(port, m_db.getStringValue());
    	if (m == null)
    		throw new InvalidSettingsException("Unknown mart: "+m_db.getStringValue()+" - server down?");
    	Dataset ds = getDataset(port, m, m_dataset.getStringValue());
    	if (ds == null)
    		throw new InvalidSettingsException("Unknown dataset: "+m_dataset.getStringValue());
    	
    	String query = String.format("<!DOCTYPE Query>"
    	+"<Query client=\"javaclient\" processor=\"TSV\" "+limit+" header=\"1\" >"
    	+"	<Dataset name=\"%s\" config=\"%s\">"
    	+attributes.toString()
    	+"</Dataset></Query>", ds.getName(), m.getConfig()
    	);
    	
    	logger.info("Accessing biomart database: "+m_db.getStringValue());
    	logger.info("Accessing dataset: "+m_dataset.getStringValue());
    	
    	logger.info(query);
    	String results = port.getResults(query);
    	logger.info(results);
    	
    	MyDataContainer c = new MyDataContainer(exec.createDataContainer(outSpec), "Row");
		return new BufferedDataTable[] { c.close() };
	}
	

	private Mart getMart(final PortalServiceImpl port, String mart_name) {
		List<Mart> marts = port.getMarts(null);
		for (Mart m : marts) {
			if (m.getName().equals(mart_name) || m.getDisplayName().equals(mart_name))
				return m;
		}
		
		return null;
	}

	private Dataset getDataset(final PortalServiceImpl port, final Mart m, String dataset_name) {
		List<Dataset> datasets = port.getDatasets(m.getName());
		for (Dataset ds : datasets) {
			if (ds.getName().equals(dataset_name) || ds.getDisplayName().equals(dataset_name)) {
				return ds;
			}
		}
		
		return null;
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
	            throws InvalidSettingsException {
		return new DataTableSpec[] { make_output_spec(null) };
	}
	
	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_db.saveSettingsTo(settings);
		m_url.saveSettingsTo(settings);
		m_filter.saveSettingsTo(settings);
		m_dataset.saveSettingsTo(settings);
		m_what.saveSettingsTo(settings);
		m_rowlimit.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_db.validateSettings(settings);
		m_url.validateSettings(settings);
		m_filter.validateSettings(settings);
		m_dataset.validateSettings(settings);
		m_what.validateSettings(settings);
		m_rowlimit.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_db.loadSettingsFrom(settings);
		m_url.loadSettingsFrom(settings);
		m_filter.loadSettingsFrom(settings);
		m_dataset.loadSettingsFrom(settings);
		m_what.loadSettingsFrom(settings);
		m_rowlimit.loadSettingsFrom(settings);
	}


	@Override
	public String getStatus(String jobID) throws Exception {
		// NOOP for this node
		return null;
	}
}
