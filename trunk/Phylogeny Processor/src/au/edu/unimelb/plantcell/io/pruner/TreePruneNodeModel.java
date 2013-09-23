package au.edu.unimelb.plantcell.io.pruner;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forester.io.parsers.PhylogenyParser;
import org.forester.io.parsers.util.ParserUtils;
import org.forester.io.writers.PhylogenyWriter;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyMethods;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;
import org.forester.util.ForesterUtil;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.BooleanCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelOptionalString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;


/**
 * Creates a phyloxml document from the input data, decorated with data from the input table.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class TreePruneNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Phylo Tree Prune");
        
    /** user config keys */
	static final String CFGKEY_INFILE    = "input file";
	static final String CFGKEY_OUTFILE   = "output phyloxml file";
    static final String CFGKEY_OVERWRITE = "overwrite";
	static final String CFGKEY_SPECIES   = "species-column";
	static final String CFGKEY_TAXA          = "taxa-column";
	static final String CFGKEY_TAXA_REGEXP   = "taxa-regexp";
	static final String CFGKEY_STRATEGY      = "pruning-strategy";
    
	static final String[] PRUNING_STRATEGYS = new String[] { "Same species, 100% support, same parent, 100% identity (shortest pruned)", "Remove polytomies", "Prune polytomies to binary" };
	
	// persisted state
	private final SettingsModelString m_infile = new SettingsModelString(CFGKEY_INFILE, "");
	private final SettingsModelOptionalString m_outfile= new SettingsModelOptionalString(CFGKEY_OUTFILE, "", true);
	private final SettingsModelBoolean m_overwrite = new SettingsModelBoolean(CFGKEY_OVERWRITE, Boolean.FALSE);
	private final SettingsModelString m_taxa    = new SettingsModelString(CFGKEY_TAXA, "");
	private final SettingsModelString m_species = new SettingsModelString(CFGKEY_SPECIES, "");			// scientific name assumed for now
	private final SettingsModelString m_taxa_regexp   = new SettingsModelString(CFGKEY_TAXA_REGEXP, "(.*)");
	private final SettingsModelString m_strategy      = new SettingsModelString(CFGKEY_STRATEGY, PRUNING_STRATEGYS[0]);
   
	// not persisted -- used during execute()
	private Pattern m_taxa_pattern;
	
    /**
     * Constructor for the node model.
     */
    protected TreePruneNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	
    	int seq_idx = inData[0].getSpec().findColumnIndex(m_taxa.getStringValue());
    	if (seq_idx < 0)
    		throw new InvalidSettingsException("Cannot find taxa column: "+m_taxa.getStringValue()+" - reconfigure?");
    	
    	File infile = new File(m_infile.getStringValue());
    	PhylogenyParser parser = ParserUtils.createParserDependingOnFileType(infile, true);
    	Phylogeny[] phys = PhylogenyMethods.readPhylogenies(parser, infile);
    	logger.info("Loaded "+phys.length+" trees from "+infile.getAbsolutePath()+" for pruning.");
    	
    	
    	// 1. create taxa map
    	m_taxa_pattern = Pattern.compile(m_taxa_regexp.getStringValue());
    	
    	int initial_capacity = (int) (inData[0].getRowCount() * 1.25);
    	if (initial_capacity > 1024 * 1024) 
    		initial_capacity = 1024 * 1024;
    	final Map<String,SequenceValue> taxa_map = new HashMap<String,SequenceValue>(initial_capacity);
    	final Map<String,String> species_map = new HashMap<String,String>(initial_capacity);
    	final Map<String,String> rowid2taxa  = new HashMap<String,String>(initial_capacity);
    	for (DataRow r : inData[0]) {
    		DataCell seq_cell = r.getCell(seq_idx);
    		if (seq_cell == null || seq_cell.isMissing() || !(seq_cell instanceof SequenceValue))
    			continue;
    		SequenceValue sv = (SequenceValue) seq_cell;
    		String taxa = getTaxa(sv.getID());
    		taxa_map.put(taxa, sv);
    		rowid2taxa.put(r.getKey().getString(), taxa);
    	}
    	
    	// 2. apply pruning strategy to each tree
    	final PruningStrategy ps = make_pruning_strategy(m_strategy.getStringValue(), species_map);
    	final Set<String> node_names = new HashSet<String>(initial_capacity);

    	for (int i=0; i<phys.length; i++) {
    		Phylogeny p = phys[i];
    		
    		// HACK TODO FIXME: we only prune the first tree in the file for now...
    		// we do this because the output table cant store the results of multiple prunes... does anyone want this?
    		if (i == 0) {
	    		logger.info("Identifying nodes in tree: "+p.getName());
	    		for (PhylogenyNodeIterator it = p.iteratorPreorder(); it.hasNext(); ) {
	    			PhylogenyNode n = it.next();
	    			if (n.isExternal() && taxa_map.containsKey(n.getName())) {
	    				node_names.add(n.getName());
	    			}
	    		}
	    		
	    		logger.info("Pruning tree: "+p.getName());
	    		ps.execute(p, taxa_map);
	    		phys[i] = p;
    		}
    		
    	}
    	
    	// 2a. save pruned tree (if state permits) 
    	if (m_outfile.getStringValue().length() > 0) {
    		File out = new File(m_outfile.getStringValue());
    		if (out.exists() && out.isFile() && !m_overwrite.getBooleanValue())
    			throw new InvalidSettingsException("Will not overwrite existing file: "+out.getAbsolutePath());
    		PhylogenyWriter writer = new PhylogenyWriter();
    		// NB: all trees get saved!
        	writer.toPhyloXML(phys, 0, out, ForesterUtil.LINE_SEPARATOR);
    	}
    	
    	// 3. create output table
    	CellFactory pruned_cf = new AbstractCellFactory(make_output_spec()) {
    		
			@Override
			public DataCell[] getCells(DataRow row) {
				String       taxa = rowid2taxa.get(row.getKey().getString());
				SequenceValue  sv = taxa_map.get(taxa);
				boolean  is_found = (sv != null) && node_names.contains(taxa);
				if (is_found) {
					return new DataCell[] { ps.acceptTaxa(sv) ? BooleanCell.FALSE : BooleanCell.TRUE, 
										DataType.getMissingCell(), 
										is_found ? BooleanCell.TRUE : BooleanCell.FALSE };
				} else {
					return new DataCell[] { BooleanCell.FALSE, DataType.getMissingCell(), BooleanCell.FALSE };
				}
			}
    		
    	};
    	ColumnRearranger cr = new ColumnRearranger(inData[0].getSpec());
    	cr.append(pruned_cf);
    	BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], cr, exec);
    	
    	return new BufferedDataTable[] {out};
    }

    private PruningStrategy make_pruning_strategy(final String strat, final Map<String,String> species_map) throws InvalidSettingsException {
    	if (strat.equals(PRUNING_STRATEGYS[0])) {
    		return new PruneShortestIdentity(100, 100, species_map);
    	} else if (strat.equals(PRUNING_STRATEGYS[1])) {
    		return new PrunePolytomic();
    	} else if (strat.equals(PRUNING_STRATEGYS[2])) {
    		return new PrunePolytomic(true);
    	} else {
    		throw new InvalidSettingsException("Unknown pruning strategy: "+strat);
    	}
	}

	private DataColumnSpec[] make_output_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[3];
		cols[0] = new DataColumnSpecCreator("Removed from tree?", BooleanCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Reason for pruning", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Found in input tree?", BooleanCell.TYPE).createSpec();
		return cols;
	}

	private String getTaxa(final String taxa) {
		assert(taxa != null);
		
		Matcher m = m_taxa_pattern.matcher(taxa);
		if (m.find()) {
			if (m.groupCount() >= 1)
				return m.group(1);
			// FIXME: warn about no parentheses in regexp?
			return taxa;
		} else {
			return null;
		}
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
        return new DataTableSpec[]{ new DataTableSpec(inSpecs[0], new DataTableSpec(make_output_spec())) };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_infile.saveSettingsTo(settings);
    	m_outfile.saveSettingsTo(settings);
    	m_overwrite.saveSettingsTo(settings);
    	m_species.saveSettingsTo(settings);
    	m_taxa.saveSettingsTo(settings);
    	m_taxa_regexp.saveSettingsTo(settings);
    	m_strategy.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_infile.loadSettingsFrom(settings);
    	m_outfile.loadSettingsFrom(settings);
    	m_overwrite.loadSettingsFrom(settings);
    	m_species.loadSettingsFrom(settings);
    	m_taxa.loadSettingsFrom(settings);
    	m_taxa_regexp.loadSettingsFrom(settings);
    	m_strategy.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_infile.validateSettings(settings);
    	
    	m_outfile.validateSettings(settings);
    	m_overwrite.validateSettings(settings);
    	m_species.validateSettings(settings);
    	m_taxa.validateSettings(settings);
    	m_taxa_regexp.validateSettings(settings);
    	m_strategy.validateSettings(settings);
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

