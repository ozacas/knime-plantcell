package au.edu.unimelb.plantcell.io.read.fasta;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.workflow.FlowVariable;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;


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
public class FastaReaderNodeModel extends AbstractFastaNodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("FASTA Reader");
        
    // settings for this node: regular expressions to process the ">" lines, and the fasta sequence filename
    private final SettingsModelStringArray m_fasta          = (SettingsModelStringArray) make(CFGKEY_FASTA);
    private final SettingsModelString m_oneshot_fasta       = (SettingsModelString) make(CFGKEY_ONESHOT_FASTA);
    private final SettingsModelString  m_accsn_re           = (SettingsModelString) make(CFGKEY_ACCSN_RE);
    private final SettingsModelString  m_descr_re           = (SettingsModelString) make(CFGKEY_DESCR_RE);
    private final SettingsModelString  m_entry_handler      = (SettingsModelString) make(CFGKEY_ENTRY_HANDLER);
    private final SettingsModelBoolean m_stats              = new SettingsModelBoolean(CFGKEY_MAKESTATS, DEFAULT_MAKESTATS);
    private final SettingsModelString  m_seqtype            = new SettingsModelString(CFGKEY_SEQTYPE, SequenceType.AA.toString());
    private final SettingsModelBoolean m_use_accsn_as_rowid = (SettingsModelBoolean) make(CFGKEY_USE_ACCSN_AS_ROWID);
    
    /**
     * Constructor for the node model.
     */
    protected FastaReaderNodeModel() {
        super(0, 2); // output ports only
    }

    public static SettingsModel make(String k) {
    	if (k.equals(CFGKEY_FASTA)) {
    		return new SettingsModelStringArray(k, new String[] { });
    	} else if (k.equals(CFGKEY_ONESHOT_FASTA)) {
    		return new SettingsModelString(CFGKEY_ONESHOT_FASTA, "");
    	} else if (k.equals(CFGKEY_ACCSN_RE)) {
    		return new SettingsModelString(k, DEFAULT_ACCSN_RE);
    	} else if (k.equals(CFGKEY_DESCR_RE)) {
    		return new SettingsModelString(k, DEFAULT_DESCR_RE);
    	} else if (k.equals(CFGKEY_ENTRY_HANDLER)) {
    		return new SettingsModelString(k, DEFAULT_ENTRY_HANDLER);
    	} else if (k.equals(CFGKEY_USE_ACCSN_AS_ROWID)) {
    		return new SettingsModelBoolean(k, true);
    	}
    	return null;
    }
    
    protected DataTableSpec make_output_spec() {   
        // 1. create the column specification in accordance with the as_single parameter
        DataColumnSpec[] allColSpecs = new DataColumnSpec[2];
        allColSpecs[0] = 
            new DataColumnSpecCreator("Sequence", SequenceCell.TYPE).createSpec();
        allColSpecs[1] = 
        	new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
        
        return new DataTableSpec(allColSpecs);
    }
    
    private List<String> getFileList() throws InvalidSettingsException {
    	List<String> ret = new ArrayList<String>();
    	
    	// 1. always add files in the file input box to the list to be loaded (EVEN if inside a loop)
    	for (String f : m_fasta.getStringArrayValue()) {
    		ret.add(f);
    	}
    	
    	// 2. peek thru the available flow variables to find the one which overrides the appropriate
    	//    fasta configuration setting
    	Map<String,FlowVariable> fv_map = this.getAvailableInputFlowVariables();
    	for (String k : fv_map.keySet()) {
    		if (k.equals(CFGKEY_ONESHOT_FASTA)) {
    			ret.add(fv_map.get(k).getValueAsString());
    		} else {
    			logger.warn("Flow variable override of: "+k);
    		}
    	}
    	
    	if (ret.size() < 1) {
    		throw new InvalidSettingsException("No files to process!");
    	} else {
    		logger.info("Found "+ret.size()+" FASTA files to process.");
    	}
    	
    	return ret;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
         
    	DataTableSpec outputSpec = make_output_spec();
    	DataTableSpec statSpec   = SequenceStatistics.getOutputSpec();
    	
    	List<String> filenames = getFileList();
      
    	MyDataContainer c1 = new MyDataContainer(exec.createDataContainer(outputSpec), "Seq");
        MyDataContainer c2 = new MyDataContainer(exec.createDataContainer(statSpec), "Row");
        
        long n_seq   = 0;
        long n_seq_rej = 0;
        Pattern accsn_matcher = Pattern.compile(m_accsn_re.getStringValue());
        Pattern descr_matcher = Pattern.compile(m_descr_re.getStringValue());
       
        // let's add sequences from input file(s) into the output port
        String line  = null;
        String[] accsn = null;
        String[] descr = null;
        StringBuffer seq = null;
      
        int files_done = 0;
        final double portion           = 1.0 / filenames.size();
        SequenceType st = SequenceType.getValue(m_seqtype.getStringValue());
        
        this.setSingleEntry(m_entry_handler.getStringValue().equals(DEFAULT_ENTRY_HANDLER));
        this.setUseAccessionAsRowID(m_use_accsn_as_rowid.getBooleanValue());
        
        for (String fname : filenames) {
           logger.info("Processing FASTA file: "+fname);
           this.setFilename(fname);
          
           File input_sequences     = new File(fname);
           SequenceStatistics stats = m_stats.getBooleanValue() ? new SequenceStatistics(input_sequences) : null;
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
           
           InputStream is = null;
           if (is_compressed) {
        	   is = new GZIPInputStream(new FileInputStream(input_sequences), 16*1024);
           } else {
        	   is = new FileInputStream(input_sequences);
           }
           
           /**
            * We define the mapping between FASTA files and java as 7-bit: I dont think
            * any other character encoding is used by these files - maybe UTF8???
            */
    	   BufferedReader rseq = new BufferedReader(new InputStreamReader(is, "US-ASCII"));

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
		    	    if (save_sequence(c1, st, accsn, descr, seq, stats)) {
		                	n_seq++;
		                	accsn = null; // help java garbage collector
		                	descr = null;
		            }
	    	    }
	          
	            if (n_seq % 1000 == 0) {
	            	try {
		            	// check if the execution monitor was canceled
		                exec.checkCanceled();
	            	} catch (CanceledExecutionException ce) {
	            		rseq.close();		// avoid open file leak
	            		throw ce;
	            	}
	                // and update node progress "traffic light"
	            	double frac = 0;
	            	if (frac > 1.0) {
	            		frac = 1.0;
	            	}
	                exec.setProgress(((double)files_done*portion)+(frac*portion), "Adding " + n_seq+" from "+fname);
	            }
	        }
	      
	        rseq.close();
	        if (stats != null) {
	        	stats.addStats(c2);
	        }
	        files_done++;
        }
        
        // once we are done, we close the container and return its table
        logger.info("Matched "+n_seq+ " sequences, failed to match "+n_seq_rej+" sequences.");
        return new BufferedDataTable[]{c1.close(), c2.close()};
    }
   
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec out = make_output_spec();
        DataTableSpec out2= SequenceStatistics.getOutputSpec();
        return new DataTableSpec[] {out, out2};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_fasta.saveSettingsTo(settings);
        m_accsn_re.saveSettingsTo(settings);
        m_descr_re.saveSettingsTo(settings);
        m_entry_handler.saveSettingsTo(settings);
        m_seqtype.saveSettingsTo(settings);
        m_stats.saveSettingsTo(settings);
        m_use_accsn_as_rowid.saveSettingsTo(settings);
        m_oneshot_fasta.saveSettingsTo(settings);
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
        m_entry_handler.loadSettingsFrom(settings);
        if (settings.containsKey(CFGKEY_SEQTYPE)) {
        	m_seqtype.loadSettingsFrom(settings);
        } else {
        	m_seqtype.setStringValue(SequenceType.UNKNOWN.toString());
        }
        if (settings.containsKey(CFGKEY_MAKESTATS)) {
        	m_stats.loadSettingsFrom(settings);
        } else {
        	m_stats.setBooleanValue(Boolean.FALSE);
        }
        if (settings.containsKey(CFGKEY_USE_ACCSN_AS_ROWID)) {
        	m_use_accsn_as_rowid.loadSettingsFrom(settings);
        } else {
        	m_use_accsn_as_rowid.setBooleanValue(false);		// false for backward compatibility
        }
        if (settings.containsKey(CFGKEY_ONESHOT_FASTA)) {
        	m_oneshot_fasta.loadSettingsFrom(settings);
        }
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
        m_entry_handler.validateSettings(settings);
      
        if (settings.containsKey(CFGKEY_MAKESTATS)) {
        	m_stats.validateSettings(settings);
        }
        if (settings.containsKey(CFGKEY_SEQTYPE)) {
        	m_seqtype.validateSettings(settings);
        }
        if (settings.containsKey(CFGKEY_USE_ACCSN_AS_ROWID)) {
        	m_use_accsn_as_rowid.validateSettings(settings);
        }
        if (settings.containsKey(CFGKEY_ONESHOT_FASTA)) {
        	m_oneshot_fasta.validateSettings(settings);
        }
    }
   
}

