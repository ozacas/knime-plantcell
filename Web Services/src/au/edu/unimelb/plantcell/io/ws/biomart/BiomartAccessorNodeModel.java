package au.edu.unimelb.plantcell.io.ws.biomart;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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
import org.osgi.framework.Bundle;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.servers.biomart.Attribute;
import au.edu.unimelb.plantcell.servers.biomart.BioMartSoapService;
import au.edu.unimelb.plantcell.servers.biomart.Dataset;
import au.edu.unimelb.plantcell.servers.biomart.Filter;
import au.edu.unimelb.plantcell.servers.biomart.Mart;
import au.edu.unimelb.plantcell.servers.biomart.PortalServiceImpl;

import com.sun.org.apache.xml.internal.security.utils.Base64;


/**
 * Accessor for the biomart query engine
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class BiomartAccessorNodeModel extends au.edu.unimelb.plantcell.io.ws.tmhmm.AbstractWebServiceNodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Biomart Accessor");
    
    // dialog configuration & model settings
	public static final String CFGKEY_URL           = "biomart-home";
	public static final String CFGKEY_FILTER        = "biomart-filter";
	public static final String CFGKEY_DB 			= "biomart-database";
	public static final String CFGKEY_DATASET       = "biomart-dataset";		// within chosen database
	public static final String CFGKEY_WHAT          = "biomart-what";
	public static final String CFGKEY_ROWLIMIT      = "row-limit-on-data-fetched";
	public static final String CFGKEY_WANTED_FILTER = "user-configured-filters";

	private final SettingsModelString m_url   = new SettingsModelString(CFGKEY_URL, "");
	//private final SettingsModelString m_query = new SettingsModelString(CFGKEY_QUERY, "");
	private final SettingsModelString m_db    = new SettingsModelString(CFGKEY_DB, "");
	private final SettingsModelString m_dataset=new SettingsModelString(CFGKEY_DATASET, "");
	private final SettingsModelStringArray m_filter = new SettingsModelStringArray(CFGKEY_FILTER, new String[] {});
	private final SettingsModelStringArray m_what   = new SettingsModelStringArray(CFGKEY_WHAT, new String[] {});
	private final SettingsModelIntegerBounded m_rowlimit = new SettingsModelIntegerBounded(CFGKEY_ROWLIMIT, 1000, 0, Integer.MAX_VALUE);
	private final SettingsModelStringArray m_wanted_filters = new SettingsModelStringArray(CFGKEY_WANTED_FILTER, new String[] {});
	
	// MRU cache for objects (rather than reissue SOAP calls needlessly). Resetting the node will destroy this state.
	private static List<Mart>    m_last_marts;
	private static List<Dataset> m_last_datasets;
	private static String        m_last_datasets_mart;
	private static List<Filter>  m_mru_filters;
	private static Mart          m_mru_filters_mart;
	private static Dataset       m_mru_filters_ds;
	
	
	public BiomartAccessorNodeModel() {
		super(0, 1);
		m_last_marts = null;
		m_last_datasets = null;
		m_last_datasets_mart = null;		// m_last_datasets is specific to A PARTICULAR MART
		m_mru_filters = null;				// m_mru_filters is specific to a Mart AND Dataset
		m_mru_filters_mart = null;
		m_mru_filters_ds = null;
	}
	
	/**
	 * Construct and return a {@link DataTableSpec} as required for execute. The column names will be adjusted
	 * if the database chosen doesn't guarantee unique names for each attribute according to a KNIME convention
	 * to avoid KNIME throwing a duplicate column name exception.
	 * 
	 * @param inSpec
	 * @return
	 * @throws InvalidSettingsException
	 */
	public DataTableSpec make_output_spec(DataTableSpec inSpec) throws InvalidSettingsException {
		String[] items = m_what.getStringArrayValue();
		if (items.length < 1)
			throw new InvalidSettingsException("Must select at least one attribute!");
		
		DataColumnSpec[] cols = new DataColumnSpec[items.length];
		HashSet<String> done = new HashSet<String>();
		for (int i=0; i<cols.length; i++) {
			String item = items[i];
			if (item.indexOf(':') > 0) {
				item = item.substring(item.indexOf(':')+1);
			}
			if (done.contains(item)) {
				int val = 1;
				do {
					item = items[i] + "#"+val;
					val++;
				} while (done.contains(item));
				// fall thru
			}
			cols[i] = new DataColumnSpecCreator(item, StringCell.TYPE).createSpec();
			done.add(item);
		}
		
		return new DataTableSpec(cols);
	}
	
	/**
	 * Returns a handle to the SOAP service
	 * @return
	 */
	public static BioMartSoapService getService() {
		try {
			Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.io.ws");
  			URL u = FileLocator.find(bundle, new Path("/wsdl/biomart.wsdl"), null);
  			 
  			 // must not call default constructor for local WSDL... so...
  			BioMartSoapService mart = new BioMartSoapService(u,new QName("http://soap.api.biomart.org/", "BioMartSoapService"));
			
			return mart;
		} catch (Exception e) {
			e.printStackTrace();
			return new BioMartSoapService();
		}
	}
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
	
    	PortalServiceImpl  port = getService().getPortalServiceImplPort();
    	DataTableSpec outSpec = make_output_spec(null);
    	
    	String limit = "";
    	if (m_rowlimit.getIntValue() > 0) {
    		limit = "limit=\""+String.valueOf(m_rowlimit.getIntValue())+"\"";
    		logger.warn("Only reporting up to "+m_rowlimit.getIntValue()+" rows of data.");
    	} else {
    		logger.warn("No limit for number of rows: may take a long time!");
    	}
    	
    	StringBuilder attributes = new StringBuilder();
    	for (String attr : m_what.getStringArrayValue()) {
    		if (attr.indexOf(':') > 0) {
    			attr = attr.substring(0, attr.indexOf(':'));
    		}
    		attributes.append("<Attribute name= \""+attr+"\" />\n");
    	}
    	logger.info("Expecting "+m_what.getStringArrayValue().length+ " columns!");
    	
    	int n_filters = 0;
    	StringBuilder filters = new StringBuilder();
    	for (String filter : m_wanted_filters.getStringArrayValue()) {
    		int idx = filter.indexOf('=');
    		if (idx < 0)
    			throw new InvalidSettingsException("Incorrect filter data, got: "+filter);
    		String         key = filter.substring(0, idx);
    		String base64_data = filter.substring(idx+1);
    		
    		Object val = SerializationUtils.deserialize(Base64.decode(base64_data));
    		if (key.length() < 1 || val == null) {
    			logger.warn("No filter value specified for "+key+" -- please reconfigure!");
    			continue;
    		}
    		filters.append(getFilterXML(key, val));
    		n_filters++;
    	}
    	logger.info("Added "+n_filters+ " query filters to biomart query.");
    	
    	Mart m = getMart(port, m_db.getStringValue());
    	if (m == null)
    		throw new InvalidSettingsException("Unknown mart: "+m_db.getStringValue()+" - server down?");
    	Dataset ds = getDataset(port, m, m_dataset.getStringValue());
    	if (ds == null)
    		throw new InvalidSettingsException("Unknown dataset: "+m_dataset.getStringValue());
    	
    	String query = String.format(
    			"<!DOCTYPE Query>"
    	+"<Query virtualSchemaName= \"default\" formatter= \"TSV\" header= \"1\" "+limit+" uniqueRows= \"0\" count = \"\" datasetConfigVersion = \"0.6\" >"
    	+"	<Dataset name= \"%s\" interface= \"default\" >"
    	+attributes.toString()
    	+filters.toString()
    	+"</Dataset></Query>", ds.getName(), m.getConfig()
    	);
    	
    	logger.info("Accessing biomart database: "+m_db.getStringValue());
    	logger.info("Accessing dataset: "+m_dataset.getStringValue());
    	
    	MyDataContainer c = new MyDataContainer(exec.createDataContainer(outSpec), "Row");
    	exec.checkCanceled();
    	boolean warned = false;
    	try {
    		logger.debug(query);
    		String results = port.getResults(query);
    		BufferedReader rdr = new BufferedReader(new StringReader(results));
    		String line;
    		int line_counter = 0;
    		while ((line = rdr.readLine()) != null) {
    			String[] fields = line.split("\\t");
    			if (line_counter == 0) {
    				line_counter++;
    				logger.info("Header line: "+line);
    				continue;
    			}
    			if (fields.length != outSpec.getNumColumns()) {
    				if (!warned) {
    					logger.warn("Expected "+outSpec.getNumColumns()+" columns but got "+fields.length+" - line is: "+line);
    					logger.warn("Adding missing values for missing attributes. Further warnings disabled.");
    				}
    				warned = true;
    			}
    			DataCell[] cells = new DataCell[outSpec.getNumColumns()];
    			for (int i=0; i<cells.length; i++) {
    				if (i < fields.length)
    					cells[i] = new StringCell(fields[i]);
    				else 
    					cells[i] = DataType.getMissingCell();
    			}
    			c.addRow(cells);
    		}
    	} catch (Exception ex) {
    		ex.printStackTrace();
    		throw ex;
    	}
    	
		return new BufferedDataTable[] { c.close() };
	}

	/**
	 * Returns an XML fragment representing the filter. If the value is a collection, it will
	 * trim each element and comma separate them for the value as biomart requires. If the
	 * value is a scalar (single value) then it will be trimmed and then placed into the XML fragment returned. 
	 * Pretty ugly web service api though!
	 * 
	 * @param key
	 * @param val: either Collection<String> or Object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private String getFilterXML(String key, Object val) throws InvalidSettingsException {
		StringBuilder sb = new StringBuilder();
		if (val instanceof Collection) {
			for (String s : (Collection<String>)val) {
				sb.append(s.trim());
				sb.append(",");
			}
			
			// remove trailing , (if any)
			if (sb.length() > 0 && sb.charAt(sb.length()-1) == ',') {
				sb.deleteCharAt(sb.length()-1);
			}
		} else {
			sb.append(val.toString().trim());
		}
		
		if (sb.length() < 1) {
			throw new InvalidSettingsException("Filter "+key+" has no value! Reconfigure!");
		}
		return "<Filter name=\""+key+"\" value=\""+sb.toString()+"\" />\n";
	}


	@Override
	protected void reset() {
		super.reset();
		
		// remove cache for web services
		m_last_marts = null;
		m_last_datasets = null;
		m_last_datasets_mart = null;
		m_mru_filters = null;
		m_mru_filters_mart = null;
		m_mru_filters_ds = null;
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
		m_wanted_filters.saveSettingsTo(settings);
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
		m_wanted_filters.validateSettings(settings);
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
		m_wanted_filters.loadSettingsFrom(settings);
	}


	@Override
	public String getStatus(String jobID) throws Exception {
		// NOOP for this node
		return null;
	}


	public static Mart getMart(final PortalServiceImpl port, final String mart_name) {
		// side-effects MRU cache if needed
		getMarts(port);
		
		for (Mart m : m_last_marts) {
			if (m.getName().equals(mart_name) || m.getDisplayName().equals(mart_name) || m.getGroup().equals(mart_name))
				return m;
		}
		
		return null;
	}

	public static Dataset getDataset(final PortalServiceImpl port, final Mart m, final String dataset_name) {	
		/* MRU cache is side-effected by this call if needed */
		getDatasets(port, m);
		
		for (Dataset ds : m_last_datasets) {
			if (ds.getName().equals(dataset_name) || ds.getDisplayName().equals(dataset_name)) {
				return ds;
			}
		}
		
		return null;
	}

	public static Filter getFilter(final PortalServiceImpl port, final String mart,
			final String dataset, final String filter_name) {
		Mart m = getMart(port, mart);
		if (m == null)
				return null;
		Dataset ds = getDataset(port, m, dataset);
		if (ds == null)
				return null;
		List<Filter> filters = getFilters(port, m, ds);
		for (Filter f : filters) {
			if (f.getName().equals(filter_name) || f.getDisplayName().equals(filter_name) || f.getDescription().equals(filter_name)) {
				return f;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns a list of filters for the specified biomart dataset. These filters are modified from the raw data to
	 * have HTML entities (eg. &gt;) decoded in the getDisplayName() so that they present correctly in the configure dialog.
	 * No other member of the Filter instances are modified.
	 * 
	 * @param port
	 * @param m
	 * @param ds
	 * @return list of available filters sorted by displayname (never null but may be empty)
	 */
	public static List<Filter> getFilters(final PortalServiceImpl port, final Mart m, final Dataset ds) {
		assert(m != null && ds != null);
		
		if (m_mru_filters != null && m_mru_filters_mart.equals(m) && m_mru_filters_ds.equals(ds)) 
			return m_mru_filters;
		else {
			ArrayList<Filter> ret = new ArrayList<Filter>();
			ret.addAll(port.getFilters(ds.getName(), null, null));
			m_mru_filters = ret;
			m_mru_filters_mart = m;
			m_mru_filters_ds = ds;
			fixBadDisplayNames(ret);
			Collections.sort(ret, new Comparator<Filter>() {

				@Override
				public int compare(Filter arg0, Filter arg1) {
					assert(arg0 != null && arg1 != null);
					return arg0.getDisplayName().compareTo(arg1.getDisplayName());
				}
				
			});
			return ret;
		}
	}
	
	private static List<Filter> fixBadDisplayNames(ArrayList<Filter> ret) {
		assert(ret != null);
		for (Filter f : ret) {
			f.setDisplayName(StringEscapeUtils.unescapeHtml(f.getDisplayName()));
		}
		return ret;
	}

	public static List<Dataset> getDatasets(final PortalServiceImpl port, final Mart m) {
		assert(m != null);
		if (m_last_datasets != null && m.getName().equals(m_last_datasets_mart)) {
			return m_last_datasets;
		}
		// else...
		ArrayList<Dataset> ret = new ArrayList<Dataset>();
		for (Dataset ds : port.getDatasets(m.getName())) {
			if (!ds.isIsHidden())
				ret.add(ds);
		}
		
		// update MRU cache
		m_last_datasets = ret;
		m_last_datasets_mart = m.getName();
		
		return ret;
	}
	
	public static Map<String,Mart> getMarts(final PortalServiceImpl port) {
		HashMap<String,Mart> ret = new HashMap<String,Mart>();
		
		if (m_last_marts == null)
			m_last_marts = port.getMarts(null);
		
    	for (Mart m : m_last_marts) {
    		if (!m.isIsHidden()) {
    			ret.put(m.getDisplayName(), m);
    		}
    	}
    	if (ret.size() > 0) {
    		return ret;
    	} else {
    		ret.put("Server not available", null);
    		return ret;
    	}
	}


	public static List<Attribute> getAttributes(final PortalServiceImpl port, final String datasetName, final String martName) {
		Mart m = getMart(port, martName);
		Dataset ds = getDataset(port, m, datasetName);
		if (m != null && ds != null) {
			return port.getAttributes(ds.getName(), m.getConfig(), null, true);
		} else {
			return new ArrayList<Attribute>();
		}
	}
	
	protected static List<String> getAttributesAsString(final PortalServiceImpl port, final String db, final String mart_name) {
		ArrayList<String> ret = new ArrayList<String>();
	
		List<Attribute> attrs = getAttributes(port, db, mart_name);
		if (attrs != null && attrs.size() > 0) {
			for (Attribute a : attrs) {
				ret.add(a.getName()+": "+a.getDisplayName());
			}
		}
		
		if (ret.size() == 0) {
			ret.add("No attributes available!");
		}
		return ret;
	}
	
}
