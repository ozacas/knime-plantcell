package au.edu.unimelb.plantcell.io.read.phyloxml;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.forester.io.parsers.PhylogenyParser;
import org.forester.io.parsers.util.ParserUtils;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyMethods;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.data.BranchData;
import org.forester.phylogeny.data.Confidence;
import org.forester.phylogeny.data.NodeData;
import org.forester.phylogeny.data.Sequence;
import org.forester.phylogeny.data.Taxonomy;
import org.forester.phylogeny.data.Uri;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.MyDataContainer;


/**
 * Creates a table from the input phylogenetic tree, with support for most, but not all, phyloxml features.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class PhyloXMLReaderNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Phylo Reader");
        
    /** user config keys */
	static final String CFGKEY_INFILE    = "input file";
	static final String CFGKEY_NN_AS_SUPPORT = "internal-node-names-as-support?";
	static final String CFGKEY_MISSING_NN_IS_1 = "empty-node-name-is_1?";
    
	// persisted state
	private final SettingsModelString m_infile = new SettingsModelString(CFGKEY_INFILE, "");
	private final SettingsModelBoolean m_internal_nn_as_support = new SettingsModelBoolean(CFGKEY_NN_AS_SUPPORT, Boolean.FALSE);
	private final SettingsModelBoolean m_treat_missing_nn_as_perfect_support  = new SettingsModelBoolean(CFGKEY_MISSING_NN_IS_1, Boolean.FALSE);
	
	
    /**
     * Constructor for the node model.
     */
    protected PhyloXMLReaderNodeModel() {
        super(0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	
    	logger.info("Reading Tree from: "+m_infile.getStringValue());
    	File            infile = new File(m_infile.getStringValue());
    	PhylogenyParser parser = ParserUtils.createParserDependingOnFileType(infile, true);
    	Phylogeny[]       phys = PhylogenyMethods.readPhylogenies(parser, infile);
    	
    	DataTableSpec outSpec = make_output_spec();
    	MyDataContainer      c = new MyDataContainer(exec.createDataContainer(outSpec), "Node");
    	boolean treat_internal_node_names_as_support = m_internal_nn_as_support.getBooleanValue();
    	boolean treat_empty_as_1 = m_treat_missing_nn_as_perfect_support.getBooleanValue();
    			
    	for (int i=0; i<phys.length; i++) {
    		for (PhylogenyNodeIterator it = phys[i].iteratorPreorder(); it.hasNext(); ) {
    			PhylogenyNode n = it.next();
    			@SuppressWarnings("unused")
				BranchData   bd = n.getBranchData();
    			NodeData     nd = n.getNodeData();
    			
	    		DataCell[] cells = new DataCell[outSpec.getNumColumns()];
	    		for (int j=0; j<cells.length; j++) {
	    			cells[j] = DataType.getMissingCell();
	    		}
	    		
	    		cells[0] = new StringCell(infile.getAbsolutePath());
	    		cells[11]= new IntCell(i+1);
	    		
				double support = getBootstrap(n, treat_internal_node_names_as_support, treat_empty_as_1);
				if (!Double.isNaN(support))
					cells[12]= new DoubleCell(support);
	    		
	    		if (!n.isRoot()) {
	    			cells[10] = new DoubleCell(n.calculateDistanceToRoot());
	    			cells[9]  = new DoubleCell(n.getDistanceToParent());
	    		}
	    		
	    		Taxonomy t = null;
	    		List<Taxonomy> tlist = nd.getTaxonomies();
	    		if (tlist != null && tlist.size() > 0)
	    			t = nd.getTaxonomy();
	    		
	    		if (t != null) {
	    			cells[8]  = new StringCell(t.getCommonName());
	    			String code = t.getTaxonomyCode();
	    			if (code != null && code.trim().length() > 0)
	    				cells[7]  = new StringCell(code);
	 
	    			String url = getImageURL(t.getUris());
	    			
	    			cells[6] = (url != null) ? new StringCell(url.toString()) : DataType.getMissingCell();
	    			if (t.getScientificName() != null)
	    				cells[5] = new StringCell(t.getScientificName());
	    		}
	    		
	    		cells[1] = new StringCell(n.getName());
	    		if (!n.isRoot()) {
	    			cells[2] = new StringCell(n.getParent().getName());
	    		}
	    		
	    		cells[3] = new IntCell(n.getNumberOfDescendants());
	    		List<PhylogenyNode> kids = n.getAllDescendants();
	    		int cnt = 0;
	    		if (kids != null) {
		    		for (PhylogenyNode node : kids) {
		    			if (node.isExternal()) {
		    				cnt++;
		    			}
		    		}
	    		}
	    		cells[4] = new IntCell(cnt);
	    		Sequence seq = getSequence(nd.getSequences());
	    		if (seq != null && seq.getMolecularSequence() != null)
	    			cells[13]= new StringCell(seq.getMolecularSequence());
	    		cells[14] = new StringCell(String.valueOf(n.getId()));
	    		if (!n.isRoot())
	    			cells[15] = new StringCell(String.valueOf(n.getParent().getId()));
	    		c.addRow(cells);
    		}
    	}
    	
        return new BufferedDataTable[] { c.close() };
    }

    private Sequence getSequence(List<Sequence> sequences) {
		if (sequences == null || sequences.size() < 1) {
			return null;
		}
		return sequences.get(0);
	}

	private String getImageURL(List<Uri> uris) {
		if (uris == null || uris.size() < 1)
			return null;
		for (Uri u : uris) {
			if (u.getType().equals("image"))
				return uris.get(0).toString();
		}
		return null;
	}

	private double getBootstrap(final PhylogenyNode n, boolean use_internal_node_names_as_support, boolean treat_empty_as_perfect_support) {
		BranchData bd = n.getBranchData();
		
		if (bd != null && bd.isHasConfidences()) {
			for (Confidence c : bd.getConfidences()) {
				if (c.getType().equals("bootstrap")) {
					return c.getValue();
				}
			}
		} else if (use_internal_node_names_as_support && !n.isExternal()) {
			try {
				String nn = n.getName();
				if (nn != null && nn.length() < 1 && treat_empty_as_perfect_support) {
					return 1.0;
				} else if (nn != null && nn.length() >= 1 && use_internal_node_names_as_support) {
					return Double.valueOf(nn);
				}
				return Double.NaN;
			} catch (NumberFormatException nfe) {
				return Double.NaN;
			}
		}
		return Double.NaN;
	}

	private DataTableSpec make_output_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[16];
		cols[0] = new DataColumnSpecCreator("Input file", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Node name", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Parent node name (missing iff root)", StringCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("Number of descendants (direct)", IntCell.TYPE).createSpec();
		cols[4] = new DataColumnSpecCreator("Number of tip (external) nodes (direct)", IntCell.TYPE).createSpec();
		cols[5] = new DataColumnSpecCreator("Taxonomy: scientific name", StringCell.TYPE).createSpec();
		cols[6] = new DataColumnSpecCreator("Taxonomy: image URL", StringCell.TYPE).createSpec();
		cols[7] = new DataColumnSpecCreator("Taxonomy: code", StringCell.TYPE).createSpec();
		cols[8] = new DataColumnSpecCreator("Taxonomy: common name", StringCell.TYPE).createSpec();
		cols[9] = new DataColumnSpecCreator("Branch length (to parent)", DoubleCell.TYPE).createSpec();
		cols[10]= new DataColumnSpecCreator("Distance from root", DoubleCell.TYPE).createSpec();
		cols[11]= new DataColumnSpecCreator("Tree Number (from 1, iff file has multiple trees)", IntCell.TYPE).createSpec();
		cols[12]= new DataColumnSpecCreator("Bootstrap support value", DoubleCell.TYPE).createSpec();
		cols[13]= new DataColumnSpecCreator("Molecular Sequence", StringCell.TYPE).createSpec();
		cols[14]= new DataColumnSpecCreator("Node ID", StringCell.TYPE).createSpec();
		cols[15]= new DataColumnSpecCreator("Parent Node ID", StringCell.TYPE).createSpec();
		
		return new DataTableSpec(cols);
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
        return new DataTableSpec[]{ make_output_spec() };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_infile.saveSettingsTo(settings);
    	m_internal_nn_as_support.saveSettingsTo(settings);
    	m_treat_missing_nn_as_perfect_support.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_infile.loadSettingsFrom(settings);
    	m_internal_nn_as_support.loadSettingsFrom(settings);
    	m_treat_missing_nn_as_perfect_support.loadSettingsFrom(settings);
    	
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_infile.validateSettings(settings);
    	m_internal_nn_as_support.validateSettings(settings);
    	m_treat_missing_nn_as_perfect_support.validateSettings(settings);
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

