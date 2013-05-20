package au.edu.unimelb.plantcell.io.write.fasta;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;


/**
 * This is the model implementation of FastaWriter.
 * Creates a .fasta file with the specified accession, description, and sequence columns
 *
 * @author Andrew Cassin
 */
public class FastaWriterNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Fasta Writer");
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_SEQ   = "Sequence";
	static final String CFGKEY_FILE  = "Output Filename";
    static final String CFGKEY_OVERWRITE = "overwrite";
	static final String CFGKEY_FILENAME_SUFFIX = "suffix";

    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels".
   
    private final SettingsModelString       m_seq_col = (SettingsModelString) make(CFGKEY_SEQ);
    private final SettingsModelString      m_filename = (SettingsModelString) make(CFGKEY_FILE);
    private final SettingsModelBoolean    m_overwrite = (SettingsModelBoolean) make(CFGKEY_OVERWRITE);
    private final SettingsModelString     m_suffix    = (SettingsModelString) make(CFGKEY_FILENAME_SUFFIX);
   
    /**
     * Constructor for the node model.
     */
    protected FastaWriterNodeModel() {
        super(1, 1);
    }

    public static SettingsModel make(String field_name) {
    	if (field_name.equals(CFGKEY_SEQ))
    		return new SettingsModelString(CFGKEY_SEQ, CFGKEY_SEQ);
    	else if (field_name.equals(CFGKEY_FILE))
    		return new SettingsModelString(CFGKEY_FILE, ""); 
    	else if (field_name.equals(CFGKEY_OVERWRITE)) 
    		return new SettingsModelBoolean(CFGKEY_OVERWRITE, false);
    	else if (field_name.equals(CFGKEY_FILENAME_SUFFIX)) 
    		return new SettingsModelString(CFGKEY_FILENAME_SUFFIX, "");
    	else
    		return null;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        String fname = m_filename.getStringValue();
        if (fname == null || fname.length() < 1) {
        	throw new InvalidSettingsException("No filename specified... nothing to save!");
        }
        
        // create a map from each suffix to an open file writer for saving sequences to
        HashMap<String,PrintWriter> file_map = new HashMap<String,PrintWriter>();
        String val = m_suffix.getStringValue();
    	boolean use_suffix = (val != null && val.length() > 0 && !val.equalsIgnoreCase("<none>"));
    	File basename = new File(fname);
    	int suffix_idx = -1;
    	if (!use_suffix) {
    		file_map.put(basename.getName(), new PrintWriter(new FileWriter(basename)));
    	} else {
    		suffix_idx = inData[0].getSpec().findColumnIndex(m_suffix.getStringValue());
    		if (suffix_idx < 0)
    			throw new InvalidSettingsException("Cannot locate suffix column... aborting! "+m_suffix.getStringValue());
    	}
    	
    	// which column has the sequences?
        int seq_idx = inData[0].getSpec().findColumnIndex(m_seq_col.getStringValue());
        if (seq_idx  < 0) 
        	throw new InvalidSettingsException("Cannot find sequence column: "+m_seq_col.getStringValue());
        
        // replicate input data on output port
        DataTableSpec inSpec  = inData[0].getDataTableSpec();
       
        ColumnRearranger outputTable = new ColumnRearranger(inSpec);
        int[] cols = new int[inSpec.getNumColumns()];
        for (int i=0; i<cols.length; i++) {
        	cols[i] = i;
        }
        outputTable.keepOnly(cols);
        
        RowIterator it = inData[0].iterator();
        while (it.hasNext()) {
        	DataRow r = it.next();
        	DataCell sc = r.getCell(seq_idx);
        	if (sc == null || sc.isMissing())
        		continue;
        	SequenceValue sv = (SequenceValue) sc;
        	String id = sv.getID();
    		id = id.replaceAll("[\\r\\n]+", " ");
    		
    		// never fails (unless exception thrown) and the writer is ready for use...
    		PrintWriter pw = get_pw(use_suffix, suffix_idx, r, file_map, basename);
    		
        	if (sv.hasDescription()) {
        		String descr = sv.getDescription();			
        		descr = descr.replaceAll("[\\r\\n]+", " "); // ensure no line breaks in the description cause problems
        		pw.print(">"+id);
        		pw.print(" ");
        		pw.println(descr);
        	} else {
        		pw.println(">"+id);
        	}
        	pw.println(sv.getStringValue());
        }
        
        // close all files from the file map 
        logger.info("Saved sequences to "+file_map.size()+" files.");
        for (String s : file_map.keySet()) {
        	PrintWriter pw = file_map.get(s);
        	pw.close();
        }
        
        BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], outputTable, exec);
        // once we are done, we close the container and return its table
        return new BufferedDataTable[] { out };
    }

    /**
     * HACK TODO: code copied from SpectraWriterNodeModel... need a manager class for this!
     * 
     * Create a print writer which is ready for use based on node configuration and the current row (containing the peak list to be saved).
     * The map is updated to include the PrintWriter for further use if needed
     * 
     * @param use_suffix
     * @param suffix_idx
     * @param r
     * @param file_map
     * @return
     * @throws IOException
     * @throws InvalidSettingsException if an invalid suffix is encountered during execute() or if a file already exists and the node configuration does not permit overwrite
     */
    private PrintWriter get_pw(boolean use_suffix, int suffix_idx, DataRow r,
			HashMap<String, PrintWriter> file_map, File basename) throws InvalidSettingsException, IOException {
		if (!use_suffix) {
			return file_map.get(basename.getName());
		} else {
			String bn = basename.getName();
			Pattern p = Pattern.compile("^(.*)(\\.[a-zA-Z0-9]+)$");
			Matcher m = p.matcher(bn);
			DataCell suffix_cell =  r.getCell(suffix_idx);
			if (suffix_cell == null || suffix_cell.isMissing())
				throw new InvalidSettingsException("Suffix cannot be missing!");
			String suffix_to_use = suffix_cell.toString();
			if (suffix_to_use.indexOf(File.separatorChar) >= 0)
				throw new InvalidSettingsException("Suffix cannot contain: "+File.separatorChar);
			
			File f = null;
			if (m.matches()) {
				String sans_ext = m.group(1);
				f = new File(basename.getParent(), sans_ext+suffix_cell.toString()+m.group(2));
			} else {
				f = new File(basename.getParent(), bn+suffix_cell.toString());
			}
			
			if (file_map.containsKey(f.getName())) {
				return file_map.get(f.getName());
			} else {
				if (f.exists() && !m_overwrite.getBooleanValue())
					throw new InvalidSettingsException("Will not overwrite existing: "+f.getAbsolutePath());
				
				logger.info("Saving sequences to "+f.getAbsolutePath());
				PrintWriter pw = new PrintWriter(new FileWriter(f));
				file_map.put(f.getName(), pw);
				return pw;
			}
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
   
        return new DataTableSpec[]{inSpecs[0]};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         m_seq_col.saveSettingsTo(settings);
         m_filename.saveSettingsTo(settings); 
         m_overwrite.saveSettingsTo(settings);
         m_suffix.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_seq_col.loadSettingsFrom(settings);
        m_filename.loadSettingsFrom(settings); 
        m_overwrite.loadSettingsFrom(settings);
        // backward compatibility with existing workflows
        if (settings.containsKey(CFGKEY_FILENAME_SUFFIX)) {
        	m_suffix.loadSettingsFrom(settings);
        } else {
        	m_suffix.setStringValue("");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
         m_seq_col.validateSettings(settings);
         m_filename.validateSettings(settings);
         m_overwrite.validateSettings(settings);
         // backward compatibility with existing workflows
         if (settings.containsKey(CFGKEY_FILENAME_SUFFIX)) {
         	m_suffix.validateSettings(settings);
         } 
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

