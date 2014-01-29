package au.edu.unimelb.plantcell.cluster.management;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.apis.ApiMetadata;
import org.jclouds.apis.Apis;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.ComputeMetadataIncludingStatus;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.Location;
import org.jclouds.logging.jdk.config.JDKLoggingModule;
import org.jclouds.openstack.keystone.v2_0.KeystoneApi;
import org.jclouds.openstack.keystone.v2_0.domain.Access;
import org.jclouds.openstack.keystone.v2_0.domain.Endpoint;
import org.jclouds.openstack.keystone.v2_0.domain.Service;
import org.jclouds.openstack.keystone.v2_0.domain.Tenant;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.Providers;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.SetCell;
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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;


/**
 * This is the model implementation of ListNodes.
 * Lists the nodes available via the chosen provider
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ListNodesNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("List Cluster");
        
    static final String CFGKEY_PROVIDER = "cloud-provider";
    static final String CFGKEY_IDENTITY = "identity";
    static final String CFGKEY_PASSWD   = "password";
    static final String CFGKEY_ENDPOINT = "endpoint";
    
    // persisted state
    private final SettingsModelString m_provider = new SettingsModelString(CFGKEY_PROVIDER, "");
    private final SettingsModelString m_identity = new SettingsModelString(CFGKEY_IDENTITY, "");
    private final SettingsModelString m_passwd   = new SettingsModelString(CFGKEY_PASSWD, "");
    private final SettingsModelString m_endpoint = new SettingsModelString(CFGKEY_ENDPOINT, "");

    // not persisted state: lazy initialised by getCloudList()
    private static final HashMap<String,Object> name2metadata = new HashMap<String,Object>();
    
    /**
     * Constructor for the node model.
     */
    protected ListNodesNodeModel() {
        this(0, 1);
    }

    protected ListNodesNodeModel(final int in_ports, final int out_ports) {
    	super(in_ports, out_ports);
    }
    
    /**
     * Side-effects name2metadata static member if not already initialised
     * @return
     */
    public static String[] getCloudList() {
    	if (name2metadata.size() > 0)
    		return name2metadata.keySet().toArray(new String[0]);
    				
    	Iterable<ApiMetadata> apis = Apis.all();
    	for (ApiMetadata am : apis) {
    		name2metadata.put(am.getName(), am);
    	}
    	
    	Iterable<ProviderMetadata> providers = Providers.all();
    	for (ProviderMetadata pm : providers) {
    		name2metadata.put(pm.getName(), pm);
    	}
    	
    	if (name2metadata.size() == 0)
    		return getCloudList();		// for some reason (classloading?) need to do this twice at times?
    	
    	ArrayList<String> ret = new ArrayList<String>();
    	ret.addAll(name2metadata.keySet());
    	Collections.sort(ret);
    	return ret.toArray(new String[0]);
    }
    
    /**
     * Return the metadata associated with the specified provider or api into the cloud
     * @param provider_or_api
     * @return null if not found, or an instance of either {@Link ProviderMetadata} or {@Link ApiMetadata}
     */
    public static Object getMetadata(final String provider_or_api) {
    	String[] metadata_objects = getCloudList();
    	if (metadata_objects == null || metadata_objects.length < 1)
    		return null;
    	return name2metadata.get(provider_or_api);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	DataTableSpec outSpec = make_output_spec();
    	MyDataContainer c = new MyDataContainer(exec.createDataContainer(outSpec), "Node"); 
    	
    	logger.info("Attempting to load configuration for provider: "+m_provider.getStringValue());
    	
    	// 1. get compute service instance
    	ContextBuilder             cb = make_context_builder();
    	ComputeServiceContext context = getComputeService(cb);
    	ApiMetadata      api_metadata = cb.getApiMetadata();
    	
    	if (api_metadata == null)
    		throw new InvalidSettingsException("No API available!");
    	
    	api_metadata.getDefaultProperties().list(System.err);
    	
    	Function<Credentials,Access> auth = context.utils().injector().getInstance(Key.get(new TypeLiteral<Function<Credentials,Access>>(){}));
    	Access a = auth.apply(new Credentials.Builder<Credentials>()
    						.identity(m_identity.getStringValue())
    						.credential(m_passwd.getStringValue()).build());
    	
    	logger.info(a);
    	logger.info(" User Name = " + a.getUser().getName());
    	logger.info(" User ID = " + a.getUser().getId());
    	
    	//logger.info(" Tenant Name = " + a.getToken().getTenant().get().getName());
    	//logger.info(" Tenant ID = " + a.getToken().getTenant().get().getId());
    	logger.info(" Token ID = " + a.getToken().getId());
    	logger.info(" Token Expires = " + a.getToken().getExpires());
    	 for (Service service: a) {
    		 logger.info(" Service = " + service.getName());
    		 for (Endpoint e: service) {
    			 logger.info(" Endpoint = " + e.getPublicURL());
    		 }
    	}
    	
    	 
    	ComputeService compute = null;
    	try {
	    	compute = context.getComputeService();
	    	Set<? extends Location> locations = compute.listAssignableLocations();
	    	for (Location l : locations) {
	    		logger.info(l);
	    	}
	    	
	    	Set<? extends Hardware> hardware_configs = compute.listHardwareProfiles();
	    	for (Hardware hw : hardware_configs) {
	    		DataCell[] cells = new DataCell[outSpec.getNumColumns()];
	    		cells[0] = new StringCell(hw.getName());
	    		cells[1] = getStatus(hw);
	    		cells[2] = new StringCell(hw.getType().toString());
	    		cells[3] = new StringCell(hw.getLocation().toString());
	    		c.addRow(cells);
	    	}
	    	
	    	Set<? extends org.jclouds.compute.domain.Image> images = compute.listImages();
	    	for (Image i : images) {
	    		DataCell[] cells = new DataCell[outSpec.getNumColumns()];
	    		if (i.getName() != null)
	    			cells[0] = new StringCell(i.getName());
	    		else
	    			cells[0] = DataType.getMissingCell();
	    		cells[1] = getStatus(i);
	    		cells[2] = new StringCell(i.getType().toString());
	    		cells[3] = new StringCell(i.getLocation().toString());
	    		
	    		c.addRow(cells);
	    	}
	    	
	    	KeystoneApi keystone_service = cb.buildApi(KeystoneApi.class);
	    	if (keystone_service != null) {
	    		Set<? extends Tenant> tenants = keystone_service.getServiceApi().listTenants();
	    		for (Tenant t : tenants) {
	    			DataCell[] cells = new DataCell[outSpec.getNumColumns()];
	    			cells[0] = new StringCell(t.getDescription());
	    			cells[1] = new StringCell(t.getName());
	    			cells[2] = new StringCell("TENANT");
	    			cells[3] = new StringCell(t.getId());
	    			c.addRow(cells);
	    		}
	    	}
	    	
	    	Set<? extends ComputeMetadata> nodes = compute.listNodes();
	    	for (ComputeMetadata cm : nodes) {
	    		DataCell[] cells = new DataCell[outSpec.getNumColumns()];
	    		cells[0] = new StringCell(cm.getName());
	    		cells[1] = getStatus(cm);
	    		cells[2] = new StringCell(cm.getType().toString());
	    		cells[3] = new StringCell(cm.getLocation().toString());
	    		c.addRow(cells);
	    	}
	    	
	    	Set<? extends Location> locs = compute.listAssignableLocations();
	    	for (Location l : locs) {
	    		DataCell[] cells = new DataCell[outSpec.getNumColumns()];
	    		cells[0] = new StringCell(l.getDescription());
	    		List<DataCell> loc_col = new ArrayList<DataCell>();
	    		for (String s : l.getIso3166Codes()) {
	    			loc_col.add(new StringCell(s));
	    		}
	    		cells[1] = loc_col.size() > 0 ? CollectionCellFactory.createSetCell(loc_col) : DataType.getMissingCell();
	    		cells[2] = new StringCell("LOCATION");
	    		cells[3] = new StringCell(l.getId());
	    		c.addRow(cells);
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    		logger.info("Cannot get compute nodes from cloud!", e);
    		throw e;
    	} finally {
    		if (compute != null)
    			compute.getContext().close();
    	}
    	return new BufferedDataTable[] { c.close() };
    }
    
    protected ComputeServiceContext getComputeService(final ContextBuilder context) {
    	String endpoint = null;

    	if (m_endpoint.getStringValue().trim().length() > 0) 
    		endpoint = m_endpoint.getStringValue();
    	logger.info("Using endpoint: "+endpoint+" for provider/API: "+m_provider.getStringValue());
    	return getComputeService(context);
    }
    
    protected ComputeServiceContext getComputeService(final ContextBuilder context, final String endpoint) {
    	return init_builder(context, endpoint, m_identity.getStringValue(), m_passwd.getStringValue()).buildView(ComputeServiceContext.class);
    }
    
    private ContextBuilder init_builder(final ContextBuilder cb, String endpoint, String username,
			String passwd) {
    	Iterable<AbstractModule> modules = ImmutableSet.<AbstractModule> of(new JDKLoggingModule());
    	return cb.endpoint(endpoint)
		.credentials(m_identity.getStringValue(), m_passwd.getStringValue())
		.modules(modules);
	}

	protected ContextBuilder make_context_builder() throws InvalidSettingsException {
    	Object o = getMetadata(m_provider.getStringValue());
    	if (o instanceof ApiMetadata) {
    		return ContextBuilder.newBuilder((ApiMetadata) o);
    	} else if (o instanceof ProviderMetadata) {
    		return ContextBuilder.newBuilder((ProviderMetadata) o);
    	} else {
    		throw new InvalidSettingsException("Unknown/invalid cloud: "+o);
    	}
    }

	/**
     * Returns a set of status flags describing the object or missing cell if there aren't any
     * @param cm
     * @return
     */
    private DataCell getStatus(final ComputeMetadata cm) {
		ArrayList<StringCell> tags = new ArrayList<StringCell>();

    	if (cm != null && cm instanceof Hardware) {
    		Hardware hw = (Hardware) cm;
    		if (hw.getHypervisor() != null)
    			tags.add(new StringCell("hypervisor="+hw.getHypervisor()));
    		tags.add(new StringCell("ram="+hw.getRam()));
    		for (org.jclouds.compute.domain.Volume v : hw.getVolumes()) {
    			String dev = v.getDevice();
    			if (dev == null)
    				dev = v.getId();
    			tags.add(new StringCell("storage="+dev+","+v.getSize()+","+v.getType().toString()));
    		}
    	} else if (cm == null || (cm.getTags().size() < 1 && !(cm instanceof ComputeMetadataIncludingStatus)))
			return DataType.getMissingCell();
		
		for (String tag : cm.getTags()) {
			tags.add(new StringCell(tag));
		}
		if (cm instanceof ComputeMetadataIncludingStatus) {
			tags.add(new StringCell(((ComputeMetadataIncludingStatus)cm).getStatus().toString()));
		}
		return CollectionCellFactory.createSetCell(tags);
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    private DataTableSpec make_output_spec() {
    	DataColumnSpec[] cols = new DataColumnSpec[4];
    	cols[0] = new DataColumnSpecCreator("Node Name", StringCell.TYPE).createSpec();
    	cols[1] = new DataColumnSpecCreator("Status (set)", SetCell.getCollectionType(StringCell.TYPE)).createSpec();
    	cols[2] = new DataColumnSpecCreator("Description", StringCell.TYPE).createSpec();
    	cols[3] = new DataColumnSpecCreator("Location", StringCell.TYPE).createSpec();
    	return new DataTableSpec(cols);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    
        return new DataTableSpec[]{make_output_spec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_provider.saveSettingsTo(settings);
    	m_identity.saveSettingsTo(settings);
    	m_passwd.saveSettingsTo(settings);
    	m_endpoint.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_provider.loadSettingsFrom(settings);
    	m_identity.loadSettingsFrom(settings);
    	m_passwd.loadSettingsFrom(settings);
    	m_endpoint.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_provider.validateSettings(settings);
    	m_identity.validateSettings(settings);
    	m_passwd.validateSettings(settings);
    	m_endpoint.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
    }

}

