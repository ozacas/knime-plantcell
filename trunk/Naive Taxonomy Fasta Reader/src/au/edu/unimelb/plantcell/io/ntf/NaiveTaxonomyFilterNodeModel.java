package au.edu.unimelb.plantcell.io.ntf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
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


/**
 * This is the model implementation of NaiveTaxonomyFilter.
 * Used to extract sequences from a FASTA file which match a given column of taxa. Regular expressions can be provided to match the taxa entry from the description in the FASTA file. Taxa desired form the input to the node.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class NaiveTaxonomyFilterNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(NaiveTaxonomyFilterNodeModel.class);

	public static final String CFGKEY_TAXA_COL = "taxonomy-column";
	public static final String CFGKEY_FASTA = "fasta-file";
	public static final String CFGKEY_RE_TAXA = "taxonomy-regexp";
	public static final String CFGKEY_RE_ACCSN_DESCR = "accsn-descr-regexp";

    private final SettingsModelString m_taxa_col = new SettingsModelString(CFGKEY_TAXA_COL, "Taxonomy");
    private final SettingsModelString m_fasta    = new SettingsModelString(CFGKEY_FASTA, "nr.fasta");
    private final SettingsModelString m_re_taxa  = new SettingsModelString(CFGKEY_RE_TAXA, "\\[([^\\]]+?)\\]");
    private final SettingsModelString m_re_accsn_descr = new SettingsModelString(CFGKEY_RE_ACCSN_DESCR, "^(\\S+)\\s+(.*)$");
    
    // not persisted
    private Pattern m_re_taxa_compiled;
    
    /**
     * Constructor for the node model.
     */
    protected NaiveTaxonomyFilterNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	logger.info("Computing acceptable species tree from input data");
    	BufferedDataContainer out = exec.createDataContainer(make_output_spec());
    	RowIterator it = inData[0].iterator();
    	int taxa_idx = inData[0].getDataTableSpec().findColumnIndex(m_taxa_col.getStringValue());
    	AcceptableTaxa at = new AcceptableTaxa();
    	while (it.hasNext()) {
    		DataRow   r = it.next();
    		String taxa = r.getCell(taxa_idx).toString();
    		at.add(taxa);
    	}
    	logger.info("Tree computed.");
    	
    	InputStream is;
    	File fname = new File(m_fasta.getStringValue());
    	if (!fname.exists() || !fname.canRead()) {
    		throw new Exception("Unable to read "+m_fasta.getStringValue()+"... aborting!");
    	}
    	boolean is_compressed = fname.getName().toLowerCase().endsWith(".gz");
    	if (is_compressed) {
       	   is = new GZIPInputStream(new FileInputStream(m_fasta.getStringValue()), 16*1024);
        } else {
       	   is = new FileInputStream(m_fasta.getStringValue());
        }
   	   	BufferedReader rseq = new BufferedReader(new InputStreamReader(is));

   	   	logger.info("Compiling regular expressions.");
   	   	m_re_taxa_compiled = Pattern.compile(m_re_taxa.getStringValue());
   	   	Pattern re_accsn_descr     = Pattern.compile(m_re_accsn_descr.getStringValue());
   	   	logger.info("Regular expressions successfully compiled.");

        boolean done = false;
        boolean already_got_header = false;
        String line = "";
        int id = 1;
        /***
         * AWFUL HACK TODO: code borrowed from fasta reader node -- need clean abstraction!
         */
        logger.info("Processing file "+fname.getAbsolutePath());
        while (!done) {
	    	   
    	    // get header line
    	    if (!already_got_header) {
	    	    do {
	    	    	line = rseq.readLine();
	    	    	if (line == null) {
	    	    		done = true;
	    	    		break;
	    	    	}
	    	    } while (!line.startsWith(">"));
    	    }
    	    
    	    if (!done) {
    	    	  String[] entries = line.split("\\x01");
	              if (entries.length > 0 && entries[0].startsWith(">")) {
	                	entries[0] = entries[0].substring(1);	// skip over > for parse_accession()
	              }
	              
	              String tline;
	              StringBuffer seq = new StringBuffer(10 * 1024);
	              boolean got_seq = false;
	              already_got_header = false;
	              int tline_len = 0;
	              do {
	            	  if ((line = rseq.readLine()) == null) {
	            		  already_got_header = false;
	            		  break;
	            	  }
	            	  tline         = line.trim();
	            	  tline_len     = tline.length();
	            	  if (tline_len > 0) {
		            	  char first_c  = tline.charAt(0);
		            	  if (first_c == '>') {
		            		  got_seq = false;
		            		  already_got_header = true;
		            		  break;
		            	  } 
		            	  
		            	  if (Character.isLetter(first_c) || first_c == '*' || first_c == '-') {
		            		  seq.append(tline);
		            		  got_seq = true;
		            	  }
	            	  }
	              } while (tline_len == 0 || got_seq );
	              
	              String matched_entry = suitable_taxonomy(seq.toString(), entries, at);
	              if (matched_entry != null) {
	            	  DataCell[] cells = new DataCell[4];
	            	  Matcher m = re_accsn_descr.matcher(matched_entry);
	            	  if (!m.find() || m.groupCount() < 2) {
	            		  throw new Exception("Unable to find accsn AND description in FASTA entry");
	            	  }
	            	  cells[0] = new StringCell(m.group(1));
	            	  cells[1] = new StringCell(m.group(2));
	            	  cells[2] = new StringCell(seq.toString());
	            	  m = m_re_taxa_compiled.matcher(matched_entry);
	            	  if (!m.find() || m.groupCount() < 1) {
	            		  throw new Exception("Unable to find taxonomy entry in FASTA entry");
	            	  }
	            	  cells[3] = new StringCell(m.group(1));
	            	  out.addRowToTable(new DefaultRow("Seq"+id++, cells));
	              }
	              
	              if (id % 1000 == 0) {
	            	  exec.setProgress("Added "+id+" matching sequences.");
	              }
    	    }
          
	    }
	      
	    rseq.close();
	    logger.info("Species filtering completed successfully.");
	    
    	out.close();
        return new BufferedDataTable[]{out.getTable()};
    }

    /**
     * Examines each FASTA description entry using the specified species tree and tests
     * them to see whether the sequence is to be output or not (filtered). If it is output,
     * the matching entry (first encountered) is returned. Otherwise null.
     * 
     * @param string
     * @param entries
     * @param at
     * @return
     */
    private String suitable_taxonomy(String seq, String[] entries, AcceptableTaxa at) {
		for (String entry : entries) {
			Matcher m = m_re_taxa_compiled.matcher(entry);
			while (m.find()) {
				String matched_taxa = m.group(1);
				if (at.match((matched_taxa.toLowerCase()))) {
						return entry;
				}
			}
		}
		return null;
	}

	private DataTableSpec make_output_spec() {
    	DataColumnSpec[] cols = new DataColumnSpec[4];
    	cols[0] = new DataColumnSpecCreator("Accession", StringCell.TYPE).createSpec();
    	cols[1] = new DataColumnSpecCreator("Description", StringCell.TYPE).createSpec();
    	cols[2] = new DataColumnSpecCreator("Sequence", StringCell.TYPE).createSpec();
    	cols[3] = new DataColumnSpecCreator("Matching Specie (first)", StringCell.TYPE).createSpec();

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
      
        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_taxa_col.saveSettingsTo(settings);
    	m_fasta.saveSettingsTo(settings);
    	m_re_taxa.saveSettingsTo(settings);
    	m_re_accsn_descr.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_taxa_col.loadSettingsFrom(settings);
    	m_fasta.loadSettingsFrom(settings);
    	m_re_taxa.loadSettingsFrom(settings);
    	m_re_accsn_descr.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
       m_taxa_col.validateSettings(settings);
       m_fasta.validateSettings(settings);
       m_re_taxa.validateSettings(settings);
       m_re_accsn_descr.validateSettings(settings);
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

