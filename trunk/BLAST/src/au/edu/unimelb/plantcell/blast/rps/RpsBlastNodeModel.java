package au.edu.unimelb.plantcell.blast.rps;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.blast.BLASTPlusNodeModel;
import au.edu.unimelb.plantcell.blast.BlastResultsWalker;
import au.edu.unimelb.plantcell.blast.TabbedCSVLogger;
import au.edu.unimelb.plantcell.core.ErrorLogger;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcell.io.write.fasta.FastaWriter;
import au.edu.unimelb.plantcore.core.regions.BlastHitRegion;


/**
 * This is the model implementation of BLASTPlus.
 * Supports local execution of NCBI BLAST+ executables (which must be  installed separately)
 *
 * @author http://www.plantcell.unimelb.edu.au/
 */
public class RpsBlastNodeModel extends BLASTPlusNodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("NCBI RPSBLAST");

	public static final String CFGKEY_RPS_VARIANT    = "rps-variant-program"; // rpsblast or rpstblastn
	public static final String CFGKEY_TBLASTN_STRAND = "tblastn-strand";
	
        
    private SettingsModelString m_column        = new SettingsModelString(CFGKEY_QUERY_DATABASE, "");
    private SettingsModelString m_rps_variant   = new SettingsModelString(CFGKEY_RPS_VARIANT, "rpsblast");
    private SettingsModelString m_strand        = new SettingsModelString(CFGKEY_TBLASTN_STRAND, "both");
    
    // not persisted
    private File m_tmp_fasta;
    
    /**
     * Constructor for the node model.
     */
    protected RpsBlastNodeModel() {
            super(1, 2);
            m_strand.setEnabled(false);		// to ensure consistency of model settings
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean valid_sequence_type(SequenceValue sv) {
    	assert(sv != null);
    	if (m_rps_variant.getStringValue().toLowerCase().equals("rpsblast")) {
    		return (sv.getSequenceType().equals(SequenceType.AA));
    	} else {
    		return (!sv.getSequenceType().equals(SequenceType.AA));
    	}
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	logger.info("Locating blast program...");
    	File blast = find_blast_program(m_rps_variant.getStringValue());
    	if (blast == null) {
    		throw new InvalidSettingsException("Unable to locate "+m_rps_variant.getStringValue()+" - re-configure?");
    	}
    	
    	/**
    	 *  this node is tricky: it doesnt specify an output table spec until the fields are 
    	 *  known from the stream or, if an error occurs, a silly output table is created to keep KNIME happy
    	 */
    	
    	logger.info("Running "+blast.getAbsolutePath());
    	CommandLine cmdLine = new CommandLine(blast);
    	
    	// this temp file must be setup
    	m_tmp_fasta = File.createTempFile("ncbi_blast_tmp", ".fasta");	// file does not exist yet but will be populated per blast batch...
    
    	// ensure blast computes the requested columns
    	addArguments(cmdLine);
    	
    	RowIterator it = inData[0].iterator();
    	int seq_idx = inData[0].getSpec().findColumnIndex(m_column.getStringValue());
    	if (seq_idx < 0) {
    		throw new InvalidSettingsException("Unable to locate column: "+m_column.getStringValue()+" - reconfigure?");
    	}
    	
    	// for the BLAST from fasta file node, we know that there is no tracks to begin
    	// with (cant be: no input ports to the node) so we dont have to add to the properties
    	DataColumnSpec[] col = new DataColumnSpec[] { mergeColumnProperties(inData[0].getSpec().getColumnSpec(seq_idx).getProperties(), 
    			new Track[] {new Track(Track.RPSBLAST_TRACK,     getTrackCreator()) }
    	) };
		final MyDataContainer c_seq = new MyDataContainer(exec.createDataContainer(new DataTableSpec(col)), "Query");
    	// the tsv takes care of the first output port (we dont know the columns until data comes back from BLAST)
		TabbedCSVLogger tsv = new TabbedCSVLogger(logger, exec);

    	try {
    		BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(it,
        			seq_idx, 1000, 1000 * 1000, getSequenceProcessor(exec));
        			
	    	while (bsi.hasNext()) {
	    		Map<UniqueID,SequenceValue> batch_map = bsi.nextAsMap();
	    		if (batch_map == null)
	    			break;
	    		
	    		new FastaWriter(m_tmp_fasta, batch_map).write();
	    		
				logger.info("Running batch of "+batch_map.size()+" sequences: "+cmdLine.toString());
				tsv.setBatch(batch_map);
				DefaultExecutor exe = new DefaultExecutor();
		    	exe.setExitValues(new int[] {0});
		    	exe.setStreamHandler(new PumpStreamHandler(tsv, new ErrorLogger(logger)));
		    	exe.setWorkingDirectory(getQueryDatabaseFile().getParentFile());
		    	exe.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
		    	
	        	int exitCode = exe.execute(cmdLine);
	        	logger.info("got exit code: "+exitCode+" from BLAST");
	        	
	        	if (exe.isFailure(exitCode)) {
	        		if (exe.getWatchdog().killedProcess())
	        			throw new Exception("BLAST failed - watchdog says no...");
	        		else
	        			throw new Exception("BLAST failed - check console messages and input data");
	        	}
	        	
	        	// user want annotated sequences?
		    	tsv.walkResults(new BlastResultsWalker() {
	
		    		@Override
					public void hit(SequenceValue hit, List<BlastHitRegion> hits) {
		    			if (!reportHits())
							return;
			    		
			    		// add non-null result tracks to sc
			    		SequenceCell sc;
						try {
							sc = new SequenceCell(hit);
				    		addTrack(sc, Track.RPSBLAST_TRACK,     hits);
				    		
				    		c_seq.addRow(new DataCell[] { sc });
						} catch (InvalidSettingsException e) {
							e.printStackTrace();
						}
					}
	
					@Override
					public void nohit(SequenceValue sv) {
						if (!reportNoHits())
							return;
						try {
							c_seq.addRow(new DataCell[] { new SequenceCell(sv) });
						} catch (InvalidSettingsException e) {
							e.printStackTrace();
						}
					}
		    		
		    	});
	    		}
	    	
	    } finally {
	    		m_tmp_fasta.delete();
	    }
    	
        return new BufferedDataTable[]{tsv.getTSVTable(), c_seq.close() };
    }

    @Override
    protected void addArguments(CommandLine cmdLine) {
    	cmdLine.addArgument("-db");
    	// query database must be adjusted for RPSBLAST argument semantics (trim .loo extension)
    	String db = getDB();
    	if (db.toLowerCase().endsWith(".loo")) {
    		db = db.substring(0, db.length()-4);
    	}
    	cmdLine.addArgument(db);
    	cmdLine.addArgument("-outfmt");
    	// for the -outfmt argument we turn automatic quoting off as it impacts mac-compatibility
    	cmdLine.addArgument(getOutFmtString(), false);
    	cmdLine.addArgument("-query");
    	cmdLine.addArgument(getQueryDatabase());
    	cmdLine.addArgument("-evalue");
    	cmdLine.addArgument(new Double(getEvalue()).toString());
    	if (useLowComplexityFilter()) {
    		cmdLine.addArgument("-seg");
    		cmdLine.addArgument("yes");
    	}
    	
    	if (m_rps_variant.getStringValue().equals("rpstblastn")) {
    		cmdLine.addArgument("-strand");
    		cmdLine.addArgument(m_strand.getStringValue());
    	} else {
    		// rpstblastn does not appear to support multi-threading as of v2.2.26
    		cmdLine.addArgument("-num_threads");
        	cmdLine.addArgument(String.valueOf(getNumThreads()));
    	}
    	
    	String other_args = getOtherArguments();
    	if (other_args.length() > 0) {
    		String[] additional_args = other_args.split("\\s+");
    		for (String s : additional_args) {
    			cmdLine.addArgument(s);
    		}
    	}
    }

	/**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
    	super.saveSettingsTo(settings);
    	m_column.saveSettingsTo(settings);
    	m_rps_variant.saveSettingsTo(settings);
    	m_strand.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
    	super.loadValidatedSettingsFrom(settings);
    	m_column.loadSettingsFrom(settings);
    	if (settings.containsKey(CFGKEY_RPS_VARIANT)) {			// backward compatibility: if not found assume rpsblast
    		m_rps_variant.loadSettingsFrom(settings);
    		m_strand.loadSettingsFrom(settings);
    	} else {
    		m_rps_variant.setStringValue("rpsblast");
    		m_strand.setStringValue("both");
    	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	super.validateSettings(settings);
    	m_column.validateSettings(settings);
    	if (settings.containsKey(CFGKEY_RPS_VARIANT)) {
    		m_rps_variant.validateSettings(settings);
    		m_strand.validateSettings(settings);
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

    @Override
    public String getQueryDatabase() {
    	return m_tmp_fasta.getAbsolutePath();
    }
}

