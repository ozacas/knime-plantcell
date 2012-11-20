package au.edu.unimelb.plantcell.io.write.fasta;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

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

    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels".
   
    private final SettingsModelString       m_seq_col = (SettingsModelString) make(CFGKEY_SEQ);
    private final SettingsModelString      m_filename = (SettingsModelString) make(CFGKEY_FILE);
    private final SettingsModelBoolean    m_overwrite = (SettingsModelBoolean) make(CFGKEY_OVERWRITE);
   
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
    	else
    		return null;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        logger.info("Writing fasta file... "+m_filename.getStringValue());
        String fname = m_filename.getStringValue();
        if (fname == null || fname.length() < 1) {
        	throw new Exception("No filename specified... nothing to save!");
        }
        
        int seq_idx = inData[0].getSpec().findColumnIndex(m_seq_col.getStringValue());
        if (seq_idx  < 0) 
        	throw new InvalidSettingsException("");
        
        File f = new File(fname);
        if (!m_overwrite.getBooleanValue() && f.exists()) {
        	throw new Exception("Will not overwrite existing: "+fname+" - configure the node to override if this is what you want.");
        }
        // replicate input data on output port
        DataTableSpec inSpec  = inData[0].getDataTableSpec();
       
        PrintWriter out_stream = new PrintWriter(new BufferedWriter(new FileWriter(fname)));
      
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
        	
        	if (sv.hasDescription()) {
        		out_stream.print(">"+sv.getID());
        		out_stream.print(" ");
        		out_stream.println(sv.getDescription());
        	} else {
        		out_stream.println(">"+sv.getID());
        	}
        	out_stream.println(sv.getStringValue());
        }
        out_stream.close();
        BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], outputTable, exec);
        // once we are done, we close the container and return its table
        return new BufferedDataTable[] { out };
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

