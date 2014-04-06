package au.edu.unimelb.plantcell.io.write.phyloxml;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forester.io.parsers.PhylogenyParser;
import org.forester.io.parsers.util.ParserUtils;
import org.forester.io.writers.PhylogenyWriter;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyMethods;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.data.Annotation;
import org.forester.phylogeny.data.BranchColor;
import org.forester.phylogeny.data.BranchData;
import org.forester.phylogeny.data.BranchWidth;
import org.forester.phylogeny.data.Confidence;
import org.forester.phylogeny.data.DomainArchitecture;
import org.forester.phylogeny.data.NodeData;
import org.forester.phylogeny.data.ProteinDomain;
import org.forester.phylogeny.data.Sequence;
import org.forester.phylogeny.data.Taxonomy;
import org.forester.phylogeny.data.Uri;
import org.forester.phylogeny.iterators.PhylogenyNodeIterator;
import org.forester.util.ForesterUtil;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorAttr;
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
import au.edu.unimelb.plantcell.core.cells.SequenceValue;


/**
 * Creates a phyloxml document from the input data, decorated with data from the input table.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class PhyloXMLWriterNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("PhyloXML Writer");
        
    /** user config keys */
	static final String CFGKEY_INFILE    = "input file";
	static final String CFGKEY_OUTFILE   = "output phyloxml file";
    static final String CFGKEY_OVERWRITE = "overwrite";
	static final String CFGKEY_SPECIES   = "species-column";
	static final String CFGKEY_DOMAIN_LABELS = "domain-labels-collection";
	static final String CFGKEY_DOMAIN_STARTS = "domain-start-collection";
	static final String CFGKEY_DOMAIN_ENDS   = "domain-end-collection";
	static final String CFGKEY_START_PROG    = "start-archaeo?";
	static final String CFGKEY_TAXA          = "taxa-column";
	static final String CFGKEY_TAXA_REGEXP   = "taxa-regexp";
	static final String CFGKEY_WANT_SEQUENCE = "want-sequence?";
	static final String CFGKEY_WANT_IMAGES   = "image-url";
	static final String CFGKEY_ASSUME_SUPPORT= "assume-internal-node-names-are-support?";
	static final String CFGKEY_BRANCH_WIDTHS = "branch-width-column";
	static final String CFGKEY_VECTOR_DATA   = "vector-data-column";
    
	// persisted state
	private final SettingsModelString m_infile = new SettingsModelString(CFGKEY_INFILE, "");
	private final SettingsModelString m_outfile= new SettingsModelString(CFGKEY_OUTFILE, "");
	private final SettingsModelBoolean m_overwrite = new SettingsModelBoolean(CFGKEY_OVERWRITE, Boolean.FALSE);
	private final SettingsModelBoolean m_start_prog = new SettingsModelBoolean(CFGKEY_START_PROG, Boolean.TRUE);
	private final SettingsModelString m_taxa    = new SettingsModelString(CFGKEY_TAXA, "");
	private final SettingsModelString m_species = new SettingsModelString(CFGKEY_SPECIES, "");			// scientific name assumed for now
	private final SettingsModelString m_domain_labels = new SettingsModelString(CFGKEY_DOMAIN_LABELS, "");
	private final SettingsModelString m_domain_starts = new SettingsModelString(CFGKEY_DOMAIN_STARTS, "");
	private final SettingsModelString m_domain_ends   = new SettingsModelString(CFGKEY_DOMAIN_ENDS, "");
	private final SettingsModelString m_taxa_regexp   = new SettingsModelString(CFGKEY_TAXA_REGEXP, "(.*)");
	private final SettingsModelBoolean m_save_sequence= new SettingsModelBoolean(CFGKEY_WANT_SEQUENCE, Boolean.FALSE);
	private final SettingsModelString m_image_url = new SettingsModelString(CFGKEY_WANT_IMAGES, "");
	private final SettingsModelBoolean m_assume_support = new SettingsModelBoolean(CFGKEY_ASSUME_SUPPORT, Boolean.FALSE);
	private final SettingsModelString m_branch_widths = new SettingsModelString(CFGKEY_BRANCH_WIDTHS, "");
	private final SettingsModelString m_vector_data = new SettingsModelString(CFGKEY_VECTOR_DATA, "");	// some workflows wont have this setting saved: backward compatibility
	
   
	// not persisted -- used during execute()
	private Pattern m_taxa_pattern;
	
    /**
     * Constructor for the node model.
     */
    protected PhyloXMLWriterNodeModel() {
        super(1, 1);
        m_start_prog.setEnabled(false);		// TODO BUG FIXME: causes KNIME crash at the moment... not sure why
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	try {
    	// 0. find the various columns, some are mandatory others not
    	int taxa_idx = inData[0].getSpec().findColumnIndex(m_taxa.getStringValue());
    	if (taxa_idx < 0)
    		throw new InvalidSettingsException("Cannot find column of sequences to decorate onto tree (reconfigure?): "+m_taxa.getStringValue());
    	int species_idx    = inData[0].getSpec().findColumnIndex(m_species.getStringValue());
    	int dom_labels_idx = inData[0].getSpec().findColumnIndex(m_domain_labels.getStringValue());
    	int dom_start_idx  = inData[0].getSpec().findColumnIndex(m_domain_starts.getStringValue());
    	int dom_end_idx    = inData[0].getSpec().findColumnIndex(m_domain_ends.getStringValue());
    	int image_idx      = inData[0].getSpec().findColumnIndex(m_image_url.getStringValue());
    	int vector_idx     = inData[0].getSpec().findColumnIndex(m_vector_data.getStringValue());
    	logger.info("Using taxa column '"+m_taxa.getStringValue()+"' to map onto tree");
    	if (species_idx >= 0) {
    		logger.info("Using species column: "+m_species.getStringValue());
    	}
    	// 1. get started...
    	logger.info("Reading tree from "+m_infile.getStringValue());
    	File infile  = new File(m_infile.getStringValue());
    	File outfile = new File(m_outfile.getStringValue());
    	if (outfile.exists() && !m_overwrite.getBooleanValue())
    		throw new InvalidSettingsException("Will not overwrite existing: "+outfile.getAbsolutePath());
    	logger.info("Saving decorated tree to "+m_outfile.getStringValue());
    	PhylogenyParser parser = ParserUtils.createParserDependingOnFileType(infile, true);
    	Phylogeny[] phys = PhylogenyMethods.readPhylogenies(parser, infile);
    	m_taxa_pattern = Pattern.compile(m_taxa_regexp.getStringValue());
    	logger.info("Loaded and now decorating "+phys.length+" input trees.");
    	
    	// 2. process input rows collecting data as we go...
    	HashMap<String,Color>   colour_map = new HashMap<String,Color>();
    	HashMap<String,String> species_map = new HashMap<String,String>();
    	HashMap<String,String> uri_map = new HashMap<String,String>();
    	HashMap<String,DomainArchitecture> domain_map = new HashMap<String,DomainArchitecture>();
    	HashMap<String,SequenceValue> taxa_map = new HashMap<String,SequenceValue>();
    	HashMap<String,String> taxa2bwval = new HashMap<String,String>();	// only populated if want_branch_widths
    	HashMap<String,VectorDataList> taxa2vectordata = new HashMap<String,VectorDataList>();
    	int not_decorated = 0;
    	int bw_idx = inData[0].getSpec().findColumnIndex(m_branch_widths.getStringValue());
    	boolean want_branch_widths = (bw_idx >= 0);
				
    	if (!want_branch_widths)
    		logger.warn("Not computing branch widths for each branch in the tree");
    	else {
    		logger.info("Using unique count of column '"+m_branch_widths.getStringValue()+"' to determine branch widths. Only decorated taxa are used.");
    	}
    	for (DataRow r : inData[0]) {
    		// use the row colours for the taxa in the phyloxml
    		DataCell taxa_cell = r.getCell(taxa_idx);
    		
    		// skip missing values for taxa...
    		if (taxa_cell == null || taxa_cell.isMissing())
    			continue;
    		
    		SequenceValue sv = (SequenceValue) taxa_cell;
    		String taxa = sv.getID();
    		if (taxa == null)	{	// no ID? ie. no decoration...
    			not_decorated++;
    			continue;
    		}
    		
    		taxa_map.put(taxa, sv);
    		ColorAttr c = inData[0].getSpec().getRowColor(r);
    		Color   col = c.getColor();
    		if (!col.equals(ColorAttr.getInactiveColor())) {
    			colour_map.put(taxa, col);
    		}
    		
    		if (species_idx >= 0) {
    			DataCell species_cell = r.getCell(species_idx);
    			if (species_cell != null && !species_cell.isMissing()) 
    				species_map.put(taxa, species_cell.toString());
    		}
    		if (image_idx >= 0) {
    			DataCell image_cell = r.getCell(image_idx);
    			if (image_cell != null && !image_cell.isMissing())
    				uri_map.put(taxa, image_cell.toString());
    		}
    	
    		if (dom_start_idx >= 0 && dom_end_idx >= 0 && dom_labels_idx >= 0) {
    			DataCell start_list = r.getCell(dom_start_idx);
    			DataCell end_list = r.getCell(dom_end_idx);
    			DataCell labels = r.getCell(dom_labels_idx);
    			
    			if (start_list != null && end_list != null && labels != null &&
    					!start_list.isMissing() && !end_list.isMissing() && !labels.isMissing()) {
    				
    				addDomainsToMap(domain_map, taxa, start_list, end_list, labels, sv.getLength(), r.getKey().getString());
    			}
    			
    		}
    		if (want_branch_widths) {
    			DataCell branch_value = r.getCell(bw_idx);
    			if (branch_value != null && !branch_value.isMissing())
    				taxa2bwval.put(taxa, branch_value.toString());
    		}
    		
    		if (vector_idx >= 0) {
    			DataCell vector_data_cell = r.getCell(vector_idx);
    			if (!vector_data_cell.isMissing())
    				taxa2vectordata.put(taxa, new VectorDataList(vector_data_cell));
    		}
    	}
    	logger.info("Colour map has "+colour_map.size()+" taxa.");
    	logger.info("Species map has "+species_map.size()+" taxa.");
    	logger.info("Domain map has "+domain_map.size()+" taxa.");
    	logger.info("Taxa map has "+taxa_map.size()+" taxa.");
    	logger.info("Branch width value map has "+taxa2bwval.size()+" taxa.");
    	logger.info("Vector data map has "+taxa2vectordata.size()+ " taxa.");
    	
    	MyDataContainer c = new MyDataContainer(exec.createDataContainer(make_output_spec()), "Row");
    	
    	// 3. walk tree decorating it with input data...
    	HashMap<String,HashSet<String>> node2branchset = new HashMap<String,HashSet<String>>();
    	
    	int decorated = 0;
    	for (Phylogeny phy : phys) {
    		int saw = 0;
    		for (final PhylogenyNodeIterator it = phy.iteratorPostorder(); it.hasNext(); ) {
    			final PhylogenyNode n = it.next();
    			boolean is_decorated = false;
    			saw++;
    			
    			// assume node name for internal nodes is support? If so, then convert it to a 
    			// PhyloXML confidence setting so that Archa... understands the data - TODO FIXME should we support non-bootstrap confidence types?
    			if (m_assume_support.getBooleanValue()) {
    				if (n.isInternal() && n.getName().trim().matches("^[\\d\\.]+$")) {
    					try {
    						String support = n.getName();
    						Confidence conf = new Confidence(Double.valueOf(support), "bootstrap");
    						n.getBranchData().addConfidence(conf);
        					n.setName("");
    					} catch (NumberFormatException nfe) {
    						// do nothing, we just leave the node as it is...
    					}
    				}
    			}
    			
    			if (n.isExternal() && want_branch_widths) {
    				// we dont compute it unless we have to: expensive in time and memory!
    				String name = n.getName();
    				
    				String value = taxa2bwval.get(getTaxa(name));
    				if (value != null) {
    					propagate_to_root(node2branchset, n, value);
    					is_decorated = true;
    				}
    			}
    			
    			String taxa = getTaxa(n.getName());
    			if (colour_map.containsKey(taxa)) {
    				BranchData bd = new BranchData();
    				BranchColor bc = new BranchColor(colour_map.get(taxa));
    				bd.setBranchColor(bc);
    				n.setBranchData(bd);
    				is_decorated = true;
    			}
    			if (species_map.containsKey(taxa)) {
    				NodeData nd = n.getNodeData();
    				Taxonomy t = new Taxonomy();
    				String name = species_map.get(taxa);
    				t.setScientificName(name);
    				String image_url = (image_idx >= 0) ? uri_map.get(taxa) : null;
    				if (image_url != null) {
    					t.addUri(new Uri(image_url, "", "image"));
    				}
    				nd.setTaxonomy(t);
    				is_decorated = true;
    			}
    			
    			if (taxa2vectordata.containsKey(taxa)) {
    				VectorDataList vdl = taxa2vectordata.get(taxa);
    				assert(vdl != null);
    				vdl.setNodeVector(n);
    			}
    			
    			// we just build up s for now, and then decide later whether to use it with the annotation data
    			Sequence s = new Sequence();
				if (m_save_sequence.getBooleanValue()) {
					SequenceValue sv = taxa_map.get(taxa);
					if (sv != null) {
						// only add an annotation to the phyloxml if a description for the sequence is available
						if (sv.hasDescription()) {
							Annotation annot = new Annotation();
							annot.setDesc(sv.getDescription());
    						s.addAnnotation(annot);
						}
						// but all sequences objects should have sequence...
						s.setMolecularSequence(sv.getStringValue());
					}
				}
				
				// internal tree nodes cannot have sequence data (or domain architecture)
				if (n.isExternal()) {
	    			if (domain_map.containsKey(taxa)) {
	    				s.setDomainArchitecture(domain_map.get(taxa));
	    				n.getNodeData().setSequence(s);
	    				is_decorated = true;
	    			} else if (m_save_sequence.getBooleanValue()) {
	    				n.getNodeData().setSequence(s);
	    				is_decorated = true;
	    			}
				}
    			
    			if (is_decorated)
    				decorated++;
    		}
    		
    		logger.info("Saw "+saw+" tree nodes in tree.");
    	}
    	
    	// 3a. now that the whole tree has been traversed we can add the branch widths
    	// note that we use the square root transform to avoid branches being too big on screen in Archaeopteryx...
    	if (want_branch_widths) {
    		logger.info("Branch set has "+node2branchset.size()+" tree nodes.");
    		boolean want_sqrt = false;
    		for (HashSet<String> set : node2branchset.values()) {
    			if (set.size() > 50) {
    				want_sqrt = true; 
    				logger.warn("Encountered large branch widths: applying SQRT transform to all.");
    				break;
    			}
    		}
    		
	    	HashMap<String,List<String>> missing_widths = new HashMap<String,List<String>>();
	    	int min_bw = Integer.MAX_VALUE;
	    	int max_bw = Integer.MIN_VALUE;
	    	for (Phylogeny phy : phys) {
	    		for (final PhylogenyNodeIterator it = phy.iteratorPostorder(); it.hasNext(); ) {
	    			final PhylogenyNode n = it.next();
	    			
	    			String id = ""+n.getId();
	    			HashSet<String> set = node2branchset.get(id);
	    			if (set == null || n.getBranchData() == null) {
	    				ArrayList<String> taxa = new ArrayList<String>();
    					for (PhylogenyNode kids : n.getAllExternalDescendants()) {
    						taxa.add(kids.getName());
    					}
    					missing_widths.put(id,taxa);
	    			} else {
	    				BranchData bd = n.getBranchData();
	    				assert(bd != null);
	    				int size = set.size();
	    				if (size > max_bw) 
	    					max_bw = size;
	    				if (size < min_bw)
	    					min_bw = size;
	    				BranchWidth bw = new BranchWidth(want_sqrt ? Math.sqrt(size) : size);
	    				bd.setBranchWidth(bw);
	    			}
	    		}
	    	}
	    	logger.info("Minimum branch width decorated onto tree was "+min_bw+", maximum was "+max_bw);
	    	
	    	if (missing_widths.size() > 0) {
	    		logger.warn(""+missing_widths.size()+" tree nodes do not have branch widths set (input taxa table is incomplete?). These correspond to nodes with the following descendants:");
	    		for (String id : missing_widths.keySet()) {
	    			StringBuilder sb = new StringBuilder();
	    			for (String kid : missing_widths.get(id)) {
	    				sb.append(kid);
	    				sb.append(' ');
	    			}
	    			logger.warn("\t"+sb.toString().trim());
	    		}
	    	}
    	}
    	
    	// 3b. save it...
    	PhylogenyWriter writer = new PhylogenyWriter();
    	writer.toPhyloXML(phys, 0, outfile, ForesterUtil.LINE_SEPARATOR);
    	
    	if (not_decorated > 0)
    		logger.warn("The supplied taxa regexp did not match "+not_decorated+" rows of input data.");
    	logger.info("Decorated: "+decorated+" taxa.");
    	
    	DataCell[] cells = new DataCell[3];
    	cells[0] = new StringCell(m_infile.getStringValue());
    	cells[1] = new StringCell(outfile.getAbsolutePath());
    	cells[2] = new IntCell(decorated);
    	c.addRow(cells);
    	
    	// 4. start phylo tree view requested by the user? (optional)
    	/*if (m_start_prog.getBooleanValue() && phys != null && phys.length > 0) {
    		 // Display of the tree(s) with Archaeopteryx.
           Archaeopteryx.createApplication( phys );
    	}*/

    	// 5. all done
        return new BufferedDataTable[] { c.close() };
        
    	} catch (Exception e) {
    		e.printStackTrace();
    		throw e;
    	}
    	
    }

    private void propagate_to_root(final HashMap<String, HashSet<String>> node2bval, 
    								final PhylogenyNode n, final String value) {
    	assert(node2bval != null && n != null && value != null);
    	String id = "" + n.getId();
    	HashSet<String> set = node2bval.get(id);
    	if (set == null) {
    		set = new HashSet<String>();
    	}
    	set.add(value);
    	node2bval.put(id, set);
    	
		if (n.isRoot()) {
			return;		// do not recurse job done!
		} else {
			propagate_to_root(node2bval, n.getParent(), value);
		}
	}

	private DataTableSpec make_output_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[3];
		cols[0] = new DataColumnSpecCreator("Input file", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Output file", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Number of decorated taxa", IntCell.TYPE).createSpec();
		return new DataTableSpec(cols);
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

	private void addDomainsToMap(final HashMap<String, DomainArchitecture> domain_map,
			final String taxa, final DataCell start_list, final DataCell end_list, final DataCell label_list, final int total_length, final String rk) throws InvalidSettingsException {
		assert(domain_map != null);
		CollectionDataValue starts = null;
		if (start_list instanceof CollectionDataValue) 
			starts = (CollectionDataValue)start_list;
		CollectionDataValue ends = null;
		if (end_list instanceof CollectionDataValue) 
			ends = (CollectionDataValue)end_list;
	
		CollectionDataValue labels = null;
		if (label_list instanceof CollectionDataValue) 
			labels = (CollectionDataValue)label_list;
		
		if (starts == null || ends == null || labels == null || starts.size() != ends.size() || starts.size() != labels.size())
			throw new InvalidSettingsException("Missing data for domains: aborting on row"+rk);
		
		Iterator<DataCell> it_starts = starts.iterator();
		Iterator<DataCell> it_ends = ends.iterator();
		Iterator<DataCell> it_labels = labels.iterator();
		
		DomainArchitecture da = new DomainArchitecture();
		for (int i=0; i<starts.size(); i++) {
			DataCell label = it_labels.next();
			DataCell start = it_starts.next();
			DataCell end   = it_ends.next();
			
			if (label == null || start == null || end == null || label.isMissing() || start.isMissing() || end.isMissing()) {
				continue;
			}
			
			ProteinDomain pd = new ProteinDomain(label.toString(), 
												Integer.valueOf(start.toString())+1, 
												Integer.valueOf(end.toString())+1, 1.0);
			da.addDomain(pd);
		}
		da.setTotalLength(total_length);		// total length of the molecular sequence (eg. AA)
	
		// adding zero sized vector to the XML output will cause an xml load exception so...
		if (starts.size() > 0)
			domain_map.put(taxa, da);
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
    	m_outfile.saveSettingsTo(settings);
    	m_overwrite.saveSettingsTo(settings);
    	m_start_prog.saveSettingsTo(settings);
    	m_species.saveSettingsTo(settings);
    	m_domain_labels.saveSettingsTo(settings);
    	m_domain_starts.saveSettingsTo(settings);
    	m_domain_ends.saveSettingsTo(settings);
    	m_taxa.saveSettingsTo(settings);
    	m_taxa_regexp.saveSettingsTo(settings);
    	m_save_sequence.saveSettingsTo(settings);
    	m_image_url.saveSettingsTo(settings);
    	m_assume_support.saveSettingsTo(settings);
    	m_branch_widths.saveSettingsTo(settings);
    	m_vector_data.saveSettingsTo(settings);
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
    	m_start_prog.loadSettingsFrom(settings);
    	m_species.loadSettingsFrom(settings);
    	m_domain_labels.loadSettingsFrom(settings);
    	m_domain_starts.loadSettingsFrom(settings);
    	m_domain_ends.loadSettingsFrom(settings);
    	m_taxa.loadSettingsFrom(settings);
    	m_taxa_regexp.loadSettingsFrom(settings);
    	m_save_sequence.loadSettingsFrom(settings);
    	m_image_url.loadSettingsFrom(settings);
    	if (settings.containsKey(CFGKEY_ASSUME_SUPPORT)) {			// backward compatibility
    		m_assume_support.loadSettingsFrom(settings);
    	} else {
    		m_assume_support.setBooleanValue(Boolean.FALSE);		// dont assume by default
    	}
    	if (settings.containsKey(CFGKEY_BRANCH_WIDTHS)) {
    		m_branch_widths.loadSettingsFrom(settings);
    	} else {
    		m_branch_widths.setStringValue("");
    	}
    	if (settings.containsKey(CFGKEY_VECTOR_DATA)) {
    		m_vector_data.loadSettingsFrom(settings);
    	} else {
    		m_vector_data.setStringValue("");
    	}
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
    	m_start_prog.validateSettings(settings);
    	m_species.validateSettings(settings);
    	m_domain_labels.validateSettings(settings);
    	m_domain_starts.validateSettings(settings);
    	m_domain_ends.validateSettings(settings);
    	m_taxa.validateSettings(settings);
    	m_taxa_regexp.validateSettings(settings);
    	m_save_sequence.validateSettings(settings);
    	m_image_url.validateSettings(settings);
    	
    	// the settings have been left out for backward compatibility (as some saved workflows may not have the data)
    	//m_assume_support.validateSettings(settings);
    	//m_branch_widths.validateSettings(settings);
    	//m_vector_data.validateSettings(settings);
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

