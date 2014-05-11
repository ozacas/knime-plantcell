package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import java.io.File;
import java.io.IOException;

import org.forester.io.parsers.PhylogenyParser;
import org.forester.io.parsers.util.ParserUtils;
import org.forester.io.writers.PhylogenyWriter;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyMethods;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;
import org.forester.util.ForesterUtil;
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

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * This node applys heat information (in the form of pairwise data for OTUs to the branches
 * of the tree) to the chosen input tree. Output is to a phyloxml file. The result is a tree
 * similar to http://www.phycoweb.net/software/TreeGradients/index.html
 * 
 * Optionally, the branch widths for the tree can be set to the number of pairwise measurements available for the heat.
 * 
 * One possible use for this node is the application of Ka/Ks analysis data onto a tree to illustrate regions of positive selection
 * 
 * @author acassin
 *
 */
public class ApplyHeatNodeModel extends NodeModel {

	private final static NodeLogger logger = NodeLogger.getLogger("Heat to Tree");
	
	static public final String CFGKEY_IN_TREE = "input-tree";
	static public final String CFGKEY_OUT_TREE= "output-tree";
	static public final String CFGKEY_A = "OTU-A";	// external leaf node named A
	static public final String CFGKEY_HEAT = "heat-value";		// colour is determined by domain of chosen column
	static public final String CFGKEY_HEAT_BY = "heat-propagation-strategy";
	static public final String CFGKEY_BRANCH_WIDTH_BY = "branch-width-strategy";
	static public final String CFGKEY_OVERWRITE = "overwrite-output-file?";
	
	static public final String[] HEAT_STRATEGY = {  "average of descendant external nodes", 
													"maximum of descendent external nodes", "average of directly connected nodes only",
													"maximum of directly connected nodes only" };
	static public final String[] WIDTH_STRATEGY = { "None (leave as is)", "Number of taxa (with heat) supporting branch" };
													
	/**
	 * persisted configured state
	 */
	private final SettingsModelString m_in = new SettingsModelString(CFGKEY_IN_TREE, "");
	private final SettingsModelString m_out= new SettingsModelString(CFGKEY_OUT_TREE, "");
	private final SettingsModelString m_a  = new SettingsModelString(CFGKEY_A, "");
	private final SettingsModelString m_heat = new SettingsModelString(CFGKEY_HEAT, "");
	private final SettingsModelString m_heat_by = new SettingsModelString(CFGKEY_HEAT_BY, HEAT_STRATEGY[0]);
	private final SettingsModelString m_width_by = new SettingsModelString(CFGKEY_BRANCH_WIDTH_BY, WIDTH_STRATEGY[0]);
	private final SettingsModelBoolean m_overwrite = new SettingsModelBoolean(CFGKEY_OVERWRITE, false);
	
	
	public ApplyHeatNodeModel() {
		super(1,1);
	}

	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
		int a_idx = inData[0].getSpec().findColumnIndex(m_a.getStringValue());
		int heat_idx = inData[0].getSpec().findColumnIndex(m_heat.getStringValue());
		
		logger.info("Reading tree from "+m_in.getStringValue());
    	File infile  = new File(m_in.getStringValue());
    	File outfile = new File(m_out.getStringValue());
    	if (outfile.exists() && !m_overwrite.getBooleanValue())
    		throw new InvalidSettingsException("Will not overwrite existing: "+outfile.getAbsolutePath());
    	logger.info("Saving heatmapped tree to "+m_out.getStringValue());
    	PhylogenyParser parser = ParserUtils.createParserDependingOnFileType(infile, true);
    	Phylogeny[]       phys = PhylogenyMethods.readPhylogenies(parser, infile);
    	
    	// NB: we apply the heat to all trees in the input file    	
    	AbstractHeatModel  hm = makeHeatModel(m_heat_by.getStringValue());
    	AbstractWidthModel wm = makeWidthModel(m_width_by.getStringValue());
    	
    	for (Phylogeny p : phys) {
    		PhylogenyNodeIterator it = p.iteratorExternalForward();
    		hm.start(p, inData[0], a_idx, heat_idx);
    		wm.start(p);
    		while (it.hasNext()) {
    			PhylogenyNode n = it.next();
    			// apply the width and heat models as chosen by the user for each node (internal or tip/external)
    			hm.apply(n);
    			wm.apply(n);
    		}
    		hm.finish(p);
        	wm.finish(p);
    	}
    
    	
    	// TODO: copy input rows into output table
    	MyDataContainer c1 = new MyDataContainer(exec.createDataContainer(inData[0].getSpec()), "Row");
    	for (DataRow r : inData[0]) {
    		c1.addRow(r);
    	}
    	
    	// and save it out...
    	PhylogenyWriter writer = new PhylogenyWriter();
    	writer.toPhyloXML(phys, 0, outfile, ForesterUtil.LINE_SEPARATOR);
    	
		return new BufferedDataTable[] { c1.close() };
	}
	
	private AbstractWidthModel makeWidthModel(final String wantedModel) {
		return new DefaultWidthModel();
	}

	private AbstractHeatModel makeHeatModel(final String wantedModel) throws InvalidSettingsException {
		assert(wantedModel != null);
		if (wantedModel.equalsIgnoreCase("average of descendant external nodes")) {
			AbstractHeatModel hm = new DefaultHeatModel(logger);
			return hm;
		}
		
		// will cause caller to fail
		throw new InvalidSettingsException("Unknown/unsupported heat model: "+wantedModel);
	}

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
    	return new DataTableSpec[] { inSpecs[0] };
    }
    
	@Override
	protected void reset() {
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_in.saveSettingsTo(settings);
		m_out.saveSettingsTo(settings);
		m_a.saveSettingsTo(settings);
		m_heat.saveSettingsTo(settings);
		m_heat_by.saveSettingsTo(settings);
		m_width_by.saveSettingsTo(settings);
		m_overwrite.saveSettingsTo(settings);
	}
	
	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_in.loadSettingsFrom(settings);
		m_out.loadSettingsFrom(settings);
		m_a.loadSettingsFrom(settings);
		m_heat.loadSettingsFrom(settings);
		m_heat_by.loadSettingsFrom(settings);
		m_width_by.loadSettingsFrom(settings);
		m_overwrite.loadSettingsFrom(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_in.validateSettings(settings);
		m_out.validateSettings(settings);
		m_a.validateSettings(settings);
		m_heat.validateSettings(settings);
		m_heat_by.validateSettings(settings);
		m_width_by.validateSettings(settings);
		m_overwrite.validateSettings(settings);
	}

	@Override
	protected void loadInternals(File arg0, ExecutionMonitor arg1)
			throws IOException, CanceledExecutionException {
		// no op
	}

	@Override
	protected void saveInternals(File arg0, ExecutionMonitor arg1)
			throws IOException, CanceledExecutionException {
		// no op
	}

}
