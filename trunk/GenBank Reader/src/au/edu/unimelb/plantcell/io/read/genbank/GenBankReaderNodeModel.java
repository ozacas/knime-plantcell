package au.edu.unimelb.plantcell.io.read.genbank;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.biojava.bio.seq.Feature.Template;
import org.biojava.bio.seq.io.ParseException;
import org.biojava.bio.seq.io.SymbolTokenization;
import org.biojava.bio.symbol.Alphabet;
import org.biojava.bio.symbol.IllegalAlphabetException;
import org.biojava.bio.symbol.SimpleSymbolList;
import org.biojava.bio.symbol.Symbol;
import org.biojava.bio.symbol.SymbolList;
import org.biojavax.Namespace;
import org.biojavax.RankedCrossRef;
import org.biojavax.RankedDocRef;
import org.biojavax.bio.BioEntryRelationship;
import org.biojavax.bio.seq.RichFeature;
import org.biojavax.bio.seq.RichSequence;
import org.biojavax.bio.seq.io.GenbankFormat;
import org.biojavax.bio.seq.io.RichSeqIOListener;
import org.biojavax.bio.taxa.NCBITaxon;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * This is the model implementation of GenBankReader.
 * Using BioJava, this node reads the specified files/folder for compressed genbank or .gb files and loads the sequences into a single table along with most of key metadata
 *
 * @author http://www.plantcell.unimelb.edu.au
 */
public class GenBankReaderNodeModel extends FastGenbankNodeModel implements RichSeqIOListener {

	// most of the setting model's are inherited from the superclass
	static final String CFGKEY_SEQTYPE = "sequence-type";		// DNA, RNA or Protein
	
	// most settings are re-used from the superclass
	private final SettingsModelString  m_seqtype = new SettingsModelString(CFGKEY_SEQTYPE, "DNA");
	
	// internal state during execute -- not persisted
	private StringBuffer m_symbols;
	private String       m_accsn;
    private StringBuffer m_comments;
    private NCBITaxon    m_taxon;
    @SuppressWarnings("unused")
	private boolean      m_circular;
    private String       m_descr;
    private String       m_seq_version;
    private int          m_entry_version;
    private RichFeature  m_feature;
    private ArrayList<StringCell> m_feature_cells;

    /**
     * Constructor for the node model.
     */
    protected GenBankReaderNodeModel() {
        super();		// same as superclass
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unused")
	@Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	List<File> files_to_read = files2process();
        
        // the data table spec of the single output table, 
        // the table will have three columns:
        DataColumnSpec[] allColSpecs = new DataColumnSpec[9];
        allColSpecs[0] = 
            new DataColumnSpecCreator("GenBank ID", StringCell.TYPE).createSpec();
        allColSpecs[1] = 
            new DataColumnSpecCreator("GenBank Sequence", StringCell.TYPE).createSpec();
        allColSpecs[2] = 
            new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
        allColSpecs[3] =
        	new DataColumnSpecCreator("NCBI Taxon ID", IntCell.TYPE).createSpec();
        allColSpecs[4] =
        	new DataColumnSpecCreator("Sequence Version", StringCell.TYPE).createSpec();
        allColSpecs[5] =
        	new DataColumnSpecCreator("Entry Version", IntCell.TYPE).createSpec();
        allColSpecs[6] =
        	new DataColumnSpecCreator("Comments", StringCell.TYPE).createSpec();
        allColSpecs[7] = 
        	new DataColumnSpecCreator("Description", StringCell.TYPE).createSpec();
        allColSpecs[8] =
        	new DataColumnSpecCreator("Feature Properties (list)", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
        
        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
        
        // the execution context will provide us with storage capacity, in this
        // case a data container to which we will add rows sequentially
        // Note, this container can also handle arbitrary big data tables, it
        // will buffer to disc if necessary.
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
      
        int done_files = 0;
        int hit = 1;
        
        // setup the match data structure (int[]) for the taxa
        
        // BUG: the superclass treats the taxa as NAMES not IDs -- FIXME!!!
        String[] taxa_ids = getFilteringTaxa();
        int max = 0;
        ArrayList<Integer> bits_to_set = new ArrayList<Integer>();
        for (String id : taxa_ids) {
        	if (id.trim().length() > 0) {
        		Integer taxa_id = new Integer(id.trim());
        		if (taxa_id.intValue() > max) {
        			max = taxa_id.intValue();
        			bits_to_set.add(taxa_id);
        		}
        	}
        }
        DenseBitVector bv = new DenseBitVector(max+1);
        for (Integer i : bits_to_set) {
        	bv.set(i.intValue());
        }
		boolean has_taxa_filter = (bv.cardinality() > 0);
		int[] final_taxa_ids = new int[(int) bv.cardinality()];
		
		// process the files
		int failed_files = 0;
    	for (File f : files_to_read) {
    		int cnt = 0;
    		int accepted = 0;
    		// here we use the fully qualified type to make it clear which biojava package we want
    		org.biojavax.bio.seq.io.GenbankFormat gbf = new GenbankFormat();
    		InputStream is;
    		SymbolTokenization st = RichSequence.IOTools.getDNAParser();
    		if (m_seqtype.getStringValue().equalsIgnoreCase("RNA")) {
    			st = RichSequence.IOTools.getRNAParser();
    		} else if (m_seqtype.getStringValue().equalsIgnoreCase("Protein")) {
    			st = RichSequence.IOTools.getProteinParser();
    		}
    	    		
    		// make a new stream rather than use one which has been partially read
    		BufferedReader rdr = new BufferedReader(new InputStreamReader(make_input_stream(f)));
    		
    		// the SeqIOListener (this) will setup internal member variables for the loop 
    		// to process...
    		boolean more = true;
    		try {
	    		do {
	    			// setup internal state to ensure missing cells get generated if the entry does not specify it
	    			m_accsn       = null;
	    			m_symbols     = null;
	    			m_taxon       = null;
	    			m_comments    = null;
	    			m_seq_version = null;
	    			m_descr         = null;
	    			m_feature_cells = null;
	    			
	    			// read the next genbank sequence from the input, failing gracefully to handle poor entries well
    				more = gbf.readRichSequence(rdr, st, this, null);
    				cnt++;
	    			
	    			if (has_taxa_filter) {
	    				int t_id = m_taxon.getNCBITaxID();
	    				if (t_id < 0 || t_id >= bv.length()) {
	    					continue;
	    				}
	    				
	    				if (!bv.get(t_id)) {
	    						continue;
	    				}
	    			}
	    			
	    			// add the row to the table, since it has passed the taxonomy filter (if any)
	    			DataCell[] cells = new DataCell[9];
	    			cells[0] = safe_cell(m_accsn);
	    			cells[1] = safe_cell(m_symbols);
	    			cells[2] = new StringCell(f.getName());
	    			
	    			cells[3] = (m_taxon != null) ? new IntCell(m_taxon.getNCBITaxID()) : DataType.getMissingCell();
	    			cells[4] = safe_cell(m_seq_version);
	    			cells[5] = new IntCell(m_entry_version);
	    			cells[6] = safe_cell(m_comments);
	    			cells[7] = safe_cell(m_descr);
	    			cells[8] = safe_cell(m_feature_cells);
	    			
	    			accepted++;
	    			container.addRowToTable(new DefaultRow("GB"+hit, cells));
	    			hit++;
	    			if (hit % 200 == 0) {
	    				exec.checkCanceled();
	    			}
	    		} while (more);
	    		logger.info("Processed "+cnt+" genbank entries (accepted "+accepted+") in "+f.getName());

    		} catch (Exception e) {
    			failed_files++;
    			logger.warn("Error in genbank record in "+f.getName()+" error msg is: ");
    			logger.warn(e.getMessage());
    			e.printStackTrace();
    		}
    		rdr.close();
    		
    		done_files++;
    		exec.checkCanceled();
    		exec.setProgress(((double) done_files) / files_to_read.size());
    	}
    	
    	logger.info("Processed "+done_files+" files ("+failed_files+" contained errors). Loading complete.");
    	
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	super.saveSettingsTo(settings);
    	m_seqtype.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	super.loadValidatedSettingsFrom(settings);
    	m_seqtype.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	super.validateSettings(settings);
    	m_seqtype.validateSettings(settings);
    }

    /**
     *  **************************** RichSeqIOListener methods ****************************
     *  
     */
	@Override
	public void addFeatureProperty(Object arg0, Object arg1)
			throws ParseException {
		if (m_feature_cells == null) {
			m_feature_cells = new ArrayList<StringCell>();
		}
		if (arg0 != null && arg1 != null) {
			String key = arg0.toString();
			int colon_idx = key.indexOf(':');		// remove namespace prefix from key (not for users!)
			if (colon_idx >= 0) {
				key = key.substring(colon_idx+1);
			}
			m_feature_cells.add(new StringCell(key+"="+arg1.toString()));
		}
	}

	@Override
	public void addSequenceProperty(Object key, Object value)
			throws ParseException {	
	}

	@Override
	public void addSymbols(Alphabet alphabet, Symbol[] symbols, int start, int len)
			throws IllegalAlphabetException {
		assert(start < len && start >= 0);
		assert(alphabet != null && symbols != null);
		
		SymbolList sl;
		if (start == 0) {
			sl = new SimpleSymbolList(symbols, len, alphabet);
		} else {
			Symbol[] new_list = new Symbol[len-start];
			System.arraycopy(symbols, start, new_list, 0, len-start);
			sl = new SimpleSymbolList(new_list, new_list.length, alphabet);
		}
		m_symbols.append(sl.seqString().toUpperCase());
	}

	@Override
	public void endFeature() throws ParseException {	
	}

	@Override
	public void endSequence() throws ParseException {
	}

	@Override
	public void setName(String arg0) throws ParseException {	
	}

	@Override
	public void startFeature(Template arg0) throws ParseException {	
		 m_feature = RichFeature.Tools.makeEmptyFeature();	
	}

	@Override
	public void startSequence() throws ParseException {
		m_symbols = new StringBuffer(1024);
	}

	@Override
	public RichFeature getCurrentFeature() throws ParseException {
		return m_feature;
	}

	@Override
	public void setAccession(String arg0) throws ParseException {
		m_accsn = arg0;
	}

	@Override
	public void setCircular(boolean arg0) throws ParseException {
		m_circular = arg0;
	}

	@Override
	public void setComment(String arg0) throws ParseException {
		if (m_comments == null) {
			m_comments = new StringBuffer(1024);
		}
		m_comments.append(arg0+"\n");
	}

	@Override
	public void setDescription(String arg0) throws ParseException {
		m_descr = arg0;
	}

	@Override
	public void setDivision(String arg0) throws ParseException {	
	}

	@Override
	public void setIdentifier(String arg0) throws ParseException {	
	}

	@Override
	public void setNamespace(Namespace arg0) throws ParseException {	
	}

	@Override
	public void setRankedCrossRef(RankedCrossRef arg0) throws ParseException {
	}

	@Override
	public void setRankedDocRef(RankedDocRef arg0) throws ParseException {
	}

	@Override
	public void setRelationship(BioEntryRelationship arg0)
			throws ParseException {		
	}

	@Override
	public void setSeqVersion(String arg0) throws ParseException {
		m_seq_version = arg0;
	}

	@Override
	public void setTaxon(NCBITaxon arg0) throws ParseException {
		m_taxon = arg0;
	}

	@Override
	public void setURI(String arg0) throws ParseException {		
	}

	@Override
	public void setVersion(int arg0) throws ParseException {
		m_entry_version = arg0;
	}

}

