package au.edu.unimelb.plantcell.cluster.management;

import java.util.ArrayList;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
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

/**
 * This is the model implementation of CreateMachine.
 * Node which can create several machines and then run a chef wizard to initialise the nodes with a particular config.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class CreateMachineNodeModel extends ListNodesNodeModel {
    
	public final static String CFGKEY_NUM_MACHINES = "machines-to-create";
	public final static String CFGKEY_CONSTRAINTS  = "system-requirements";
	public final static String CFGKEY_GROUP_NAME   = "machine-group-name";
	public final static String CFGKEY_LOCATION     = "location";
	
	/**
	 * NB: the superclass load/save code is invoked too so these are just the local state
	 */
	private final SettingsModelStringArray    m_constraints = new SettingsModelStringArray(CFGKEY_CONSTRAINTS, new String[] {});
	private final SettingsModelIntegerBounded m_nodes       = new SettingsModelIntegerBounded(CFGKEY_NUM_MACHINES, 1, 1, 100);
	private final SettingsModelString         m_group       = new SettingsModelString(CFGKEY_GROUP_NAME, "MyCluster");
	private final SettingsModelString         m_location    = new SettingsModelString(CFGKEY_LOCATION, "");
	
	 // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Create machines");
    
    /**
     * Constructor for the node model.
     */
    protected CreateMachineNodeModel() {
        super(0,1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	logger.info("Creating "+m_nodes.getIntValue()+" nodes in cloud: "+getEndpoint());
    	
    	ContextBuilder                 cb = make_context_builder();
    	ComputeService            compute = getComputeService(cb, logger);
    	logger.info("Obtained compute service successfully!");
    	
    	Template                 template = createTemplateFromConfiguration(compute.templateBuilder());
    	logger.info("Created machine template successfully!");
    	
    	exec.checkCanceled();
    	logger.info("Creating nodes... (please wait may take a long time)");
    	Set<? extends NodeMetadata> nodes = compute.createNodesInGroup(m_group.getStringValue(), m_nodes.getIntValue(), template);
    	logger.info("Created "+nodes.size()+ "nodes on cloud.");
    	
    	DataTableSpec             outSpec = make_output_spec();
    	MyDataContainer                 c = new MyDataContainer(exec.createDataContainer(outSpec), "Node");
    	
    	exec.checkCanceled();
    	for (NodeMetadata nm : nodes) {
    		DataCell[] cells = new DataCell[outSpec.getNumColumns()];
    		cells[0] = safeStringCell(nm.getHostname());
    		cells[1] = safeStringCell(nm.getId());
    		cells[2] = safeStringCell(nm.getImageId());
    		cells[3] = safeStringCell(nm.getName());
    		cells[4] = safeStringCell(nm.getProviderId());
    		cells[5] = safeStringCell(nm.getHardware().toString());
    		cells[6] = safeStringCell(nm.getType().name());
    		cells[7] = safeStringCell(nm.getBackendStatus());
    		cells[8] = listOfIPAddresses(nm.getPublicAddresses());
    		cells[9] = listOfIPAddresses(nm.getPrivateAddresses());
    		cells[10]= safeStringCell(nm.getOperatingSystem().toString());
    		c.addRow(cells);
    	}
    	
    	return new BufferedDataTable[] { c.close() };
    }
    
    private DataCell safeStringCell(final String s) {
    	if (s == null)
    			return DataType.getMissingCell();
    	return new StringCell(s);
    }
    
    private DataCell listOfIPAddresses(Set<String> addr) {
		if (addr == null || addr.size() < 1)
			return DataType.getMissingCell();
		
		ArrayList<StringCell> ret = new ArrayList<StringCell>();
		for (String address : addr) {
			ret.add(new StringCell(address));
		}
		return CollectionCellFactory.createListCell(ret);
	}

	private Template createTemplateFromConfiguration(final TemplateBuilder tb) {
    	hardware(tb);
    	location(tb);
    	software(tb);
    	return tb.build();
    }
    
    private TemplateBuilder hardware(final TemplateBuilder tb) {
    	// TODO.. FIXME
    	return tb.smallest();
    }
    
    private TemplateBuilder location(final TemplateBuilder tb) {
    	String loc = m_location.getStringValue();
    	if (loc == null || loc.length() < 1) 
    		return tb;
    	return tb.locationId(loc);
    }
    
    private TemplateBuilder software(final TemplateBuilder tb) {
    	TemplateOptions to = new TemplateOptions();
    	to.blockOnComplete(false);
    	to.blockUntilRunning(false);
    	
    	return tb.os64Bit(true).imageDescriptionMatches("Ubuntu").osVersionMatches("1[0-9].[0-1][0-4]");
    }
    
    private DataTableSpec make_output_spec() {
    	DataColumnSpec[] cols = new DataColumnSpec[11];
    	
    	cols[0] = new DataColumnSpecCreator("Hostname", StringCell.TYPE).createSpec();
    	cols[1] = new DataColumnSpecCreator("Node ID", StringCell.TYPE).createSpec();
    	cols[2] = new DataColumnSpecCreator("Image ID", StringCell.TYPE).createSpec();
    	cols[3] = new DataColumnSpecCreator("Node Name", StringCell.TYPE).createSpec();
    	cols[4] = new DataColumnSpecCreator("Provider ID", StringCell.TYPE).createSpec();
    	cols[5] = new DataColumnSpecCreator("Hardware", StringCell.TYPE).createSpec();
    	cols[6] = new DataColumnSpecCreator("Type", StringCell.TYPE).createSpec();
    	cols[7] = new DataColumnSpecCreator("Backend Status", StringCell.TYPE).createSpec();
    	cols[8] = new DataColumnSpecCreator("Public IP Addresses", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
    	cols[9] = new DataColumnSpecCreator("Private IP Addresses", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
    	cols[10]= new DataColumnSpecCreator("Operating System", StringCell.TYPE).createSpec();
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
       	super.saveSettingsTo(settings);
       	m_constraints.saveSettingsTo(settings);
       	m_nodes.saveSettingsTo(settings);
       	m_group.saveSettingsTo(settings);
       	m_location.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_constraints.loadSettingsFrom(settings);
        m_nodes.loadSettingsFrom(settings);
        m_group.loadSettingsFrom(settings);
        m_location.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
        m_constraints.validateSettings(settings);
        m_nodes.validateSettings(settings);
        m_group.validateSettings(settings);
        m_location.validateSettings(settings);
    }

}

