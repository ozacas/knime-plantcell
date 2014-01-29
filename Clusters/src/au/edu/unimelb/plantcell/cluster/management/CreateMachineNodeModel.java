package au.edu.unimelb.plantcell.cluster.management;

import java.util.ArrayList;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * This is the model implementation of CreateMachine.
 * Node which can create several machines and then run a chef wizard to initialise the nodes with a particular config.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class CreateMachineNodeModel extends ListNodesNodeModel {
    
	public final static String CFGKEY_NUM_MACHINES = "machines-to-create";
	public final static String CFGKEY_IMAGE_NAME   = "operating-system-image";
	public final static String CFGKEY_GROUP_NAME   = "machine-group-name";
	public final static String CFGKEY_LOCATION     = "location";
	
	/**
	 * NB: the superclass load/save code is invoked too so these are just the local state
	 */
	private final SettingsModelString         m_image = new SettingsModelString(CFGKEY_IMAGE_NAME, "");
	private final SettingsModelIntegerBounded m_nodes = new SettingsModelIntegerBounded(CFGKEY_NUM_MACHINES, 1, 100, 1);
	private final SettingsModelString         m_group = new SettingsModelString(CFGKEY_GROUP_NAME, "MyCluster");
	private final SettingsModelString         m_location = new SettingsModelString(CFGKEY_LOCATION, "");
	
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

    	ContextBuilder             cb = make_context_builder();
    	ComputeService        compute = getComputeService(cb).getComputeService();
    	Template             template = assignTemplateFromParameters(compute.templateBuilder());
    	Set<? extends NodeMetadata> nodes = compute.createNodesInGroup(m_group.getStringValue(), m_nodes.getIntValue(), template);
    	DataTableSpec             outSpec = make_output_spec();
    	MyDataContainer c = new MyDataContainer(exec.createDataContainer(outSpec), "Node");
    	
    	exec.checkCanceled();
    	for (NodeMetadata nm : nodes) {
    		DataCell[] cells = new DataCell[outSpec.getNumColumns()];
    		cells[0] = new StringCell(nm.getHostname());
    		cells[1] = new StringCell(nm.getId());
    		cells[2] = new StringCell(nm.getImageId());
    		cells[3] = new StringCell(nm.getName());
    		cells[4] = new StringCell(nm.getProviderId());
    		cells[5] = new StringCell(nm.getHardware().toString());
    		cells[6] = new StringCell(nm.getType().name());
    		cells[7] = new StringCell(nm.getBackendStatus());
    		cells[8] = listOfIPAddresses(nm.getPublicAddresses());
    		cells[9] = listOfIPAddresses(nm.getPrivateAddresses());
    		c.addRow(cells);
    	}
    	
    	return new BufferedDataTable[] { c.close() };
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

	private Template assignTemplateFromParameters(final TemplateBuilder tb) {
    	hardware(tb);
    	location(tb);
    	software(tb);
    	return tb.build();
    }
    
    private TemplateBuilder hardware(final TemplateBuilder tb) {
    	// TODO.. FIXME
    	return tb.any();
    }
    
    private TemplateBuilder location(final TemplateBuilder tb) {
    	return tb.locationId(m_location.getStringValue());
    }
    
    private TemplateBuilder software(final TemplateBuilder tb) {
    	return tb.os64Bit(true).osDescriptionMatches("Ubuntu");
    }
    
    private DataTableSpec make_output_spec() {
    	DataColumnSpec[] cols = new DataColumnSpec[10];
    	
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
       	m_image.saveSettingsTo(settings);
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
        m_image.loadSettingsFrom(settings);
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
        m_image.validateSettings(settings);
        m_nodes.validateSettings(settings);
        m_group.validateSettings(settings);
        m_location.validateSettings(settings);
    }

}

