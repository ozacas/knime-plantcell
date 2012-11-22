package au.edu.unimelb.plantcell.io.read.fasta;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;


/**
 * This is the model implementation of FastaReader.
 * This nodes reads sequences from the user-specified FASTA file and outputs three columns per sequence: 
 * * n1) Accession 
 * * n2) Description - often not accurate in practice 
 * * n3) Sequence data * n * n
 * Neither line breaks or leading/trailing whitespace are preserved.
 *
 * @author Andrew Cassin
 */
public class SequenceExtractorNodeModel extends AbstractFastaNodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger("Extract sequences");
 
    // settings for this node: regular expressions to process the ">" lines, and the fasta sequence filename
    private final SettingsModelStringArray m_fasta    = (SettingsModelStringArray) make(CFGKEY_FASTA);
    private final SettingsModelString m_accsn_re = (SettingsModelString) make(CFGKEY_ACCSN_RE);
    private final SettingsModelString m_descr_re = (SettingsModelString) make(CFGKEY_DESCR_RE);
    private final SettingsModelString m_file_col = (SettingsModelString) make(CFGKEY_FILE_COLUMN);
    private final SettingsModelString m_accsn_col= (SettingsModelString) make(CFGKEY_ACCSN_COLUMN);
    

    /**
     * Constructor for the node model.
     */
    protected SequenceExtractorNodeModel() {
        super(1, 1);
    }

    public static SettingsModel make(String k) {
    	if (k.equals(CFGKEY_FASTA)) {
    		return new SettingsModelStringArray(k, new String[] { });
    	} else if (k.equals(CFGKEY_ACCSN_RE)) {
    		return new SettingsModelString(k, DEFAULT_ACCSN_RE);
    	} else if (k.equals(CFGKEY_DESCR_RE)) {
    		return new SettingsModelString(k, DEFAULT_DESCR_RE);
    	} else if (k.equals(CFGKEY_FILE_COLUMN)) {
    		return new SettingsModelString(k, "Filename");
    	} else if (k.equals(CFGKEY_ACCSN_COLUMN)) {
    		return new SettingsModelString(k, "Accession");
    	}
    	return null;
    }
    
    protected DataTableSpec make_output_spec() {   
        // this node only adds the sequence to the existing input
        DataColumnSpec[] allColSpecs = new DataColumnSpec[1];
        allColSpecs[0] = new DataColumnSpecCreator("Sequence", StringCell.TYPE).createSpec();
      
        
        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
        return outputSpec;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
         
    	DataTableSpec outputSpec = make_output_spec();
    	
    	ArrayList<String> filenames = new ArrayList<String>();
    	for (String f : m_fasta.getStringArrayValue()) {
    		filenames.add(f);
    	}
    	
    	if (filenames.size() < 1) {
    		throw new InvalidSettingsException("No files to process!");
    	} else {
    		logger.info("Found "+filenames.size()+" FASTA files to process.");
    	}
      
        BufferedDataContainer container = exec.createDataContainer(new DataTableSpec(inData[0].getSpec(), outputSpec));
        
        long n_seq   = 0;
        long n_seq_rej = 0;
        Pattern accsn_matcher = Pattern.compile(m_accsn_re.getStringValue());
        Pattern descr_matcher = Pattern.compile(m_descr_re.getStringValue());
       
        // 0. build up a list of files and their accessions to be matched from the chosen columns of the input table
        int file_idx = inData[0].getSpec().findColumnIndex(m_file_col.getStringValue());
        int accsn_idx= inData[0].getSpec().findColumnIndex(m_accsn_col.getStringValue());
        if (file_idx < 0 || accsn_idx < 0) {
        	throw new InvalidSettingsException("Unable to locate key columns: re-configure the node?");
        }
        RowIterator it = inData[0].iterator();
        
        HashMap<String,Set<String>> map = new HashMap<String,Set<String>>();
        
        while (it.hasNext()) {
        	DataRow r = it.next();
        	DataCell filename = r.getCell(file_idx);
        	DataCell accsn    = r.getCell(accsn_idx);
        	
        	if (filename == null || filename.isMissing() || filename.toString().length() < 1 ||
        			accsn == null|| accsn.isMissing()    || accsn.toString().length() < 1) {
        		logger.warn("Skipping row "+r.getKey().getString()+" as there is key data missing");
        		continue;
        	}
        	
        	String fname = filename.toString();
        	String acc   = accsn.toString();
        	if (!map.containsKey(fname)) {
        		HashSet<String> l = new HashSet<String>();
        		l.add(acc);
        		map.put(fname, l);
        	} else {
        		Set<String> l = map.get(fname);
        		l.add(acc);
        	}
        }
        for (String file : map.keySet()) {
        	logger.info(map.get(file).size()+" unique accessions required from file "+file);
        }
        
        // 1. let's add matching sequences from input file(s) into the output port
        String line  = null;
        String[] accsn = null;
        @SuppressWarnings("unused")
		String[] descr = null;
        StringBuffer seq = null;
      
        int files_done = 0;
        final double portion           = 1.0 / filenames.size();
        HashMap<String,String> seqmap = new HashMap<String,String>(inData[0].getRowCount());
        int matched = 0;
        for (String fname : filenames) {
           logger.info("Processing FASTA file: "+fname);
           Set<String> required_accsns = lookup_file(map, fname);
           if (required_accsns == null) {
        	   logger.info("Skipping "+fname+" as it is not in the list of files to process - reconfigure?");
        	   continue;
           }
           
           File input_sequences     = new File(fname);
           boolean is_compressed    = false;
           if (fname.toLowerCase().endsWith(".gz") || 
        		   fname.toLowerCase().endsWith(".z")) {
        	   is_compressed = true;
           }
    	   double p_size            = input_sequences.length();
           if (p_size < 1) {
            	logger.warn("Empty file: "+fname+", ignored.");
            	files_done++;
            	continue;
           }
           BufferedFileReader rseq = null;
           InputStream is = null;
           
           if (is_compressed) {
        	   is = new GZIPInputStream(new FileInputStream(input_sequences), 16*1024);
           } else {
        	   is = new FileInputStream(input_sequences);
           }
    	   rseq = BufferedFileReader.createNewReader(is);

           fname = input_sequences.getName();
           boolean done = false;
           boolean already_got_header = false;
           
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
		              accsn = parse_accession(accsn_matcher,entries);
		              descr = parse_description(descr_matcher,entries);
		              String tline;
		              seq = new StringBuffer(10 * 1024);
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
	    	    }
	            
	    	    // save the sequence to the container
	    	    
	    	    if (!done) {
	    	    	String matched_accsn = has_accession(accsn, required_accsns);
		    	    if (matched_accsn != null) {
		                	seqmap.put(fname + ":" + matched_accsn, seq.toString());
		                	matched++;
		                	exec.checkCanceled();
		        	        exec.setProgress(((double) files_done) * portion, "Matched "+matched+" sequences so far");
		            }
                	n_seq++;
	    	    }
	        }
	      
	        rseq.close();
	      
	        files_done++;
	        exec.checkCanceled();
	        exec.setProgress(((double) files_done) * portion, "Matched "+matched+" sequences so far");
        }
        
        // 2. read the input rows again, this time output'ing the match sequences (missing cells
        // where no match was found)
        it = inData[0].iterator();
        while (it.hasNext()) {
        	DataRow r = it.next();
        	DataCell[] cells = new DataCell[1];
        	DataCell file_cell = r.getCell(file_idx);
        	DataCell accsn_cell= r.getCell(accsn_idx);
        	
        	if (file_cell == null || accsn_cell == null || file_cell.isMissing() || accsn_cell.isMissing()) {
        		cells[0] = DataType.getMissingCell();
        	} else {
        		String key = file_cell.toString() + ":" + accsn_cell.toString();
        		cells[0] = seqmap.containsKey(key) ? new StringCell(seqmap.get(key)) : DataType.getMissingCell();
        	}
        	container.addRowToTable(new JoinedRow(r, new DefaultRow(r.getKey().getString(), cells)));	
        }
        
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        
        logger.info("Matched "+n_seq+ " sequences, failed to match "+n_seq_rej+" sequences.");
        return new BufferedDataTable[]{out};
    }
    
    /**
     * Returns a matched accession if any can be found, otherwise <code>null</code>
     * 
     * @param accsns
     * @param required_accsns
     * @return
     */
    private String has_accession(String[] accsns, Set<String> required_accsns) {
		for (String a : accsns) {
			if (required_accsns.contains(a))
				return a;
		}
		return null;
	}

	private Set<String> lookup_file(HashMap<String, Set<String>> map,
			String fname) {
		File f = new File(fname);
		
		if (map.containsKey(fname)) {
			logger.info("Mapped complete path for FASTA file: "+fname);
			return map.get(fname);
		} else if (map.containsKey(f.getName())) {
			logger.info("Found FASTA name match for: "+fname);
			return map.get(f.getName());
		} else {
			Set<String> fnames = map.keySet();
			for (String tmp : fnames) {
				if (tmp.toLowerCase().indexOf(f.getName().toLowerCase()) >= 0) {
					logger.info("Found fuzzy match for "+fname+" to "+tmp);
					return map.get(tmp);
				}
			}
			
			logger.warn("Unable to find match for file: "+fname);
			return null;
		}
	}

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec out = make_output_spec();
        return new DataTableSpec[] {new DataTableSpec(inSpecs[0], out) };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_fasta.saveSettingsTo(settings);
        m_accsn_re.saveSettingsTo(settings);
        m_descr_re.saveSettingsTo(settings);
        
        m_file_col.saveSettingsTo(settings);
        m_accsn_col.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fasta.loadSettingsFrom(settings);
        m_accsn_re.loadSettingsFrom(settings);
        m_descr_re.loadSettingsFrom(settings);
        
        m_file_col.loadSettingsFrom(settings);
        m_accsn_col.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {    
        m_fasta.validateSettings(settings);
        m_accsn_re.validateSettings(settings);
        m_descr_re.validateSettings(settings);
        
        m_file_col.validateSettings(settings);
        m_accsn_col.validateSettings(settings);
    }
  
}

