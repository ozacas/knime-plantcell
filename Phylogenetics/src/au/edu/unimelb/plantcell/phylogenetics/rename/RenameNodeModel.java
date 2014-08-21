package au.edu.unimelb.plantcell.phylogenetics.rename;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.forester.io.parsers.PhylogenyParser;
import org.forester.io.parsers.util.ParserUtils;
import org.forester.io.writers.PhylogenyWriter;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyMethods;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;
import org.forester.util.ForesterUtil;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.io.read.phyloxml.FileTreeViewInterface;


/**
 * This is the model implementation of PhylogenyProcessor.
 * Using the PAL library, as exported from MUSCLE node, this tree takes input data and performs tree construction, bootstrapping and other phylogenetic analyses as configured by the user.
 * 
 * @author Andrew Cassin
 */
public class RenameNodeModel extends NodeModel implements FileTreeViewInterface {
   	private static final NodeLogger logger = NodeLogger.getLogger("Tree Node Rename");
    
    public final static String CFGKEY_INPUT_FILE  = "input-file";
    public final static String CFGKEY_OUTPUT_FILE = "output-file";
    public final static String CFGKEY_OLD_NAME    = "original-name-column";
    public final static String CFGKEY_NEW_NAME    = "new-name-column";
    public final static String CFGKEY_OVERWRITE   = "overwrite?";
        
  
    private final SettingsModelString m_in = new SettingsModelString(CFGKEY_INPUT_FILE, "");
    private final SettingsModelString m_out= new SettingsModelString(CFGKEY_OUTPUT_FILE, "");
    private final SettingsModelString m_original_node_name = new SettingsModelString(CFGKEY_OLD_NAME, "");
    private final SettingsModelString m_new_node_name = new SettingsModelString(CFGKEY_NEW_NAME, "");
    private final SettingsModelBoolean m_overwrite = new SettingsModelBoolean(CFGKEY_OVERWRITE, Boolean.FALSE);
    
    /**
     * Constructor for the node model.
     */
    protected RenameNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	logger.info("Reading tree from "+m_in.getStringValue());
    	File infile  = new File(m_in.getStringValue());
    	File outfile = new File(m_out.getStringValue());
    	if (outfile.exists() && !m_overwrite.getBooleanValue())
    		throw new InvalidSettingsException("Will not overwrite existing: "+outfile.getAbsolutePath());
    	logger.info("Saving decorated tree to "+m_out.getStringValue());
    	PhylogenyParser parser = ParserUtils.createParserDependingOnFileType(infile, true);
    	Phylogeny[] phys = PhylogenyMethods.readPhylogenies(parser, infile);
    	
    	int old_name = inData[0].getSpec().findColumnIndex(m_original_node_name.getStringValue());
    	int new_name = inData[0].getSpec().findColumnIndex(m_new_node_name.getStringValue());
    	if (old_name < 0 || new_name < 0 || old_name != new_name) {
    		throw new InvalidSettingsException("Problem with columns - reconfigure?");
    	}
    	
    	int missed = 0;
    	Map<String,String> old2new = new HashMap<String,String>();
    	for (DataRow r : inData[0]) {
    		DataCell old_cell = r.getCell(old_name);
    		DataCell new_cell = r.getCell(new_name);
    		if (old_cell.isMissing() || new_cell.isMissing()) {
    			missed++;
    		}
    		old2new.put(old_cell.toString(), new_cell.toString());
    	}
    	if (missed > 0) {
    		logger.info("Skipped "+missed+" rows as they have missing values for old & new node names");
    	}
    	
    	for (Phylogeny p  : phys) {
    		PhylogenyNodeIterator it = p.iteratorPreorder();
    		while (it.hasNext()) {
    			PhylogenyNode n = it.next();
    			String old = n.getName();
    			if (old2new.containsKey(old)) {
    				n.setName(old2new.get(old));
    			}
    		}
    	}
    	
    	PhylogenyWriter writer = new PhylogenyWriter();
    	writer.toPhyloXML(phys, 0, outfile, ForesterUtil.LINE_SEPARATOR);
    	
        return new BufferedDataTable[]{inData[0]};
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
        return new DataTableSpec[]{inSpecs[0]};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_in.saveSettingsTo(settings);
    	m_out.saveSettingsTo(settings);
    	m_original_node_name.saveSettingsTo(settings);
    	m_new_node_name.saveSettingsTo(settings);
    	m_overwrite.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) 
            throws InvalidSettingsException {
    	m_in.loadSettingsFrom(settings);
    	m_out.loadSettingsFrom(settings);
    	m_original_node_name.loadSettingsFrom(settings);
    	m_new_node_name.loadSettingsFrom(settings);
    	m_overwrite.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_in.validateSettings(settings);
    	m_out.validateSettings(settings);
    	m_original_node_name.validateSettings(settings);
    	m_new_node_name.validateSettings(settings);
    	m_overwrite.validateSettings(settings);
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

	@Override
	public File getTreeFileForDisplay() {
		return new File(m_out.getStringValue());
	}

}

