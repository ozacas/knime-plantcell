package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;

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
import au.edu.unimelb.plantcell.io.read.phyloxml.FileTreeViewInterface;

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
public class ApplyHeatNodeModel extends NodeModel implements FileTreeViewInterface {

	private final static NodeLogger logger = NodeLogger.getLogger("Heat to Tree");
	
	static public final String CFGKEY_IN_TREE = "input-tree";
	static public final String CFGKEY_OUT_TREE= "output-tree";
	static public final String CFGKEY_A       = "OTU-A";	// external leaf node named A
	static public final String CFGKEY_HEAT    = "heat-value";		// colour is determined by domain of chosen column
	static public final String CFGKEY_PROPAGATION_FN = "heat-propagation-strategy";
	static public final String CFGKEY_WIDTH_FN = "branch-width-strategy";
	static public final String CFGKEY_OVERWRITE = "overwrite-output-file?";
	static public final String CFGKEY_METHOD    = "calc-colour-method";
	
	// order is important: add new methods to the END of the array
	static public final String[] DEFAULT_METHODS = new String[] { "Scale to column maximum (blue..red)", "By row colour", "Apply bootstrap values from input (blue..red)" };
	static public final String[] HEAT_STRATEGY = {  "average of child OTU's",
													"maximum of child OTU's", 
													"minimum of child OTU's",
													"median of child OTU's",
													"average of directly connected nodes only",
													"maximum of directly connected nodes only", 
													"median of directly connected nodes only", 
													"minimum of directly connected nodes only" };
	static public final String[] WIDTH_STRATEGY = { "None (leave as is)", "Number of taxa (with heat) supporting branch", "% of taxa (with heat) supporting branch" };
													
	/**
	 * persisted configured state
	 */
	private final SettingsModelString m_in = new SettingsModelString(CFGKEY_IN_TREE, "");
	private final SettingsModelString m_out= new SettingsModelString(CFGKEY_OUT_TREE, "");
	private final SettingsModelString m_a  = new SettingsModelString(CFGKEY_A, "");
	private final SettingsModelString m_heat = new SettingsModelString(CFGKEY_HEAT, "");
	private final SettingsModelString m_heat_by = new SettingsModelString(CFGKEY_PROPAGATION_FN, HEAT_STRATEGY[0]);
	private final SettingsModelString m_width_by = new SettingsModelString(CFGKEY_WIDTH_FN, WIDTH_STRATEGY[0]);
	private final SettingsModelBoolean m_overwrite = new SettingsModelBoolean(CFGKEY_OVERWRITE, false);
	private final SettingsModelString m_colour_method = new SettingsModelString(CFGKEY_METHOD, DEFAULT_METHODS[0]);
	
	
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
    	PhylogenyParser parser = ParserUtils.createParserDependingOnFileType(infile, true);
    	Phylogeny[]       phys = PhylogenyMethods.readPhylogenies(parser, infile);
    	
    	try {
	    	// NB: we apply the heat to all trees in the input file    	
	    	AbstractHeatModel  hm = makeHeatModel(m_heat_by.getStringValue());
	    	AbstractWidthModel wm = makeWidthModel(m_width_by.getStringValue());
	    	
	    	for (Phylogeny p : phys) {
	    		PhylogenyNodeIterator it = p.iteratorExternalForward();
	    		hm.start(p, inData[0], a_idx, heat_idx);
	    		wm.start(p, hm);
	    		while (it.hasNext()) {
	    			PhylogenyNode n = it.next();
	    			// apply the width and heat models as chosen by the user for each node (internal or tip/external)
	    			hm.apply(n);
	    			wm.apply(n);
	    		}
	    		hm.finish(p);
	        	wm.finish(p);
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    		throw e;
    	}
    
       	MyDataContainer c1 = new MyDataContainer(exec.createDataContainer(inData[0].getSpec()), "Row");
    	for (DataRow r : inData[0]) {
    		c1.addRow(r);
    	}
    	
    	// and save it out...
    	logger.info("Saving heatmapped tree to "+m_out.getStringValue());
    	PhylogenyWriter writer = new PhylogenyWriter();
    	writer.toPhyloXML(phys, 0, outfile, ForesterUtil.LINE_SEPARATOR);
		return new BufferedDataTable[] { c1.close() };
	}
	
	private AbstractWidthModel makeWidthModel(final String wantedModel) throws InvalidSettingsException {
		String meth = m_width_by.getStringValue();
		if (meth.startsWith("None")) {
			return new DefaultWidthModel(logger);
		} else if (meth.startsWith("Number")) {
			return new CountOfHotTaxaWidthModel(logger);
		} else if (meth.startsWith("%")) {
			return new CountOfHotTaxaWidthModel(logger, true);
		}
		throw new InvalidSettingsException("Unknown/unsupport method: "+meth);
	}

	private AbstractHeatModel makeHeatModel(final String wantedModel) throws InvalidSettingsException {
		assert(wantedModel != null);
		boolean use_bootstrap = m_colour_method.getStringValue().startsWith("Apply bootstrap");
		boolean is_numeric = m_colour_method.getStringValue().equals(ApplyHeatNodeModel.DEFAULT_METHODS[0]) || use_bootstrap;
		
		ColourManager cm = null;
		if (is_numeric) {
			cm = new ColourGradient(logger, Color.RED, Color.BLUE);
		} else {
			cm = new RowColourManager();
		}
		AbstractHeatModel hm = use_bootstrap ? new BootstrapHeatModel(logger, cm) : new DefaultHeatModel(logger, cm);
		String wm = wantedModel.toLowerCase();
		HeatModerator moderator = makeHeatModerator(wm);
		hm.setHeatModerator(moderator);
		
		if (wm.endsWith("otu's")) {
			ModerationSelector ms = new ModerationSelector() {

				@Override
				public List<PhylogenyNode> select(PhylogenyNode n) {
					return (n.isExternal()) ? null : n.getAllExternalDescendants();
				}
				
			};
			hm.setModerationSelector(ms);
			return hm;
		} else if (wm.endsWith("connected nodes only")) {
			ModerationSelector ms = new ModerationSelector() {

				@Override
				public List<PhylogenyNode> select(PhylogenyNode n) {
					return (n.isExternal()) ? null : n.getDescendants();
				}
				
			};
			hm.setModerationSelector(ms);
			return hm;
		}
		
		// will cause caller to fail
		throw new InvalidSettingsException("Unknown/unsupported heat model: "+wantedModel);
	}

    private HeatModerator makeHeatModerator(String wanted_model_lowercased) throws InvalidSettingsException {
		if (wanted_model_lowercased.startsWith("average")) {
			return new HeatModerator() {

				@Override
				public double moderate(List<Double> l) {
					return average(l);
				}
				
			};
		} else if (wanted_model_lowercased.startsWith("maximum")) {
			return new HeatModerator() {

				@Override
				public double moderate(List<Double> l) {
					return maximum(l);
				}
				
			};
		} else if (wanted_model_lowercased.startsWith("minimum")) {
			return new HeatModerator() {

				@Override
				public double moderate(List<Double> l) {
					return minimum(l);
				}
				
			};
		} else if (wanted_model_lowercased.startsWith("median")) {
			return new HeatModerator() {

				@Override
				public double moderate(List<Double> l) {
					return median(l);
				}
				
			};
		}
		
		throw new InvalidSettingsException("Unknown heat moderation model: "+wanted_model_lowercased);
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
		m_colour_method.saveSettingsTo(settings);
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
		m_colour_method.loadSettingsFrom(settings);
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
		m_colour_method.validateSettings(settings);
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

	@Override
	public File getTreeFileForDisplay() {
		return new File(m_out.getStringValue());
	}

}
