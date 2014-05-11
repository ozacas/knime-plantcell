package au.edu.unimelb.plantcell.io.write.gff;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

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
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.regions.RegionInterface;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;


/**
 * This is the model implementation of FastaWriter.
 * Creates a .fasta file with the specified accession, description, and sequence columns
 *
 * @author Andrew Cassin
 */
public class GFF2WriterNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("GFFv2 Writer");
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_SEQ   = "Sequence";
	static final String CFGKEY_FILE  = "Output Filename";
    static final String CFGKEY_OVERWRITE = "overwrite";
    static final String CFGKEY_TRACK = "track-to-save";

    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels".
   
    private final SettingsModelString       m_seq_col = (SettingsModelString) make(CFGKEY_SEQ);
    private final SettingsModelString      m_filename = (SettingsModelString) make(CFGKEY_FILE);
    private final SettingsModelBoolean    m_overwrite = (SettingsModelBoolean) make(CFGKEY_OVERWRITE);
    private final SettingsModelString     m_track     = (SettingsModelString) make(CFGKEY_TRACK);
    
    /**
     * Constructor for the node model.
     */
    protected GFF2WriterNodeModel() {
        super(1, 0);
    }

    public static SettingsModel make(String field_name) {
    	if (field_name.equals(CFGKEY_SEQ))
    		return new SettingsModelString(CFGKEY_SEQ, CFGKEY_SEQ);
    	else if (field_name.equals(CFGKEY_FILE))
    		return new SettingsModelString(CFGKEY_FILE, ""); 
    	else if (field_name.equals(CFGKEY_OVERWRITE)) 
    		return new SettingsModelBoolean(CFGKEY_OVERWRITE, false);
    	else if (field_name.equals(CFGKEY_TRACK)) {
    		return new SettingsModelString(CFGKEY_TRACK, "");
    	}
    	else
    		return null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        logger.info("Writing GFF (v2) file... "+m_filename.getStringValue());
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
        PrintWriter out_stream = new PrintWriter(new BufferedWriter(new FileWriter(fname)));
        BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(inData[0].iterator(), seq_idx, 100, 1024 * 1024, null);
        int done = 0;
        int n_rows = inData[0].getRowCount();
        out_stream.println("##gff-version 2");
        out_stream.println("##source-version knime_plantcell 1.2");
        out_stream.println("##date "+new java.util.Date().toString());

        while (bsi.hasNext()) {
        	List<SequenceValue> batch = bsi.next();
        	if (batch == null)
        		break;
        	for (SequenceValue sv : batch) {
        		Track t = sv.getTrackByName(m_track.getStringValue(), null);
        		if (t == null || t.countAnnotations() < 1)
					continue;
        		save_annotations(sv, t, out_stream);
        	}
        	exec.checkCanceled();
        	exec.setProgress(((double)done) / n_rows);
        }
        out_stream.close();
        logger.info("Processed "+done+" sequences.");
        return new BufferedDataTable[] { };
    }

    private void save_annotations(SequenceValue sv, Track t,
			PrintWriter out) {
    	
		for (RegionInterface ri : t.getAnnotation().getRegions()) {
			String line = ri.asGFF(sv, t);
			if (line != null)
				out.println(line);
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
   
        return new DataTableSpec[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         m_seq_col.saveSettingsTo(settings);
         m_filename.saveSettingsTo(settings); 
         m_overwrite.saveSettingsTo(settings);
         m_track.saveSettingsTo(settings);
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
        m_track.loadSettingsFrom(settings);
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
         m_track.validateSettings(settings);
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

